/* ===== Add Product JS ===== */

async function loadCategories() {
  const res = await apiFetch('/categories');
  if (!res?.ok) return;
  const select = document.getElementById('product-category');
  res.data.forEach(cat => {
    const opt = document.createElement('option');
    opt.value = cat.id;
    opt.textContent = `${cat.icon || ''} ${cat.name}`;
    select.appendChild(opt);
  });
}

// ── Attributes ────────────────────────────────────────────────
function addAttrRow() {
  const list = document.getElementById('attributes-list');
  const row = document.createElement('div');
  row.className = 'attr-row';
  row.innerHTML = `
    <input type="text" class="form-control attr-name" placeholder="Название">
    <input type="text" class="form-control attr-value" placeholder="Значение">
    <button class="attr-remove" onclick="removeAttrRow(this)" title="Удалить">✕</button>
  `;
  list.appendChild(row);
}
function removeAttrRow(btn) { btn.closest('.attr-row').remove(); }

// ── Image upload helpers ───────────────────────────────────────
async function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  const token = localStorage.getItem('token');
  const resp = await fetch('/api/upload', {
    method: 'POST',
    headers: token ? { Authorization: 'Bearer ' + token } : {},
    body: formData,
  });
  const data = await resp.json();
  if (!resp.ok) throw new Error(data.error || 'Ошибка загрузки');
  return data.url;
}

// ── Main photo ────────────────────────────────────────────────
async function handleMainFile(input) {
  const file = input.files ? input.files[0] : input;
  if (!file) return;

  const zone = document.getElementById('main-upload-zone');
  const placeholder = document.getElementById('main-placeholder');
  const preview = document.getElementById('main-preview');
  const loading = document.getElementById('main-loading');
  const urlInput = document.getElementById('main-image-url');

  placeholder.style.display = 'none';
  preview.style.display = 'none';
  loading.style.display = 'flex';

  try {
    const url = await uploadFile(file);
    urlInput.value = url;
    document.getElementById('main-preview-img').src = url;
    loading.style.display = 'none';
    preview.style.display = 'block';
    zone.classList.add('has-image');
  } catch (e) {
    loading.style.display = 'none';
    placeholder.style.display = 'flex';
    showToast(e.message, 'error');
  }
  if (input.value !== undefined) input.value = '';
}

function initMainZoneDragDrop() {
  const zone = document.getElementById('main-upload-zone');
  if (!zone) return;
  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag-over'); });
  zone.addEventListener('dragleave', e => { if (!zone.contains(e.relatedTarget)) zone.classList.remove('drag-over'); });
  zone.addEventListener('drop', e => {
    e.preventDefault();
    zone.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
      handleMainFile(file);
    } else {
      showToast('Можно загружать только изображения', 'error');
    }
  });
}

// ── Extra photos ──────────────────────────────────────────────
let extraCount = 0;

function addExtraPhoto() {
  const list = document.getElementById('extra-photos-list');
  const id = ++extraCount;
  const item = document.createElement('div');
  item.className = 'extra-photo-item';
  item.id = `extra-photo-${id}`;
  item.innerHTML = `
    <input type="file" id="extra-file-${id}" accept="image/*" style="display:none"
           onchange="handleExtraFile(this, ${id})">
    <div class="extra-photo-zone" onclick="document.getElementById('extra-file-${id}').click()">
      <div class="extra-photo-placeholder" id="extra-ph-${id}">
        <span class="extra-photo-icon">📷</span>
        <span class="extra-photo-hint">Добавить</span>
      </div>
      <img class="extra-photo-img" id="extra-img-${id}" src="" style="display:none" alt="">
      <div class="extra-photo-loading" id="extra-load-${id}" style="display:none">
        <div class="spinner" style="width:24px;height:24px;border-width:2px;margin:0 auto"></div>
      </div>
    </div>
    <input type="hidden" class="extra-img-url" id="extra-url-${id}" value="">
    <button class="extra-photo-remove" onclick="event.stopPropagation();removeExtraPhoto(${id})" title="Удалить">✕</button>
  `;
  list.appendChild(item);
}

async function handleExtraFile(input, id) {
  const file = input.files[0];
  if (!file) return;

  const ph = document.getElementById(`extra-ph-${id}`);
  const img = document.getElementById(`extra-img-${id}`);
  const loading = document.getElementById(`extra-load-${id}`);
  const urlInput = document.getElementById(`extra-url-${id}`);

  ph.style.display = 'none';
  img.style.display = 'none';
  loading.style.display = 'flex';

  try {
    const url = await uploadFile(file);
    urlInput.value = url;
    img.src = url;
    loading.style.display = 'none';
    img.style.display = 'block';
    document.getElementById(`extra-photo-${id}`).classList.add('has-image');
  } catch (e) {
    loading.style.display = 'none';
    ph.style.display = 'flex';
    showToast(e.message, 'error');
  }
  input.value = '';
}

function removeExtraPhoto(id) {
  document.getElementById(`extra-photo-${id}`)?.remove();
}

// ── Discount preview ──────────────────────────────────────────
function updateDiscount() {
  const price = parseFloat(document.getElementById('product-price')?.value);
  const orig  = parseFloat(document.getElementById('product-orig-price')?.value);
  const preview = document.getElementById('discount-preview');
  const pctEl   = document.getElementById('discount-pct');
  if (preview && pctEl && orig > price && price > 0) {
    preview.style.display = 'block';
    pctEl.textContent = `-${Math.round((1 - price / orig) * 100)}%`;
  } else if (preview) {
    preview.style.display = 'none';
  }
}

// ── Init ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  if (!isLoggedIn()) { window.location.href = '/login'; return; }
  loadCategories();
  initMainZoneDragDrop();
  document.getElementById('product-price')?.addEventListener('input', updateDiscount);
  document.getElementById('product-orig-price')?.addEventListener('input', updateDiscount);
});

// ── Submit ────────────────────────────────────────────────────
async function submitProduct() {
  if (!isLoggedIn()) { window.location.href = '/login'; return; }

  const errEl = document.getElementById('add-product-error');
  errEl.style.display = 'none';

  const name       = document.getElementById('product-name').value.trim();
  const categoryId = parseInt(document.getElementById('product-category').value);
  const brand       = document.getElementById('product-brand').value.trim();
  const description = document.getElementById('product-description').value.trim();
  const mainImage   = document.getElementById('main-image-url').value.trim();
  const price       = parseFloat(document.getElementById('product-price').value);
  const origPrice   = parseFloat(document.getElementById('product-orig-price').value) || 0;
  const stock       = parseInt(document.getElementById('product-stock').value);
  const delivery    = parseInt(document.getElementById('product-delivery').value) || 3;

  if (!name)            { showErr(errEl, 'Введите название товара'); return; }
  if (!categoryId)      { showErr(errEl, 'Выберите категорию'); return; }
  if (!mainImage)       { showErr(errEl, 'Загрузите главное фото товара'); return; }
  if (!price || price <= 0) { showErr(errEl, 'Введите корректную цену'); return; }
  if (isNaN(stock) || stock < 0) { showErr(errEl, 'Введите остаток товара'); return; }

  const attributes = [];
  document.querySelectorAll('.attr-row').forEach(row => {
    const n = row.querySelector('.attr-name')?.value.trim();
    const v = row.querySelector('.attr-value')?.value.trim();
    if (n && v) attributes.push({ name: n, value: v });
  });

  const images = [];
  document.querySelectorAll('.extra-img-url').forEach(inp => {
    if (inp.value.trim()) images.push(inp.value.trim());
  });

  const payload = {
    name, category_id: categoryId, brand, description, main_image: mainImage,
    price, original_price: origPrice, stock, delivery_days: delivery,
    attributes, images,
  };

  const btn = document.querySelector('button[onclick="submitProduct()"]');
  btn.disabled = true;
  btn.textContent = 'Публикация...';

  const res = await apiFetch('/seller/products', { method: 'POST', body: JSON.stringify(payload) });

  btn.disabled = false;
  btn.textContent = 'Опубликовать товар';

  if (res?.ok) {
    showToast('Товар успешно опубликован!');
    setTimeout(() => window.location.href = '/seller/dashboard', 1200);
  } else {
    showErr(errEl, res?.data?.error || 'Ошибка публикации товара');
  }
}

function showErr(el, msg) {
  el.textContent = msg;
  el.style.display = 'block';
  el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function addImageRow() { addExtraPhoto(); }
