/* ===== Favorites Page JS ===== */

async function loadFavorites() {
  if (!isLoggedIn()) {
    window.location.href = '/login?redirect=/favorites';
    return;
  }

  await loadFavIds();

  const res = await apiFetch('/favorites');
  document.getElementById('fav-loading').style.display = 'none';

  if (!res?.ok) return;

  const favs = res.data || [];
  const countEl = document.getElementById('fav-count');
  countEl.textContent = favs.length ? `${favs.length} товар${favs.length === 1 ? '' : favs.length < 5 ? 'а' : 'ов'}` : '';

  if (!favs.length) {
    document.getElementById('fav-empty').style.display = 'block';
    return;
  }

  const grid = document.getElementById('fav-grid');
  grid.innerHTML = favs.map(f => productCardHTML({
    id: f.product_id,
    name: f.name,
    main_image: f.main_image,
    rating: f.rating,
    review_count: f.review_count,
    min_price: f.min_price,
  })).join('');
}

document.addEventListener('DOMContentLoaded', () => {
  loadFavorites();
});
