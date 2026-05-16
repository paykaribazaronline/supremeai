import { signInWithEmailAndPassword } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    const loading = document.getElementById('loading');
    const loginButton = document.getElementById('loginButton');
    const rememberMe = document.getElementById('rememberMe');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');

    // Load remembered email if present
    const savedEmail = localStorage.getItem('supremeai_remember_email');
    if (savedEmail) {
        emailInput.value = savedEmail;
        rememberMe.checked = true;
        passwordInput.focus();
    }

    // Form submit handler
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearMessages();
        
        const email = emailInput.value.trim();
        const password = passwordInput.value;

        if (!email || !password) {
            showError('Please enter both email and password');
            return;
        }

        try {
            loading.classList.add('show');
            loginButton.disabled = true;

            // 1. INITIALIZE FIREBASE (via AuthHelper)
            await AuthHelper.initFirebase();

            // 2. FIREBASE AUTHENTICATION (MODULAR)
            const userCredential = await signInWithEmailAndPassword(AuthHelper.auth, email, password);
            const idToken = await userCredential.user.getIdToken();

            // 3. EXCHANGE FIREBASE TOKEN FOR SERVER JWT
            const response = await fetch('/api/auth/firebase-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ idToken: idToken })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Server authentication failed');
            }

            const data = await response.json();

            if (data.success) {
                localStorage.setItem('supremeai_firebase_authenticated', 'true');

                if (data.data && data.data.token) {
                    localStorage.setItem('supremeai_token', data.data.token);
                }

                if (data.data && data.data.user) {
                    AuthHelper.setUser({
                        uid: data.data.user.id,
                        email: data.data.user.email,
                        username: data.data.user.username,
                        role: data.data.user.role,
                        tier: data.data.user.tier
                    });
                }

                if (rememberMe.checked) {
                    localStorage.setItem('supremeai_remember_email', email);
                } else {
                    localStorage.removeItem('supremeai_remember_email');
                }

                showSuccess('✅ Login successful! Redirecting...');
                const urlParams = new URLSearchParams(window.location.search);
                const redirect = urlParams.get('redirect') || '/admin.html';
                window.location.href = redirect;
            } else {
                throw new Error(data.message || 'Authentication failed');
            }

        } catch (error) {
            console.error('Login error:', error);
            let errorMsg = 'Login failed';
            
            // Map common Firebase errors
            if (error.code) {
                switch (error.code) {
                    case 'auth/invalid-credential':
                        errorMsg = '❌ Invalid email or password';
                        break;
                    case 'auth/too-many-requests':
                        errorMsg = '❌ Too many attempts. Try again later';
                        break;
                    case 'auth/network-request-failed':
                        errorMsg = '❌ Network error. Check connection';
                        break;
                    default:
                        errorMsg = '❌ ' + error.message;
                }
            } else {
                errorMsg = '❌ ' + error.message;
            }
            showError(errorMsg);
            loginButton.disabled = false;
        } finally {
            loading.classList.remove('show');
        }
    });

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.add('show');
    }

    function showSuccess(message) {
        successMessage.textContent = message;
        successMessage.classList.add('show');
    }

    function clearMessages() {
        errorMessage.classList.remove('show');
        successMessage.classList.remove('show');
    }
});
