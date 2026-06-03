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
          ${o.status === 'delivered' ? `<button class="btn btn-sm" style="background:#fff3e0;color:#e65100;border:1px solid #ffb74d" onclick="openReturnModal(${o.id})">↩ Вернуть товар</button>` : ''}
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

// ── Return Form ────────────────────────────────────────────────

let returnPhotos = []; // Array of base64 strings

function openReturnModal(orderId) {
  returnPhotos = [];
  document.getElementById('return-order-id').value = orderId;
  document.getElementById('return-reason').value = '';
  document.getElementById('return-photos-preview').innerHTML = '';
  document.getElementById('return-photo-count').textContent = '(0 выбрано, мин. 2, макс. 10)';
  document.getElementById('return-error').style.display = 'none';
  document.getElementById('return-modal').style.display = 'flex';
}

function closeReturnModal() {
  document.getElementById('return-modal').style.display = 'none';
  returnPhotos = [];
}

function handleReturnPhotos(input) {
  const files = Array.from(input.files);
  const remaining = 10 - returnPhotos.length;
  const toAdd = files.slice(0, remaining);

  toAdd.forEach(file => {
    const reader = new FileReader();
    reader.onload = e => {
      // Compress via canvas
      const img = new Image();
      img.onload = () => {
        const MAX = 800;
        let w = img.width, h = img.height;
        if (w > MAX || h > MAX) {
          const scale = Math.min(MAX / w, MAX / h);
          w = Math.round(w * scale);
          h = Math.round(h * scale);
        }
        const canvas = document.createElement('canvas');
        canvas.width = w; canvas.height = h;
        canvas.getContext('2d').drawImage(img, 0, 0, w, h);
        const b64 = canvas.toDataURL('image/jpeg', 0.75).split(',')[1];
        returnPhotos.push(b64);
        updatePhotoPreviews();
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  });

  input.value = '';
}

function updatePhotoPreviews() {
  const preview = document.getElementById('return-photos-preview');
  preview.innerHTML = returnPhotos.map((b64, i) => `
    <div style="position:relative">
      <img src="data:image/jpeg;base64,${b64}"
           style="width:72px;height:72px;object-fit:cover;border-radius:8px;border:1px solid var(--gray-200)">
      <button onclick="removeReturnPhoto(${i})"
              style="position:absolute;top:-6px;right:-6px;width:20px;height:20px;border-radius:50%;background:var(--red);color:#fff;border:none;cursor:pointer;font-size:11px;display:flex;align-items:center;justify-content:center">✕</button>
    </div>
  `).join('');
  document.getElementById('return-photo-count').textContent =
    `(${returnPhotos.length} выбрано, мин. 2, макс. 10)`;
  document.getElementById('return-photo-btn').style.display =
    returnPhotos.length >= 10 ? 'none' : 'inline-flex';
}

function removeReturnPhoto(index) {
  returnPhotos.splice(index, 1);
  updatePhotoPreviews();
}

async function submitReturn() {
  const orderId = parseInt(document.getElementById('return-order-id').value);
  const reason  = document.getElementById('return-reason').value.trim();
  const errEl   = document.getElementById('return-error');

  errEl.style.display = 'none';
  if (!reason) { errEl.textContent = 'Укажите причину возврата'; errEl.style.display = 'block'; return; }
  if (returnPhotos.length < 2) { errEl.textContent = 'Добавьте минимум 2 фотографии'; errEl.style.display = 'block'; return; }

  const btn = document.getElementById('btn-submit-return');
  btn.disabled = true; btn.textContent = 'Отправка...';

  const res = await apiFetch('/returns', {
    method: 'POST',
    body: JSON.stringify({ order_id: orderId, reason, photos: returnPhotos })
  });

  btn.disabled = false; btn.textContent = 'Отправить заявку';

  if (res?.ok) {
    closeReturnModal();
    showToast('Заявка на возврат создана', 'success');
  } else {
    errEl.textContent = res?.data?.error || 'Ошибка отправки заявки';
    errEl.style.display = 'block';
  }
}

// ── My Returns ────────────────────────────────────────────────

const returnStatusLabels = {
  CREATED:          { text: 'Создана заявка на возврат',    color: '#1976d2' },
  COURIER_PENDING:  { text: 'Передать возврат курьеру',     color: '#e65100' },
  IN_TRANSIT:       { text: 'Возврат в пути к продавцу',    color: '#7b1fa2' },
  PENDING_DECISION: { text: 'Ожидаем решения продавца',     color: '#f57c00' },
  REFUNDED:         { text: 'Возврат успешно совершён',      color: '#2e7d32' },
};

async function openMyReturns() {
  document.getElementById('my-returns-modal').style.display = 'flex';
  const body = document.getElementById('my-returns-body');
  body.innerHTML = '<div class="loading-wrapper"><div class="spinner"></div></div>';

  const res = await apiFetch('/returns');
  if (!res?.ok) { body.innerHTML = '<p style="color:var(--red)">Ошибка загрузки</p>'; return; }

  const list = res.data.returns;
  if (!list.length) {
    body.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><p>У вас нет заявок на возврат</p></div>';
    return;
  }

  body.innerHTML = list.map(r => {
    const st = returnStatusLabels[r.status] || { text: r.status, color: '#666' };
    const courierBtn = r.status === 'COURIER_PENDING'
      ? `<button class="btn btn-sm" style="margin-top:10px;background:#fff3e0;color:#e65100;border:1px solid #ffb74d"
             onclick="markCourierSent(${r.id})">📦 Передал курьеру</button>` : '';
    return `
      <div style="border:1px solid var(--gray-200);border-radius:12px;padding:14px;margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:700">Заказ #${r.order_id}</span>
          <span style="font-size:12px;color:var(--gray-400)">${formatDate(r.created_at)}</span>
        </div>
        <div style="display:inline-block;margin:8px 0;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:600;color:#fff;background:${st.color}">${st.text}</div>
        <div style="font-size:13px;color:var(--gray-600)">${r.reason}</div>
        ${courierBtn}
      </div>`;
  }).join('');
}

async function markCourierSent(returnId) {
  const res = await apiFetch(`/returns/${returnId}/courier-sent`, { method: 'PUT' });
  if (res?.ok) {
    showToast('Статус обновлён: возврат в пути к продавцу', 'success');
    openMyReturns();
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadOrders();
  document.getElementById('order-modal')?.addEventListener('click', e => {
    if (e.target === document.getElementById('order-modal')) document.getElementById('order-modal').style.display = 'none';
  });
  document.getElementById('return-modal')?.addEventListener('click', e => {
    if (e.target === document.getElementById('return-modal')) closeReturnModal();
  });
  document.getElementById('my-returns-modal')?.addEventListener('click', e => {
    if (e.target === document.getElementById('my-returns-modal')) document.getElementById('my-returns-modal').style.display = 'none';
  });
});
