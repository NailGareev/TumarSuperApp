/* ===== Orders Page JS ===== */

let allOrders = [];
let currentFilter = '';

const statusLabels = {
  pending: { label: 'Ожидает', cls: 'badge-pending' },
  confirmed: { label: 'Подтверждён', cls: 'badge-active' },
  processing: { label: 'В обработке', cls: 'badge-processing' },
  shipped: { label: 'В пути', cls: 'badge-shipped' },
  delivered: { label: 'Доставлен', cls: 'badge-delivered' },
  cancelled: { label: 'Отменён', cls: 'badge-cancelled' },
};

async function loadOrders() {
  if (!isLoggedIn()) { window.location.href = '/login?redirect=/orders'; return; }

  const res = await apiFetch('/orders');
  document.getElementById('orders-loading').style.display = 'none';

  if (!res?.ok) return;
  allOrders = res.data;
  renderOrders(allOrders);
}

function filterOrders(status) {
  currentFilter = status;
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  event.target.classList.add('active');
  const filtered = status ? allOrders.filter(o => o.status === status) : allOrders;
  renderOrders(filtered);
}

function renderOrders(orders) {
  const el = document.getElementById('orders-list');
  if (!orders.length) {
    el.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>Нет заказов</h3>
        <p>Вы ещё не делали покупок</p>
        <a href="/catalog" class="btn btn-primary btn-lg" style="margin-top:16px">Перейти в каталог</a>
      </div>`;
    return;
  }

  el.innerHTML = orders.map(o => {
    const st = statusLabels[o.status] || { label: o.status, cls: '' };
    return `
      <div class="order-card">
        <div class="order-header" onclick="openOrderDetail(${o.id})">
          <div class="order-header-left">
            <span class="order-num">#${o.id}</span>
            <span class="order-date">${formatDate(o.created_at)}</span>
            <span class="badge-status ${st.cls}">${st.label}</span>
          </div>
          <span class="order-total-val">${formatPrice(o.total)}</span>
        </div>
        <div class="order-actions">
          <button class="btn btn-secondary btn-sm" onclick="openOrderDetail(${o.id})">Подробнее</button>
          ${o.status === 'pending' ? `<button class="btn btn-sm" style="background:#ffebee;color:var(--red)" onclick="cancelOrder(${o.id})">Отменить</button>` : ''}
          ${o.status === 'delivered' ? `<a href="/catalog" class="btn btn-outline btn-sm">Купить снова</a>` : ''}
        </div>
      </div>
    `;
  }).join('');
}

async function openOrderDetail(orderId) {
  const res = await apiFetch(`/orders/${orderId}`);
  if (!res?.ok) return;

  const o = res.data;
  const st = statusLabels[o.status] || { label: o.status, cls: '' };

  document.getElementById('modal-order-title').textContent = `Заказ #${o.id}`;
  document.getElementById('modal-order-body').innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <span class="badge-status ${st.cls}">${st.label}</span>
      <span style="font-size:13px;color:var(--gray-400)">${formatDate(o.created_at)}</span>
    </div>
    <div style="background:var(--gray-50);border-radius:8px;padding:12px;margin-bottom:16px;font-size:13px">
      <div><strong>📍 Адрес:</strong> ${o.delivery_address}</div>
      <div><strong>💳 Оплата:</strong> ${o.payment_method}</div>
    </div>
    ${(o.items || []).map(item => `
      <div class="modal-order-item">
        <img class="modal-order-img"
             src="${item.product_image || 'https://via.placeholder.com/56'}"
             onerror="this.src='https://via.placeholder.com/56'">
        <div class="modal-order-info">
          <div class="modal-order-name">${item.product_name}</div>
          <div class="modal-order-store">🏪 ${item.store_name} • x${item.quantity}</div>
        </div>
        <div class="modal-order-price">${formatPrice(item.price * item.quantity)}</div>
      </div>
    `).join('')}
    <div class="order-detail-total">
      <span>Итого</span>
      <span>${formatPrice(o.total)}</span>
    </div>
  `;

  document.getElementById('order-modal').style.display = 'flex';
}

async function cancelOrder(orderId) {
  if (!confirm('Отменить заказ?')) return;
  const res = await apiFetch(`/orders/${orderId}/cancel`, { method: 'PUT' });
  if (res?.ok) {
    showToast('Заказ отменён', 'info');
    loadOrders();
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadOrders();
  document.getElementById('order-modal')?.addEventListener('click', e => {
    if (e.target === document.getElementById('order-modal')) document.getElementById('order-modal').style.display = 'none';
  });
});
