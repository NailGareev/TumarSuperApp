/* ===== Home Page JS ===== */

async function loadCategories() {
  const res = await apiFetch('/categories');
  if (!res?.ok) return;

  buildNavCategories(res.data);

  const parents = res.data.filter(c => !c.parent_id);
  const grid = document.getElementById('category-grid');
  if (grid) {
    grid.innerHTML = parents.map(cat => `
      <a class="category-card" href="/catalog?category=${cat.slug}">
        <div class="category-icon-wrap"><span class="category-icon">${cat.icon || '📦'}</span></div>
        <div class="category-name">${cat.name}</div>
      </a>
    `).join('');
  }
}

async function loadFeaturedProducts() {
  const el = document.getElementById('featured-products');
  if (!el) return;
  const res = await apiFetch('/products/featured');
  if (!res?.ok) { el.innerHTML = '<p class="empty-state">Ошибка загрузки</p>'; return; }
  if (!res.data.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><h3>Нет товаров</h3></div>'; return; }
  el.innerHTML = res.data.map(productCardHTML).join('');
}

async function loadNewProducts() {
  const el = document.getElementById('new-products');
  if (!el) return;
  const res = await apiFetch('/products?sort=newest&limit=8');
  if (!res?.ok) { el.innerHTML = ''; return; }
  el.innerHTML = (res.data.products || []).map(productCardHTML).join('');
}

// Hero slider auto-rotate
function initHeroSlider() {
  const slides = document.querySelectorAll('.hero-slide');
  const dotsContainer = document.getElementById('hero-dots');
  if (!slides.length) return;

  slides.forEach((_, i) => {
    const dot = document.createElement('div');
    dot.className = 'hero-dot' + (i === 0 ? ' active' : '');
    dot.onclick = () => goToSlide(i);
    dotsContainer?.appendChild(dot);
  });

  let current = 0;
  function goToSlide(idx) {
    slides[current].classList.remove('active');
    document.querySelectorAll('.hero-dot')[current]?.classList.remove('active');
    current = idx;
    slides[current].classList.add('active');
    document.querySelectorAll('.hero-dot')[current]?.classList.add('active');
  }

  setInterval(() => goToSlide((current + 1) % slides.length), 4000);
}

document.addEventListener('DOMContentLoaded', async () => {
  initHeroSlider();
  loadCategories();
  await loadFavIds();
  loadFeaturedProducts();
  loadNewProducts();
});
