/* ===== Product Detail Page JS ===== */

const productId = window.location.pathname.split('/').pop();
let selectedSellerId = null;
let product = null;

async function loadProduct() {
  const res = await apiFetch(`/products/${productId}`);
  document.getElementById('product-loading').style.display = 'none';

  if (!res?.ok) {
    document.getElementById('product-content').innerHTML =
      '<div class="empty-state"><div class="empty-icon">❌</div><h3>Товар не найден</h3></div>';
    document.getElementById('product-content').style.display = 'block';
    return;
  }

  product = res.data;
  document.title = `${product.name} — Tumar Market`;

  // Breadcrumb
  const bnEl = document.getElementById('product-breadcrumb-name');
  if (bnEl) bnEl.textContent = product.name.slice(0, 50);

  // Brand
  document.getElementById('product-brand').textContent = product.brand || '';

  // Title
  document.getElementById('product-title').textContent = product.name;

  // Rating
  const ratingEl = document.getElementById('product-rating');
  if (ratingEl) {
    ratingEl.innerHTML = `${renderStars(product.rating || 0)} <span class="rating-score">${(product.rating || 0).toFixed(1)}</span>`;
  }
  const rcEl = document.getElementById('product-reviews-count');
  if (rcEl) rcEl.textContent = `${product.review_count || 0} отзывов`;

  // Main image
  const mainImg = document.getElementById('main-image');
  if (mainImg) mainImg.src = product.main_image || 'https://via.placeholder.com/400x400?text=No+Image';
  mainImg.onerror = () => mainImg.src = 'https://via.placeholder.com/400x400?text=No+Image';

  // Thumbnails
  const thumbs = [product.main_image, ...(product.images || [])].filter(Boolean);
  const thumbList = document.getElementById('thumb-list');
  if (thumbList && thumbs.length > 1) {
    thumbList.innerHTML = thumbs.map((img, i) => `
      <div class="thumb ${i === 0 ? 'active' : ''}" onclick="selectImage('${img}', this)">
        <img src="${img}" onerror="this.src='https://via.placeholder.com/64x64'">
      </div>
    `).join('');
  }

  // Sellers
  renderSellers(product.sellers || []);

  // Attributes
  if (product.attributes?.length) {
    const attrsSection = document.getElementById('attrs-section');
    const attrsTable = document.getElementById('attrs-table');
    if (attrsSection && attrsTable) {
      attrsSection.style.display = 'block';
      attrsTable.innerHTML = product.attributes.map(a => `
        <tr><td>${a.name}</td><td>${a.value}</td></tr>
      `).join('');
    }
  }

  // Description
  if (product.description) {
    const descSection = document.getElementById('desc-section');
    const descEl = document.getElementById('product-description');
    if (descSection && descEl) {
      descSection.style.display = 'block';
      descEl.textContent = product.description;
    }
  }

  document.getElementById('product-content').style.display = 'block';
  loadReviews();
}

function renderSellers(sellers) {
  const el = document.getElementById('sellers-list');
  if (!el) return;

  if (!sellers.length) {
    el.innerHTML = '<div class="empty-state"><p>Нет в наличии</p></div>';
    return;
  }

  el.innerHTML = sellers.map((s, i) => {
    const discount = s.original_price && s.original_price > s.price
      ? Math.round((1 - s.price / s.original_price) * 100) : 0;
    return `
      <div class="seller-card ${i === 0 ? 'selected' : ''}" onclick="selectSeller(${s.id}, this)">
        <div class="seller-price-col">
          <div class="seller-price">
            ${formatPrice(s.price)}
            ${discount > 0 ? `<span class="seller-discount">-${discount}%</span>` : ''}
          </div>
          ${s.original_price && s.original_price > s.price
            ? `<div class="seller-old-price">${formatPrice(s.original_price)}</div>` : ''}
        </div>
        <div class="seller-info-col">
          <div class="seller-name">🏪 ${s.store?.name || 'Магазин'}</div>
          <div class="seller-delivery">🚚 Доставка ${s.delivery_days} дн.</div>
          <div class="seller-stock">✓ В наличии: ${s.stock} шт.</div>
        </div>
        <div class="seller-actions">
          <button class="btn-buy-now" onclick="buyNow(event, ${s.id})">Купить</button>
          <button class="btn-cart-sm" onclick="openCartModal(event, ${s.id})">В корзину</button>
        </div>
      </div>
    `;
  }).join('');

  if (sellers.length) selectedSellerId = sellers[0].id;
}

function selectSeller(id, el) {
  selectedSellerId = id;
  document.querySelectorAll('.seller-card').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
}

function selectImage(src, el) {
  document.getElementById('main-image').src = src;
  document.querySelectorAll('.thumb').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}

function openCartModal(e, sellerId) {
  e.stopPropagation();
  if (!isLoggedIn()) { showToast('Войдите для добавления в корзину', 'error'); window.location.href = '/login'; return; }
  selectedSellerId = sellerId;
  const seller = product?.sellers?.find(s => s.id === sellerId);
  const infoEl = document.getElementById('modal-seller-info');
  if (infoEl && seller) {
    infoEl.innerHTML = `
      <div style="background:var(--gray-50);border-radius:8px;padding:12px;margin-bottom:16px">
        <div style="font-size:18px;font-weight:700">${formatPrice(seller.price)}</div>
        <div style="font-size:13px;color:var(--gray-500)">Продавец: ${seller.store?.name || '—'}</div>
      </div>
    `;
  }
  document.getElementById('cart-qty').value = 1;
  document.getElementById('cart-modal').style.display = 'flex';
}

function closeModal(id) {
  document.getElementById(id).style.display = 'none';
}

function changeQty(delta) {
  const input = document.getElementById('cart-qty');
  const val = Math.max(1, parseInt(input.value) + delta);
  input.value = val;
}

async function confirmAddToCart() {
  const qty = parseInt(document.getElementById('cart-qty').value);
  const res = await apiFetch('/cart', {
    method: 'POST',
    body: JSON.stringify({ product_seller_id: selectedSellerId, quantity: qty }),
  });
  if (res?.ok) {
    closeModal('cart-modal');
    showToast('Товар добавлен в корзину ✓');
    updateCartBadge();
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
}

async function buyNow(e, sellerId) {
  e.stopPropagation();
  if (!isLoggedIn()) { window.location.href = '/login'; return; }
  const res = await apiFetch('/cart', {
    method: 'POST',
    body: JSON.stringify({ product_seller_id: sellerId, quantity: 1 }),
  });
  if (res?.ok) window.location.href = '/checkout';
  else showToast(res?.data?.error || 'Ошибка', 'error');
}

// Reviews
async function loadReviews() {
  const res = await apiFetch(`/products/${productId}/reviews`);
  const el = document.getElementById('reviews-list');
  if (!el) return;

  if (!res?.ok || !res.data.length) {
    el.innerHTML = '<div class="empty-state"><div class="empty-icon">💬</div><h3>Отзывов пока нет</h3><p>Будьте первым!</p></div>';
    return;
  }

  el.innerHTML = res.data.map(r => `
    <div class="review-item">
      <div class="review-header">
        <div>
          <span class="review-author">${r.user_name}</span>
          <span style="margin-left:10px">${renderStars(r.rating)}</span>
        </div>
        <span class="review-date">${new Date(r.created_at).toLocaleDateString('ru-RU')}</span>
      </div>
      ${r.comment ? `<p class="review-text">${r.comment}</p>` : ''}
    </div>
  `).join('');
}

function openReviewModal() {
  if (!isLoggedIn()) { showToast('Войдите для написания отзыва', 'error'); return; }
  document.getElementById('review-rating').value = 0;
  document.getElementById('review-comment').value = '';
  document.querySelectorAll('.star-btn').forEach(s => s.classList.remove('active'));
  document.getElementById('review-modal').style.display = 'flex';
}

// Star rating init
document.addEventListener('DOMContentLoaded', () => {
  const starBtns = document.querySelectorAll('.star-btn');
  starBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const val = parseInt(btn.dataset.val);
      document.getElementById('review-rating').value = val;
      starBtns.forEach((s, i) => {
        s.classList.toggle('active', parseInt(s.dataset.val) <= val);
      });
    });
  });
});

async function submitReview() {
  const rating = parseInt(document.getElementById('review-rating').value);
  const comment = document.getElementById('review-comment').value.trim();
  if (!rating) { showToast('Поставьте оценку', 'error'); return; }

  const res = await apiFetch('/reviews', {
    method: 'POST',
    body: JSON.stringify({ product_id: parseInt(productId), rating, comment }),
  });
  if (res?.ok) {
    closeModal('review-modal');
    showToast('Отзыв добавлен!');
    loadReviews();
    refreshProductRating();
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
}

async function refreshProductRating() {
  const res = await apiFetch(`/products/${productId}`);
  if (!res?.ok) return;
  const p = res.data;
  const ratingEl = document.getElementById('product-rating');
  if (ratingEl) ratingEl.innerHTML = `${renderStars(p.rating || 0)} <span class="rating-score">${(p.rating || 0).toFixed(1)}</span>`;
  const rcEl = document.getElementById('product-reviews-count');
  if (rcEl) rcEl.textContent = `${p.review_count || 0} отзывов`;
}

// ── Product page favorites ────────────────────────────────────
async function loadProductFav() {
  const btn = document.getElementById('product-fav-btn');
  if (!btn) return;
  if (!isLoggedIn()) { btn.style.display = 'none'; return; }

  const res = await apiFetch('/favorites/ids');
  if (!res?.ok) return;
  const ids = new Set(res.data || []);
  const pid = parseInt(productId);
  btn.textContent = ids.has(pid) ? '❤️' : '🤍';
  btn.classList.toggle('fav-active', ids.has(pid));
}

async function toggleProductFav() {
  if (!isLoggedIn()) {
    showToast('Войдите для добавления в избранное', 'error');
    return;
  }
  const btn = document.getElementById('product-fav-btn');
  const res = await apiFetch('/favorites/toggle', {
    method: 'POST',
    body: JSON.stringify({ product_id: parseInt(productId) }),
  });
  if (res?.ok) {
    const isFav = res.data.favorited;
    btn.textContent = isFav ? '❤️' : '🤍';
    btn.classList.toggle('fav-active', isFav);
    showToast(isFav ? 'Добавлено в избранное' : 'Убрано из избранного');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadProduct();
  loadProductFav();
  // Close modal on overlay click
  document.querySelectorAll('.modal-overlay').forEach(m => {
    m.addEventListener('click', e => { if (e.target === m) m.style.display = 'none'; });
  });
});
