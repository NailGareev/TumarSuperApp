/* ===== Profile Page JS ===== */

async function loadProfile() {
  if (!isLoggedIn()) { window.location.href = '/login?redirect=/profile'; return; }

  const res = await apiFetch('/auth/me');
  document.getElementById('profile-loading').style.display = 'none';

  if (!res?.ok) { clearToken(); window.location.href = '/login'; return; }

  const user = res.data;
  setUser(user);

  document.getElementById('profile-name').textContent = user.name;
  document.getElementById('profile-email').textContent = user.email;

  const roleEl = document.getElementById('profile-role');
  const roleMap = {
    user: { label: 'Покупатель', cls: 'role-user' },
    seller: { label: 'Продавец', cls: 'role-seller' },
    admin: { label: 'Администратор', cls: 'role-admin' },
  };
  const r = roleMap[user.role] || roleMap.user;
  roleEl.textContent = r.label;
  roleEl.className = `profile-role ${r.cls}`;

  // Edit form
  document.getElementById('edit-name').value = user.name;
  document.getElementById('edit-phone').value = user.phone || '';

  // Orders count
  const ordRes = await apiFetch('/orders');
  if (ordRes?.ok) {
    document.getElementById('stat-orders').textContent = ordRes.data.length;
  }

  // Seller section
  if (user.role === 'seller' || user.role === 'admin') {
    document.getElementById('seller-stat').style.display = 'block';
  } else {
    document.getElementById('seller-promo').style.display = 'flex';
  }

  document.getElementById('profile-content').style.display = 'block';
}

document.getElementById('profile-form')?.addEventListener('submit', async e => {
  e.preventDefault();
  const name = document.getElementById('edit-name').value.trim();
  const phone = document.getElementById('edit-phone').value.trim();
  const res = await apiFetch('/auth/profile', {
    method: 'PUT',
    body: JSON.stringify({ name, phone, avatar: '' }),
  });
  if (res?.ok) {
    const user = getUser();
    if (user) { user.name = name; user.phone = phone; setUser(user); }
    showToast('Профиль обновлён');
    document.getElementById('profile-name').textContent = name;
  } else {
    showToast(res?.data?.error || 'Ошибка', 'error');
  }
});

document.addEventListener('DOMContentLoaded', loadProfile);
