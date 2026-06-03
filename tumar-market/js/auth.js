/* ===== Auth JS (login + register) ===== */

function togglePassword(fieldId = 'password') {
  const inp = document.getElementById(fieldId);
  if (inp) inp.type = inp.type === 'password' ? 'text' : 'password';
}

function fillDemo(email, password) {
  const emailEl = document.getElementById('email');
  const passEl = document.getElementById('password');
  if (emailEl) emailEl.value = email;
  if (passEl) passEl.value = password;
}

// ── LOGIN ─────────────────────────────────────────────────────
const loginForm = document.getElementById('login-form');
if (loginForm) {
  loginForm.addEventListener('submit', async e => {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const errEl = document.getElementById('login-error');
    const btn = loginForm.querySelector('button[type="submit"]');

    errEl.style.display = 'none';
    btn.disabled = true;
    btn.textContent = 'Загрузка...';

    const res = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    btn.disabled = false;
    btn.textContent = 'Войти';

    if (res?.ok) {
      setToken(res.data.token);
      setUser(res.data.user);
      showToast('Добро пожаловать, ' + res.data.user.name + '!');
      const redirect = new URLSearchParams(window.location.search).get('redirect') || '/';
      setTimeout(() => window.location.href = redirect, 500);
    } else {
      errEl.textContent = res?.data?.error || 'Ошибка авторизации';
      errEl.style.display = 'block';
    }
  });
}

// ── REGISTER ──────────────────────────────────────────────────
const registerForm = document.getElementById('register-form');
if (registerForm) {
  // Password strength
  const passInput = document.getElementById('password');
  if (passInput) {
    passInput.addEventListener('input', () => {
      const val = passInput.value;
      const fill = document.getElementById('strength-fill');
      const label = document.getElementById('strength-label');
      if (!fill || !label) return;
      if (val.length < 6) {
        fill.className = 'strength-fill weak';
        label.textContent = 'Слабый пароль';
      } else if (val.length < 10 || !/[A-Z]/.test(val) || !/[0-9]/.test(val)) {
        fill.className = 'strength-fill medium';
        label.textContent = 'Средний пароль';
      } else {
        fill.className = 'strength-fill strong';
        label.textContent = 'Надёжный пароль';
      }
    });
  }

  registerForm.addEventListener('submit', async e => {
    e.preventDefault();
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const password = document.getElementById('password').value;
    const password2 = document.getElementById('password2').value;
    const agree = document.getElementById('agree').checked;
    const errEl = document.getElementById('register-error');
    const btn = registerForm.querySelector('button[type="submit"]');

    errEl.style.display = 'none';

    if (!name) { errEl.textContent = 'Введите имя'; errEl.style.display = 'block'; return; }
    if (password !== password2) { errEl.textContent = 'Пароли не совпадают'; errEl.style.display = 'block'; return; }
    if (password.length < 6) { errEl.textContent = 'Пароль минимум 6 символов'; errEl.style.display = 'block'; return; }
    if (!agree) { errEl.textContent = 'Необходимо принять условия'; errEl.style.display = 'block'; return; }

    btn.disabled = true;
    btn.textContent = 'Регистрация...';

    const res = await apiFetch('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, name, phone }),
    });

    btn.disabled = false;
    btn.textContent = 'Зарегистрироваться';

    if (res?.ok) {
      setToken(res.data.token);
      setUser(res.data.user);
      showToast('Аккаунт создан! Добро пожаловать!');
      setTimeout(() => window.location.href = '/', 700);
    } else {
      errEl.textContent = res?.data?.error || 'Ошибка регистрации';
      errEl.style.display = 'block';
    }
  });
}

// Redirect if already logged in
document.addEventListener('DOMContentLoaded', () => {
  if (isLoggedIn() && (loginForm || registerForm)) {
    const user = getUser();
    if (user) {
      window.location.href = '/';
    }
  }
});
