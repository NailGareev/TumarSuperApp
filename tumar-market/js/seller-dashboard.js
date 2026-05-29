/* ===== Seller Dashboard JS ===== */

let sellerOrders = [];
let sellerOrdersFilter = 'active';

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
  if (!res?.ok) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">🛍</div><h3>Нет заказов</h3></div>';
    return;
  }

  sellerOrders = res.data || [];
  renderSellerOrders();
}

function filterSellerOrders(filter, evt) {
  sellerOrdersFilter = filter;
  document.querySelectorAll('.seller-orders-tabs .tab').forEach(t => t.classList.remove('active'));
  if (evt?.target) {
    evt.target.classList.add('active');
  }
  renderSellerOrders();
}

function renderSellerOrders() {
  const el = document.getElementById('seller-orders-list');
  const statusLabels = {
    pending: '⏳ Ожидает', confirmed: '✅ Подтверждён', processing: '🔧 В работе',
    shipped: '🚚 Отправлен', delivered: '✓ Доставлен', cancelled: '✕ Отменён',
  };
  const filtered = sellerOrders.filter(o => {
    const archived = isArchivedOrder(o);
    return sellerOrdersFilter === 'archive' ? archived : !archived;
  });
  if (!filtered.length) {
    const title = sellerOrdersFilter === 'archive' ? 'Архив пуст' : 'Нет заказов';
    el.innerHTML = `<div class="empty-state"><div class="empty-icon">🛍</div><h3>${title}</h3></div>`;
    return;
  }

  el.innerHTML = filtered.map(o => {
    const isArchived = isArchivedOrder(o);
    const canIssue = !isArchived && o.status !== 'delivered';
    return `
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
        <div class="order-actions">
          ${canIssue ? `<button class="btn btn-primary btn-sm" onclick="issueOrderCode(${o.id})" aria-label="Выдать код для заказа #${o.id}">Выдать заказ</button>` : ''}
          ${!isArchived ? `
            <select class="form-control order-status-select" onchange="updateOrderStatus(${o.id}, this.value)">
              <option value="">Изменить статус</option>
              <option value="confirmed">Подтвердить</option>
              <option value="processing">В обработке</option>
              <option value="shipped">Отправить</option>
              <option value="delivered">Доставлен</option>
            </select>
          ` : `<span class="order-archive-label">В архиве</span>`}
        </div>
      </div>
    `;
  }).join('');
}

function isArchivedOrder(order) {
  return order.status === 'cancelled';
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

async function issueOrderCode(orderId) {
  if (!confirm('Отправить код выдачи клиенту?')) return;

  const sendRes = await apiFetch(`/seller/orders/${orderId}/issue-code`, { method: 'POST' });
  if (!sendRes?.ok) {
    showToast(sendRes?.data?.error || 'Ошибка отправки кода', 'error');
    return;
  }
  showToast('Код отправлен клиенту');

  showIssueCodeModal(orderId);
}

function showIssueCodeModal(orderId) {
  let modal = document.getElementById('issue-code-modal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'issue-code-modal';
    modal.className = 'modal-overlay';
    modal.innerHTML = `
      <div class="modal-box" style="max-width:360px">
        <h3 style="font-size:17px;font-weight:700;margin-bottom:16px">Подтверждение выдачи</h3>
        <p style="font-size:14px;color:var(--gray-600);margin-bottom:16px">Попросите клиента назвать код из уведомления и введите его ниже.</p>
        <input id="issue-code-input" type="text" class="form-control" placeholder="Код (4 цифры)" maxlength="4"
               style="font-size:22px;text-align:center;letter-spacing:8px;font-weight:700;margin-bottom:16px">
        <div style="display:flex;gap:10px">
          <button class="btn btn-secondary" style="flex:1" onclick="closeModal('issue-code-modal')">Отмена</button>
          <button id="issue-code-confirm-btn" class="btn btn-primary" style="flex:1">Подтвердить</button>
        </div>
      </div>`;
    document.body.appendChild(modal);
    modal.addEventListener('click', e => { if (e.target === modal) closeModal('issue-code-modal'); });
  }

  document.getElementById('issue-code-input').value = '';
  modal.style.display = 'flex';
  document.getElementById('issue-code-input').focus();

  const btn = document.getElementById('issue-code-confirm-btn');
  const newBtn = btn.cloneNode(true);
  btn.parentNode.replaceChild(newBtn, btn);
  newBtn.addEventListener('click', () => confirmIssueCode(orderId));

  document.getElementById('issue-code-input').onkeydown = e => {
    if (e.key === 'Enter') confirmIssueCode(orderId);
  };
}

async function confirmIssueCode(orderId) {
  const code = document.getElementById('issue-code-input').value.trim();
  if (!code) { showToast('Введите код', 'error'); return; }

  const btn = document.getElementById('issue-code-confirm-btn');
  btn.disabled = true;
  btn.textContent = '...';

  const res = await apiFetch(`/seller/orders/${orderId}/confirm-issue`, {
    method: 'POST',
    body: JSON.stringify({ code }),
  });

  btn.disabled = false;
  btn.textContent = 'Подтвердить';

  if (res?.ok) {
    closeModal('issue-code-modal');
    showToast('Заказ выдан клиенту ✓');
    loadSellerOrders();
  } else {
    showToast(res?.data?.error || 'Неверный код', 'error');
    document.getElementById('issue-code-input').select();
  }
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
