/* ===== Catalog Page JS ===== */

let currentPage = 1;
const params = new URLSearchParams(window.location.search);
let _allCategories = [];

async function loadCategories() {
  const res = await apiFetch('/categories');
  if (!res || !res.ok || !Array.isArray(res.data)) return;
  _allCategories = res.data;

  buildNavCategories(res.data);

  if (isAppMode) {
    renderMobileCatalog();
    return;
  }

  renderDesktopSidebar(res.data);
  updateTitle();
  updateBreadcrumb();
  await loadFavIds();
  loadProducts(1);
}

function renderDesktopSidebar(data) {
  const list = document.getElementById('filter-categories');
  if (!list) return;

  const activeSlug = params.get('category');
  const parents = data.filter(c => !c.parent_id);
  const childrenMap = {};
  data.forEach(c => {
    if (c.parent_id) {
      if (!childrenMap[c.parent_id]) childrenMap[c.parent_id] = [];
      childrenMap[c.parent_id].push(c);
    }
  });

  let html = `<li><a href="/catalog" class="${!activeSlug ? 'active' : ''}">📦 Все категории</a></li>`;

  parents.forEach(p => {
    const ch = childrenMap[p.id] || [];
    const parentActive = p.slug === activeSlug;
    const childActive = ch.some(c => c.slug === activeSlug);
    const isOpen = parentActive || childActive;

    html += `<li class="filter-cat-parent">
      <a href="/catalog?category=${encodeURIComponent(p.slug)}" class="${parentActive ? 'active' : ''}">
        ${p.icon ? `<span>${p.icon}</span>` : ''}<span>${p.name}</span>
      </a>`;

    if (isOpen && ch.length) {
      html += `<ul class="filter-cat-children">`;
      ch.forEach(c => {
        html += `<li><a href="/catalog?category=${encodeURIComponent(c.slug)}" class="${c.slug === activeSlug ? 'active' : ''}">${c.name}</a></li>`;
      });
      html += `</ul>`;
    }

    html += `</li>`;
  });

  list.innerHTML = html;
}

function mobileCatGrid(cats) {
  return `<div class="category-grid">${cats.map(cat => `
    <a class="category-card" href="/catalog?category=${encodeURIComponent(cat.slug)}">
      <div class="category-icon-wrap"><span class="category-icon">${cat.icon || '📦'}</span></div>
      <div class="category-name">${cat.name}</div>
    </a>`).join('')}</div>`;
}

function setMobileCatalogHeader(show) {
  const h = document.querySelector('.catalog-header');
  if (h) h.style.display = show ? '' : 'none';
}

function renderMobileCatalog() {
  const slug = params.get('category');
  const grid = document.getElementById('products-grid');
  const sidebar = document.querySelector('.catalog-sidebar');
  if (sidebar) sidebar.style.display = 'none';

  if (!slug) {
    // Step 1: show parent categories
    document.title = 'Каталог — Tumar Market';
    setMobileCatalogHeader(false);
    grid.innerHTML = mobileCatGrid(_allCategories.filter(c => !c.parent_id));
    return;
  }

  const selectedCat = _allCategories.find(c => c.slug === slug);
  if (!selectedCat) { loadProductsInApp(); return; }

  if (!selectedCat.parent_id) {
    // Step 2: show subcategories of selected parent
    const children = _allCategories.filter(c => c.parent_id === selectedCat.id);
    if (children.length) {
      document.title = `${selectedCat.name} — Tumar Market`;
      setMobileCatalogHeader(false);
      grid.innerHTML = mobileCatGrid(children);
      return;
    }
  }

  // Step 3: subcategory selected — show products
  document.title = `${selectedCat.name} — Tumar Market`;
  const titleEl = document.getElementById('catalog-title');
  if (titleEl) titleEl.textContent = selectedCat.name;
  setMobileCatalogHeader(true);
  loadProductsInApp();
}

async function loadProductsInApp() {
  await loadFavIds();
  loadProducts(1);
}

async function loadProducts(page = 1) {
  currentPage = page;
  const grid = document.getElementById('products-grid');
  grid.innerHTML = '<div class="loading-wrapper"><div class="spinner"></div></div>';

  const category = params.get('category') || '';
  const sort = document.getElementById('sort-select')?.value || 'newest';
  const minPrice = document.getElementById('filter-min-price')?.value || '';
  const maxPrice = document.getElementById('filter-max-price')?.value || '';

  let url = `/products?page=${page}&limit=20&sort=${sort}`;
  if (category) url += `&category=${category}`;
  if (minPrice) url += `&min_price=${minPrice}`;
  if (maxPrice) url += `&max_price=${maxPrice}`;

  const res = await apiFetch(url);
  if (!res?.ok) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-icon">❌</div><h3>Ошибка загрузки</h3></div>';
    return;
  }

  const { products, total, pages } = res.data;
  if (!products.length) {
    grid.innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><h3>Товары не найдены</h3><p>Попробуйте изменить фильтры</p></div>';
  } else {
    grid.innerHTML = products.map(productCardHTML).join('');
  }

  const countEl = document.getElementById('products-count');
  if (countEl) countEl.textContent = `${total} товаров`;

  renderPagination(page, pages);
}

function applyFilters() {
  loadProducts(1);
}

function renderPagination(page, totalPages) {
  const el = document.getElementById('pagination');
  if (!el || totalPages <= 1) { if (el) el.innerHTML = ''; return; }

  let html = '';
  if (page > 1) html += `<div class="page-btn" onclick="loadProducts(${page - 1})">‹</div>`;

  const start = Math.max(1, page - 2);
  const end = Math.min(totalPages, page + 2);
  if (start > 1) html += `<div class="page-btn" onclick="loadProducts(1)">1</div><span>...</span>`;
  for (let i = start; i <= end; i++) {
    html += `<div class="page-btn ${i === page ? 'active' : ''}" onclick="loadProducts(${i})">${i}</div>`;
  }
  if (end < totalPages) html += `<span>...</span><div class="page-btn" onclick="loadProducts(${totalPages})">${totalPages}</div>`;
  if (page < totalPages) html += `<div class="page-btn" onclick="loadProducts(${page + 1})">›</div>`;

  el.innerHTML = html;
}

function updateBreadcrumb() {
  const cat = params.get('category');
  if (!cat) return;
  const el = document.getElementById('breadcrumb-category');
  if (el) el.textContent = cat;
}

async function updateTitle() {
  const slug = params.get('category');
  if (!slug) return;
  const cat = _allCategories.find(c => c.slug === slug);
  if (cat) {
    document.title = `${cat.name} — Tumar Market`;
    const titleEl = document.getElementById('catalog-title');
    if (titleEl) titleEl.textContent = cat.name;
    const bcEl = document.getElementById('breadcrumb-category');
    if (bcEl) bcEl.textContent = cat.name;
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  loadCategories();
});
