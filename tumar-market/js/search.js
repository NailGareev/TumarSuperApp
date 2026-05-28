/* ===== Search Page JS ===== */

const params = new URLSearchParams(window.location.search);
const query = params.get('q') || '';
let currentPage = 1;

async function loadCategories() {
  const res = await apiFetch('/categories');
  if (!res?.ok) return;
  const nav = document.getElementById('categories-nav');
  if (nav) res.data.slice(0, 10).forEach(c => {
    nav.innerHTML += `<li><a href="/catalog?category=${c.slug}">${c.icon} ${c.name}</a></li>`;
  });
}

async function doSearch(page = 1) {
  if (!query) { document.getElementById('search-results').innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><h3>Введите поисковый запрос</h3></div>'; return; }

  currentPage = page;
  const loading = document.getElementById('search-loading');
  const results = document.getElementById('search-results');
  loading.style.display = 'flex';
  results.innerHTML = '';

  const res = await apiFetch(`/search?q=${encodeURIComponent(query)}&page=${page}&limit=20`);
  loading.style.display = 'none';

  const queryDisplay = document.getElementById('search-query-display');
  const countEl = document.getElementById('search-count');
  const titleEl = document.getElementById('search-title');

  if (queryDisplay) queryDisplay.textContent = `"${query}"`;
  if (titleEl) titleEl.textContent = `Поиск: "${query}"`;
  document.title = `${query} — Tumar Market`;

  if (!res?.ok) {
    results.innerHTML = '<div class="empty-state"><div class="empty-icon">❌</div><h3>Ошибка поиска</h3></div>';
    return;
  }

  const { products, total } = res.data;
  if (countEl) countEl.textContent = `Найдено ${total} товаров`;

  if (!products.length) {
    results.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">🔍</div>
        <h3>Ничего не найдено</h3>
        <p>По запросу "${query}" товары не найдены</p>
        <a href="/catalog" class="btn btn-primary" style="margin-top:16px">Смотреть все товары</a>
      </div>`;
    return;
  }

  results.innerHTML = products.map(productCardHTML).join('');

  const totalPages = Math.ceil(total / 20);
  const pagEl = document.getElementById('search-pagination');
  if (pagEl && totalPages > 1) {
    let html = '';
    if (page > 1) html += `<div class="page-btn" onclick="doSearch(${page-1})">‹</div>`;
    for (let i = Math.max(1, page-2); i <= Math.min(totalPages, page+2); i++) {
      html += `<div class="page-btn ${i===page?'active':''}" onclick="doSearch(${i})">${i}</div>`;
    }
    if (page < totalPages) html += `<div class="page-btn" onclick="doSearch(${page+1})">›</div>`;
    pagEl.innerHTML = html;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  loadCategories();
  doSearch(1);
});
