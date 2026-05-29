/* ===== Tumar Market — Shared JS ===== */

const API = '/api';
let notificationsOpen = false;

// Detect when running inside TumarSuperApp WebView
const isAppMode = navigator.userAgent.includes('TumarApp');
if (isAppMode) document.documentElement.classList.add('app-mode');

// ── Token / Auth ──────────────────────────────────────────────
function getToken() {
  return localStorage.getItem('token');
}
function setToken(token) {
  localStorage.setItem('token', token);
}
function clearToken() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}
function getUser() {
  const u = localStorage.getItem('user');
  return u ? JSON.parse(u) : null;
}
function setUser(user) {
  localStorage.setItem('user', JSON.stringify(user));
}
function isLoggedIn() {
  return !!getToken();
}

// ── API Fetch ─────────────────────────────────────────────────
async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(API + path, { ...options, headers });
  const data = await res.json().catch(() => ({}));

  if (res.status === 401) {
    clearToken();
    window.location.href = '/login';
    return null;
  }
  return { ok: res.ok, status: res.status, data };
}

// ── Toast notifications ───────────────────────────────────────
function showToast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// ── Notifications ─────────────────────────────────────────────
function notificationItemHTML(n) {
  const createdAt = n.created_at ? new Date(n.created_at) : null;
  const timeLabel = createdAt
    ? createdAt.toLocaleString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
    : '';
  return `
    <div class="notification-item ${n.is_read ? '' : 'unread'}">
      <span class="notification-unread-dot" aria-hidden="true"></span>
      <div class="notification-icon">🔔</div>
      <div class="notification-content">
        <div class="notification-title">${n.title}</div>
        <div class="notification-message">${n.message}</div>
        <div class="notification-meta">
          ${n.order_id ? `<span class="notification-order">Заказ #${n.order_id}</span>` : ''}
          ${timeLabel ? `<span>${timeLabel}</span>` : ''}
        </div>
      </div>
    </div>
  `;
}

function renderNotifications(list) {
  const panelList = document.getElementById('notification-panel-list');
  const homeList = document.getElementById('home-notifications');
  const notifications = list || [];
  const emptyHTML = '<div class="notification-empty">Пока нет уведомлений</div>';
  const html = notifications.length ? notifications.map(notificationItemHTML).join('') : emptyHTML;

  if (panelList) panelList.innerHTML = html;
  if (homeList) homeList.innerHTML = html;

  const unreadCount = notifications.filter(n => !n.is_read).length;
  updateNotificationBadge(unreadCount);
}

function updateNotificationBadge(count) {
  const badge = document.getElementById('notification-badge');
  if (!badge) return;
  if (count > 0) {
    badge.textContent = count > 9 ? '9+' : String(count);
    badge.classList.add('visible');
  } else {
    badge.textContent = '';
    badge.classList.remove('visible');
  }
}

function ensureNotificationPanel() {
  let panel = document.getElementById('notification-panel');
  if (!panel) {
    panel = document.createElement('div');
    panel.id = 'notification-panel';
    panel.className = 'notification-panel';
    panel.innerHTML = `
      <div class="notification-panel-header">
        <div>
          <div class="notification-panel-title">Уведомления</div>
          <div class="notification-panel-subtitle">Последние обновления</div>
        </div>
        <button class="notification-panel-close" type="button" onclick="closeNotifications()">✕</button>
      </div>
      <div id="notification-panel-list" class="notification-list"></div>
    `;
    document.body.appendChild(panel);
  }
  return panel;
}

function toggleNotifications(event) {
  event?.stopPropagation();
  const panel = ensureNotificationPanel();
  notificationsOpen = !panel.classList.contains('visible');
  panel.classList.toggle('visible', notificationsOpen);
  if (notificationsOpen) {
    markNotificationsRead().then(loadNotifications);
  }
}

function closeNotifications() {
  const panel = document.getElementById('notification-panel');
  if (!panel) return;
  panel.classList.remove('visible');
  notificationsOpen = false;
}

async function loadNotifications() {
  if (!isLoggedIn()) return;
  const res = await apiFetch('/notifications');
  if (res?.ok) renderNotifications(res.data || []);
}

async function markNotificationsRead() {
  if (!isLoggedIn()) return;
  await apiFetch('/notifications/read', { method: 'PUT' });
}

function initNotifications() {
  const homeList = document.getElementById('home-notifications');
  if (!isLoggedIn()) {
    if (homeList) homeList.innerHTML = '<div class="notification-empty">Войдите, чтобы видеть уведомления</div>';
    return;
  }
  ensureNotificationPanel();
  loadNotifications();
  document.addEventListener('click', e => {
    if (!notificationsOpen) return;
    const panel = document.getElementById('notification-panel');
    const button = document.getElementById('notification-button');
    if (panel && !panel.contains(e.target) && button && !button.contains(e.target)) {
      closeNotifications();
    }
  });
}

// ── Price format ──────────────────────────────────────────────
function formatPrice(price) {
  if (!price && price !== 0) return '—';
  return Number(price).toLocaleString('ru-KZ') + ' ₸';
}

// ── Stars ─────────────────────────────────────────────────────
function renderStars(rating) {
  const full = Math.floor(rating);
  const half = rating % 1 >= 0.5;
  let stars = '';
  for (let i = 0; i < 5; i++) {
    if (i < full) stars += '★';
    else if (i === full && half) stars += '½';
    else stars += '☆';
  }
  return `<span class="stars">${stars}</span>`;
}

// ── Cart count ────────────────────────────────────────────────
async function updateCartBadge() {
  if (!isLoggedIn()) return;
  const res = await apiFetch('/cart');
  if (res?.ok) {
    const badge = document.getElementById('cart-badge');
    if (badge) badge.textContent = res.data.count || '';
  }
}

// ── Header rendering ──────────────────────────────────────────
function renderHeader() {
  const user = getUser();
  const navEl = document.getElementById('header-user-nav');
  if (!navEl) return;

  if (user) {
    navEl.innerHTML = `
      <a href="/profile" class="header-btn">
        <span class="icon">👤</span>
        <span>${user.name.split(' ')[0]}</span>
      </a>
      <button type="button" class="header-btn header-btn-notifications" id="notification-button" onclick="toggleNotifications(event)">
        <span class="icon notification-icon-bell">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M15 17H7a3 3 0 0 1-3-3v-1.5a7.5 7.5 0 0 1 6-7.3V4a2 2 0 1 1 4 0v1.2a7.5 7.5 0 0 1 6 7.3V14a3 3 0 0 1-3 3Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9.5 17a2.5 2.5 0 0 0 5 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="notification-badge" id="notification-badge"></span>
        </span>
        <span>Уведомления</span>
      </button>
      <a href="/cart" class="header-btn">
        <span class="cart-badge">
          <span class="icon">🛒</span>
          <span class="badge" id="cart-badge"></span>
        </span>
        <span>Корзина</span>
      </a>
      ${user.role === 'seller' || user.role === 'admin' ? `<a href="/seller/dashboard" class="header-btn"><span class="icon">🏪</span><span>Магазин</span></a>` : ''}
      <button onclick="logout()" class="header-btn"><span class="icon">🚪</span><span>Выйти</span></button>
    `;
    updateCartBadge();
  } else {
    navEl.innerHTML = `
      <a href="/login" class="header-btn"><span class="icon">👤</span><span>Войти</span></a>
      <a href="/cart" class="header-btn"><span class="icon">🛒</span><span>Корзина</span></a>
    `;
  }
}

async function logout() {
  await apiFetch('/auth/logout', { method: 'POST' });
  clearToken();
  showToast('Вы вышли из системы', 'info');
  setTimeout(() => window.location.href = '/', 500);
}

// ── Search ────────────────────────────────────────────────────
function initSearch() {
  const form = document.getElementById('search-form');
  const input = document.getElementById('search-input');
  if (!form || !input) return;

  form.addEventListener('submit', e => {
    e.preventDefault();
    const q = input.value.trim();
    if (q) window.location.href = `/search?q=${encodeURIComponent(q)}`;
  });

  const params = new URLSearchParams(window.location.search);
  const q = params.get('q');
  if (q) input.value = q;
}

// ── Favorites ──────────────────────────────────────────────────
let _favIds = new Set();

async function loadFavIds() {
  if (!isLoggedIn()) return;
  const res = await apiFetch('/favorites/ids');
  if (res?.ok) _favIds = new Set(res.data || []);
}

async function toggleFav(e, productId) {
  e.stopPropagation();
  e.preventDefault();
  if (!isLoggedIn()) {
    showToast('Войдите для добавления в избранное', 'error');
    return;
  }
  const btn = e.currentTarget;
  const res = await apiFetch('/favorites/toggle', {
    method: 'POST',
    body: JSON.stringify({ product_id: productId })
  });
  if (res?.ok) {
    const isFav = res.data.favorited;
    if (isFav) { _favIds.add(productId); } else { _favIds.delete(productId); }
    btn.textContent = isFav ? '❤️' : '🤍';
    btn.classList.toggle('fav-active', isFav);
    showToast(isFav ? 'Добавлено в избранное' : 'Убрано из избранного');
  }
}

// ── Product card ──────────────────────────────────────────────
function productCardHTML(p) {
  const isFav = _favIds.has(p.id);
  const discount = p.min_price && p.original_price && p.original_price > p.min_price
    ? Math.round((1 - p.min_price / p.original_price) * 100) : 0;
  return `
    <div class="product-card" onclick="window.location='/product/${p.id}'">
      <div class="product-card-img">
        <img src="${p.main_image || 'https://placehold.co/300x300/f3f4f6/9ca3af?text=Фото'}"
             alt="${p.name}" loading="lazy"
             onerror="this.src='https://placehold.co/300x300/f3f4f6/9ca3af?text=Фото'">
        ${discount > 0 ? `<span class="product-card-badge">-${discount}%</span>` : ''}
        <button class="fav-btn${isFav ? ' fav-active' : ''}" onclick="toggleFav(event, ${p.id})">${isFav ? '❤️' : '🤍'}</button>
      </div>
      <div class="product-card-body">
        <div class="product-card-rating">
          ${renderStars(p.rating || 0)} <span>${p.review_count || 0}</span>
        </div>
        <div class="product-card-name">${p.name}</div>
        <div class="product-card-price">${p.min_price ? formatPrice(p.min_price) : 'Нет в наячии'}</div>
        ${p.original_price && p.original_price > p.min_price ? `<div class="product-card-price-old">${formatPrice(p.original_price)}</div>` : ''}
        <button class="btn-add-cart" onclick="addToCart(event, ${p.id})">В корзину</button>
      </div>
    </div>
  `;
}

async function addToCart(e, productId) {
  e.stopPropagation();
  if (!isLoggedIn()) {
    showToast('Войдите для добавления в корзину', 'error');
    setTimeout(() => window.location.href = '/login', 1000);
    return;
  }
  // Get cheapest seller for this product
  const res = await apiFetch(`/products/${productId}`);
  if (!res?.ok || !res.data.sellers?.length) {
    showToast('Товар недоступен', 'error');
    return;
  }
  const sellerId = res.data.sellers[0].id;
  const addRes = await apiFetch('/cart', {
    method: 'POST',
    body: JSON.stringify({ product_seller_id: sellerId, quantity: 1 }),
  });
  if (addRes?.ok) {
    showToast('Товар добавлен в корзину ✓');
    updateCartBadge();
  } else {
    showToast(addRes?.data?.error || 'Ошибка', 'error');
  }
}

// ── Nav categories with mega-menu ────────────────────────────
function buildNavCategories(categories) {
  const nav = document.getElementById('categories-nav');
  if (!nav) return;

  const parents = categories.filter(c => !c.parent_id);
  const childrenMap = {};
  categories.filter(c => c.parent_id).forEach(c => {
    if (!childrenMap[c.parent_id]) childrenMap[c.parent_id] = [];
    childrenMap[c.parent_id].push(c);
  });

  let dropdown = document.getElementById('nav-mega-dropdown');
  if (!dropdown) {
    dropdown = document.createElement('div');
    dropdown.id = 'nav-mega-dropdown';
    dropdown.className = 'nav-mega-dropdown';
    document.body.appendChild(dropdown);
  }

  let hideTimer = null;
  const activeSlug = new URLSearchParams(window.location.search).get('category');

  function showDropdown(li, children) {
    clearTimeout(hideTimer);
    const rect = li.getBoundingClientRect();
    dropdown.innerHTML = children
      .map(c => `<a href="/catalog?category=${c.slug}">${c.name}</a>`)
      .join('');
    dropdown.style.top = rect.bottom + 'px';
    dropdown.style.left = Math.min(rect.left, window.innerWidth - 290) + 'px';
    dropdown.classList.add('visible');
  }

  function scheduleHide() {
    hideTimer = setTimeout(() => dropdown.classList.remove('visible'), 100);
  }

  dropdown.addEventListener('mouseenter', () => clearTimeout(hideTimer));
  dropdown.addEventListener('mouseleave', scheduleHide);

  // "Все категории" first item
  const allLi = document.createElement('li');
  allLi.className = !activeSlug ? 'nav-active' : '';
  const allA = document.createElement('a');
  allA.href = '/catalog';
  allA.textContent = '☰ Все категории';
  allLi.appendChild(allA);
  nav.appendChild(allLi);

  parents.forEach(parent => {
    const children = childrenMap[parent.id] || [];
    const li = document.createElement('li');
    if (activeSlug === parent.slug || children.some(c => c.slug === activeSlug)) {
      li.className = 'nav-active';
    }
    const a = document.createElement('a');
    a.href = `/catalog?category=${parent.slug}`;
    a.textContent = (parent.icon ? parent.icon + ' ' : '') + parent.name;
    li.appendChild(a);

    if (children.length > 0) {
      li.addEventListener('mouseenter', () => showDropdown(li, children));
      li.addEventListener('mouseleave', scheduleHide);
    }
    nav.appendChild(li);
  });
}

// ── On DOM ready ──────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  // Try to restore user from API if token exists
  (async () => {
    if (isLoggedIn() && !getUser()) {
      const res = await apiFetch('/auth/me');
      if (res?.ok) setUser(res.data);
      else clearToken();
    }
    renderHeader();
    initSearch();
    initNotifications();
  })();
});
