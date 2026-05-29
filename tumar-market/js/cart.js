/* ===== Cart Page JS ===== */

async function loadCart() {
  if (!isLoggedIn()) {
    window.location.href = '/login?redirect=/cart';
    return;
  }

  document.getElementById('cart-loading').style.display = 'flex';

  const res = await apiFetch('/cart');
  document.getElementById('cart-loading').style.display = 'none';

  if (!res?.ok) return;

  const { items, total, count } = res.data;

  const titleEl = document.getElementById('cart-count-title');
  if (titleEl) titleEl.textContent = count ? `(${count} товара)` : '';

  if (!items.length) {
    document.getElementById('cart-empty').style.display = 'block';
    document.getElementById('cart-layout').style.display = 'none';
    return;
  }

  document.getElementById('cart-empty').style.display = 'none';
  document.getElementById('cart-layout').style.display = 'grid';

  renderCartItems(items);
  renderSummary(items, total);
  updateCartBadge();
}

function renderCartItems(items) {
  const el = document.getElementById('cart-items-list');
  el.innerHTML = items.map(item => isAppMode
    ? renderCartItemMobile(item)
    : renderCartItemDesktop(item)
  ).join('');
}

function cartImg(item, size) {
  const src = item.product_image || '';
  return `<img class="cart-item-img" src="${src}" style="width:${size}px;height:${size}px"
       onerror="this.src='https://placehold.co/${size}x${size}?text=📦'" alt="">`;
}

function qtyCtrl(item) {
  return `<div class="qty-ctrl">
    <div class="qty-ctrl-btn" onclick="changeItemQty(${item.id},${item.product_seller_id},${item.quantity-1})">−</div>
    <input type="number" class="qty-ctrl-val" value="${item.quantity}" min="1"
           onchange="changeItemQty(${item.id},${item.product_seller_id},parseInt(this.value))">
    <div class="qty-ctrl-btn" onclick="changeItemQty(${item.id},${item.product_seller_id},${item.quantity+1})">+</div>
  </div>`;
}

function renderCartItemMobile(item) {
  return `
    <div class="cart-item cart-item-mobile" id="cart-item-${item.id}">
      <div class="cart-item-top">
        ${cartImg(item, 72)}
        <div class="cart-item-info">
          <div class="cart-item-name">
            <a href="/product/${item.product_id}" style="color:inherit">${item.product_name}</a>
          </div>
          <div class="cart-item-store">🏪 ${item.store_name}</div>
          <div class="cart-item-delivery">🚚 Доставка ${item.delivery_days} дн.</div>
        </div>
        <div class="cart-item-remove" onclick="removeItem(${item.id})">✕</div>
      </div>
      <div class="cart-item-bottom">
        ${qtyCtrl(item)}
        <div class="cart-item-price">${formatPrice(item.price * item.quantity)}</div>
      </div>
    </div>`;
}

function renderCartItemDesktop(item) {
  return `
    <div class="cart-item" id="cart-item-${item.id}">
      ${cartImg(item, 84)}
      <div class="cart-item-info">
        <div class="cart-item-name">
          <a href="/product/${item.product_id}" style="color:inherit">${item.product_name}</a>
        </div>
        <div class="cart-item-store">🏪 ${item.store_name}</div>
        <div class="cart-item-delivery">🚚 Доставка ${item.delivery_days} дн.</div>
      </div>
      ${qtyCtrl(item)}
      <div class="cart-item-price">${formatPrice(item.price * item.quantity)}</div>
      <div class="cart-item-remove" onclick="removeItem(${item.id})">✕</div>
    </div>`;
}

function renderSummary(items, total) {
  const rowsEl = document.getElementById('summary-rows');
  // Group by store
  const stores = {};
  items.forEach(item => {
    if (!stores[item.store_name]) stores[item.store_name] = 0;
    stores[item.store_name] += item.price * item.quantity;
  });
  rowsEl.innerHTML = Object.entries(stores).map(([name, subtotal]) => `
    <div class="summary-row"><span>🏪 ${name}</span><span>${formatPrice(subtotal)}</span></div>
  `).join('');

  document.getElementById('cart-total-price').textContent = formatPrice(total);
}

async function changeItemQty(itemId, psId, qty) {
  if (qty < 1) { await removeItem(itemId); return; }
  const res = await apiFetch(`/cart/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify({ quantity: qty }),
  });
  if (res?.ok) loadCart();
  else showToast(res?.data?.error || 'Ошибка', 'error');
}

async function removeItem(itemId) {
  const res = await apiFetch(`/cart/${itemId}`, { method: 'DELETE' });
  if (res?.ok) {
    showToast('Товар удалён из корзины', 'info');
    loadCart();
  }
}

document.addEventListener('DOMContentLoaded', loadCart);
