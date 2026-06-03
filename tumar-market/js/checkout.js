/* ===== Checkout JS ===== */

let cartData = null;

async function loadCheckout() {
  if (!isLoggedIn()) { window.location.href = '/login?redirect=/checkout'; return; }

  const tumarPayOption = document.getElementById('tumar-pay-option');
  if (tumarPayOption) {
    if (isAppMode) {
      tumarPayOption.querySelector('input').checked = true;
    } else {
      tumarPayOption.style.display = 'none';
    }
  }

  const res = await apiFetch('/cart');
  document.getElementById('checkout-loading').style.display = 'none';

  if (!res?.ok || !res.data.items.length) {
    window.location.href = '/cart';
    return;
  }

  cartData = res.data;
  document.getElementById('checkout-layout').style.display = 'grid';

  // Items
  const itemsEl = document.getElementById('checkout-items');
  itemsEl.innerHTML = res.data.items.map(item => `
    <div class="checkout-item">
      <img class="checkout-item-img"
           src="${item.product_image || 'https://via.placeholder.com/48'}"
           onerror="this.src='https://via.placeholder.com/48'"
           alt="${item.product_name}">
      <div class="checkout-item-name">
        ${item.product_name}
        <div style="color:var(--gray-400);font-size:11px">x${item.quantity} • ${item.store_name}</div>
      </div>
      <div class="checkout-item-price">${formatPrice(item.price * item.quantity)}</div>
    </div>
  `).join('');

  document.getElementById('co-subtotal').textContent = formatPrice(res.data.total);
  document.getElementById('co-total').textContent = formatPrice(res.data.total);

  // Contact info
  const user = getUser();
  if (user) {
    document.getElementById('contact-info').innerHTML = `
      <div style="background:var(--gray-50);border-radius:8px;padding:14px">
        <div style="font-weight:700">${user.name}</div>
        <div style="font-size:13px;color:var(--gray-500)">${user.email}</div>
        ${user.phone ? `<div style="font-size:13px;color:var(--gray-500)">${user.phone}</div>` : ''}
      </div>
    `;
  }
}

async function placeOrder() {
  const city = document.getElementById('delivery-city').value;
  const street = document.getElementById('delivery-street').value.trim();
  const comment = document.getElementById('delivery-comment').value.trim();
  const payment = document.querySelector('input[name="payment"]:checked')?.value;
  const errEl = document.getElementById('checkout-error');

  errEl.style.display = 'none';

  if (!street) {
    errEl.textContent = 'Введите адрес доставки';
    errEl.style.display = 'block';
    return;
  }

  const address = `${city}, ${street}${comment ? ' (' + comment + ')' : ''}`;
  const btn = document.querySelector('button[onclick="placeOrder()"]');
  btn.disabled = true;
  btn.textContent = 'Оформление...';

  const res = await apiFetch('/orders', {
    method: 'POST',
    body: JSON.stringify({ delivery_address: address, payment_method: payment }),
  });

  btn.disabled = false;
  btn.textContent = 'Подтвердить заказ';

  if (res?.ok) {
    document.getElementById('checkout-layout').style.display = 'none';
    document.getElementById('checkout-success').style.display = 'block';
    document.getElementById('order-number').textContent = `#${res.data.order_id}`;
    updateCartBadge();
  } else {
    errEl.textContent = res?.data?.error || 'Ошибка оформления заказа';
    errEl.style.display = 'block';
  }
}

document.addEventListener('DOMContentLoaded', loadCheckout);
