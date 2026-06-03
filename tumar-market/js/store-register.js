/* ===== Store Register JS ===== */

function goStep(n) {
  document.querySelectorAll('.step-content').forEach(el => el.classList.remove('active'));
  document.getElementById(`step-${n}`).classList.add('active');

  document.querySelectorAll('.step').forEach((el, i) => {
    el.classList.remove('active', 'done');
    if (i + 1 < n) el.classList.add('done');
    if (i + 1 === n) el.classList.add('active');
  });
}

document.addEventListener('DOMContentLoaded', () => {
  // Check if already has a store
  (async () => {
    if (isLoggedIn()) {
      const meRes = await apiFetch('/auth/me');
      if (meRes?.ok) {
        setUser(meRes.data);
        document.getElementById('step1-logged-in').style.display = 'block';
        document.getElementById('step1-not-logged-in').style.display = 'none';
        document.getElementById('step1-user-name').textContent = meRes.data.name;

        // Check if already has store
        const storeRes = await apiFetch('/store/my');
        if (storeRes?.ok) {
          // Already has a store, redirect
          showToast('У вас уже есть зарегистрированный магазин', 'info');
          setTimeout(() => window.location.href = '/seller/dashboard', 1000);
          return;
        }
      } else {
        document.getElementById('step1-logged-in').style.display = 'none';
        document.getElementById('step1-not-logged-in').style.display = 'block';
      }
    } else {
      document.getElementById('step1-logged-in').style.display = 'none';
      document.getElementById('step1-not-logged-in').style.display = 'block';
    }
    renderHeader();
  })();

  const form = document.getElementById('store-register-form');
  if (!form) return;

  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!isLoggedIn()) { window.location.href = '/login?redirect=/seller/store-register'; return; }

    const errEl = document.getElementById('store-reg-error');
    const btn = form.querySelector('button[type="submit"]');
    errEl.style.display = 'none';

    const data = {
      name: document.getElementById('store-name').value.trim(),
      legal_name: document.getElementById('legal-name').value.trim(),
      bin_number: document.getElementById('bin-number').value.trim(),
      email: document.getElementById('store-email').value.trim(),
      phone: document.getElementById('store-phone').value.trim(),
      address: document.getElementById('store-address').value.trim(),
      description: document.getElementById('store-description').value.trim(),
    };

    if (!data.name || !data.legal_name || !data.bin_number || !data.email || !data.phone || !data.address) {
      errEl.textContent = 'Пожалуйста, заполните все обязательные поля';
      errEl.style.display = 'block';
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Регистрация...';

    const res = await apiFetch('/store/register', { method: 'POST', body: JSON.stringify(data) });

    btn.disabled = false;
    btn.textContent = 'Зарегистрировать магазин';

    if (res?.ok) {
      // Update user role in localStorage
      const user = getUser();
      if (user) { user.role = 'seller'; setUser(user); }
      goStep(3);
    } else {
      errEl.textContent = res?.data?.error || 'Ошибка регистрации магазина';
      errEl.style.display = 'block';
    }
  });
});
