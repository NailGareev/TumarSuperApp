/* ===== Seller Dashboard JS ===== */

// ── Tab Switching ─────────────────────────────────────────────
function switchTab(name) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.seller-nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`tab-${name}`)?.classList.add('active');
  document.querySelector(`[data-tab="${name}"]`)?.classList.add('active');

  if (name === 'products') loadSellerProducts();
  if (name === 'orders') loadSellerOrders();
  if (name === 'store') loadStoreSettings();
}

document.querySelectorAll('.seller-nav-item[data-tab]').forEach(item => {
  item.addEventListener('click', e => {
    e.preventDefault();
    switchTab(item.dataset.tab);
  });
});

// ── Dashboard ─────────────────────────────────────────────────
async function loadDashboard() {
  if (!isLoggedIn()) { window.location.href = '/login'; return; }

  const res = await apiFetch('/seller/dashboard');
  if (!res?.ok) {
    if (res?.status === 404) {
      document.getElementById('no-store-banner').style.display = 'block';
    }
    return;
  }

  const { stats } = res.data;
  const grid = document.getElementById('dashboard-stats');
  grid.innerHTML = `
    <div class="stat-card">
      <div class="stat-label">Товары в каталоге</div>
      <div class="stat-value">${stats.total_products}<span class="stat-icon">📦</span></div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Всего заказов</div>
      <div class="stat-value">${stats.total_orders}<span class="stat-icon">🛍</span></div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Выручка</div>
      <div class="stat-value">${formatPrice(stats.total_revenue)}<span class="stat-icon">💰</span></div>
    </div>
    <div class="stat-card">
      <div class="stat-label">Ожидают обработки</div>
      <div class="stat-value">${stats.pending_orders}<span class="stat-icon">⏳</span></div>
    </div>
  `;
}

// ── Seller Products ───────────────────────────────────────────
async function loadSellerProducts() {
  const el = document.getElementById('seller-products-list');
  el.innerHTML = '<div class="loading-wrapper"><div class="spinner"></div></div>';

  const res = await apiFetch('/seller/products');
  if (!res?.ok) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><h3>Нет товаров</h3><p><a href="/seller/add-product">Добавить первый товар</a></p></div>';
    return;
  }

  if (!res.data.length) {
    el.innerHTML = `
      <div class="empty-state" style="padding:60px">
        <div class="empty-icon">📦</div>
        <h3>Нет товаров</h3>
        <p style="margin-bottom:16px">Добавьте первый товар в ваш магазин</p>
        <a href="/seller/add-product" class="btn btn-primary">+ Добавить товар</a>
      </div>`;
    return;
  }

  el.innerHTML = `
    <table class="products-table">
      <thead>
        <tr>
          <th>Товар</th>
          <th>Цена</th>
          <th>Остаток</th>
          <th>Рейтинг</th>
          <th>Статус</th>
          <th>Действия</th>
        </tr>
      </thead>
      <tbody>
        ${res.data.map(p => `
          <tr>
            <td>
              <div class="product-img-cell">
                <img class="product-img-sm" src="${p.main_image || 'https://via.placeholder.com/48'}"
                     onerror="this.src='https://via.placeholder.com/48'" alt="">
                <span class="product-name-cell">${p.product_name}</span>
              </div>
            </td>
            <td><strong>${formatPrice(p.price)}</strong>
              ${p.original_price && p.original_price > p.price
                ? `<br><small style="color:var(--gray-400);text-decoration:line-through">${formatPrice(p.original_price)}</small>`
                : ''}</td>
            <td>${p.stock} шт.</td>
            <td>${renderStars(p.rating)} (${p.review_count})</td>
            <td><span class="badge-status ${p.is_active ? 'badge-active' : 'badge-suspended'}">${p.is_active ? 'Активен' : 'Скрыт'}</span></td>
            <td>
              <button class="btn btn-secondary btn-sm" onclick="openEditProduct(${p.product_seller_id}, ${p.price}, ${p.original_price}, ${p.stock}, ${p.delivery_days}, ${p.is_active})">✏️</button>
              <button class="btn btn-sm" style="background:#ffebee;color:var(--red)" onclick="toggleProduct(${p.product_seller_id}, ${!p.is_active})">${p.is_active ? '🙈 Скрыть' : '👁 Показать'}</button>
              <a href="/product/${p.product_id}" class="btn btn-secondary btn-sm">👁</a>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

function openEditProduct(psId, price, origPrice, stock, delivery, isActive) {
  document.getElementById('edit-ps-id').value = psId;
  document.getElementById('edit-price').value = price;
  document.getElementById('edit-orig-price').value = origPrice || '';
  document.getElementById('edit-stock').value = stock;
  document.getElementById('edit-delivery').value = delivery;
  document.getElementById('edit-active').checked = isActive;
  document.getElementById('edit-product-modal').style.display = 'flex';
}

async function saveProductEdit() {
  const psId = document.getElementById('edit-ps-id').value;
  const body = {
    price: parseFloat(document.getElementById('edit-price').value),
    original_price: parseFloat(document.getElementById('edit-orig-price').value) || 0,
    stock: parseInt(document.getElementById('edit-stock').value),
    delivery_days: parseInt(document.getElementById('edit-delivery').value),
    is_active: document.getElementById('edit-active').checked,
  };
  const res = await apiFetch(`/seller/products/${psId}`, { method: 'PUT', body: JSON.stringify(body) });
  if (res?.ok) {
    closeModal('edit-product-modal');
    showToast('Товар обновлён');
    loadSellerProducts();
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
}

async function toggleProduct(psId, active) {
  const res = await apiFetch(`/seller/products/${psId}`, {
    method: 'PUT',
    body: JSON.stringify({ is_active: active, price: 0, stock: 0, delivery_days: 0 }),
  });
  if (res?.ok) loadSellerProducts();
}

// ── Seller Orders ─────────────────────────────────────────────
async function loadSellerOrders() {
  const el = document.getElementById('seller-orders-list');
  el.innerHTML = '<div class="loading-wrapper"><div class="spinner"></div></div>';

  const res = await apiFetch('/seller/orders');
  if (!res?.ok || !res.data.length) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">🛍</div><h3>Нет заказов</h3></div>';
    return;
  }

  const statusLabels = {
    pending: '⏳ Ожидает', confirmed: '✅ Подтверждён', processing: '🔧 В работе',
    shipped: '🚚 Отправлен', delivered: '✓ Доставлен', cancelled: '✕ Отменён',
  };

  el.innerHTML = res.data.map(o => `
    <div class="order-card">
      <div>
        <div class="order-id">#${o.id}</div>
        <div class="order-customer">👤 ${o.customer_name} ${o.customer_phone ? '| ' + o.customer_phone : ''}</div>
        <div class="order-date">${new Date(o.created_at).toLocaleDateString('ru-RU')}</div>
      </div>
      <div>
        <span class="badge-status badge-${o.status}">${statusLabels[o.status] || o.status}</span>
      </div>
      <div class="order-total">${formatPrice(o.total)}</div>
      <div>
        <select class="form-control" style="font-size:12px;padding:6px" onchange="updateOrderStatus(${o.id}, this.value)">
          <option value="">Изменить статус</option>
          <option value="confirmed">Подтвердить</option>
          <option value="processing">В обработке</option>
          <option value="shipped">Отправить</option>
          <option value="delivered">Доставлен</option>
        </select>
      </div>
    </div>
  `).join('');
}

async function updateOrderStatus(orderId, status) {
  if (!status) return;
  const res = await apiFetch(`/seller/orders/${orderId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
  if (res?.ok) { showToast('Статус обновлён'); loadSellerOrders(); }
  else showToast(res?.data?.error || 'Ошибка', 'error');
}

// ── Store Settings ────────────────────────────────────────────
async function loadStoreSettings() {
  const el = document.getElementById('store-form-wrap');
  const res = await apiFetch('/store/my');
  if (!res?.ok) {
    el.innerHTML = `<div class="empty-state"><h3>Магазин не найден</h3>
      <a href="/seller/store-register" class="btn btn-primary" style="margin-top:12px">Зарегистрировать магазин</a></div>`;
    return;
  }

  const s = res.data;
  el.innerHTML = `
    <h3 style="font-size:18px;font-weight:700;margin-bottom:20px">Настройки магазина</h3>
    <div class="store-status">
      Статус: <span class="badge-status badge-${s.status}">${s.status === 'active' ? 'Активен' : s.status}</span>
    </div>
    <form id="store-update-form" style="margin-top:20px">
      <div class="form-group">
        <label class="form-label">Название магазина</label>
        <input type="text" id="s-name" class="form-control" value="${s.name}">
      </div>
      <div class="form-group">
        <label class="form-label">Описание</label>
        <textarea id="s-desc" class="form-control" rows="3">${s.description || ''}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Адрес</label>
        <input type="text" id="s-address" class="form-control" value="${s.address || ''}">
      </div>
      <div class="form-group">
        <label class="form-label">Телефон</label>
        <input type="text" id="s-phone" class="form-control" value="${s.phone || ''}">
      </div>
      <button type="submit" class="btn btn-primary" style="width:100%">Сохранить изменения</button>
    </form>
  `;

  document.getElementById('store-update-form').addEventListener('submit', async e => {
    e.preventDefault();
    const res = await apiFetch('/store/my', {
      method: 'PUT',
      body: JSON.stringify({
        name: document.getElementById('s-name').value,
        description: document.getElementById('s-desc').value,
        address: document.getElementById('s-address').value,
        phone: document.getElementById('s-phone').value,
        logo: '',
      }),
    });
    if (res?.ok) showToast('Магазин обновлён');
    else showToast(res?.data?.error || 'Ошибка', 'error');
  });
}

function closeModal(id) { document.getElementById(id).style.display = 'none'; }

document.addEventListener('DOMContentLoaded', () => {
  loadDashboard();
  document.querySelectorAll('.modal-overlay').forEach(m => {
    m.addEventListener('click', e => { if (e.target === m) m.style.display = 'none'; });
  });
});
