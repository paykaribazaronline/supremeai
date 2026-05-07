        const loginForm = document.getElementById('loginForm');
        const errorMessage = document.getElementById('errorMessage');
        const successMessage = document.getElementById('successMessage');
        const loading = document.getElementById('loading');
        const loginButton = document.getElementById('loginButton');
        const rememberMe = document.getElementById('rememberMe');
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');

        // Initialize AuthHelper (Firebase config already inside it)
        let auth;
        AuthHelper.initFirebase().then(() => {
            auth = firebase.auth();
            console.log('Firebase Auth initialized');
        });
        
        // Page load
        window.addEventListener('DOMContentLoaded', () => {
            // Load remembered email if present
            const savedEmail = localStorage.getItem('supremeai_remember_email');
            if (savedEmail) {
                emailInput.value = savedEmail;
                rememberMe.checked = true;
                passwordInput.focus();
            }
        
            // Check if already logged in - but don't redirect here (handled in form submit)
        });

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

                // 1. FIREBASE AUTHENTICATION
                let idToken;
                if (!auth) {
                    await AuthHelper.initFirebase();
                    auth = window.firebase.auth();
                }
                const userCredential = await auth.signInWithEmailAndPassword(email, password);
                idToken = await userCredential.user.getIdToken();

                // 2. EXCHANGE FIREBASE TOKEN FOR SERVER JWT (to establish backend session)
// backend expects payload: { idToken: <Firebase ID token> }
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
                    // Set authenticated flag for AuthHelper
                    localStorage.setItem('supremeai_firebase_authenticated', 'true');

                    // Store the server-issued JWT token
                    if (data.data && data.data.token) {
                        localStorage.setItem('supremeai_token', data.data.token);
                    }

                    // Store user info from server (includes role/tier)
                    if (data.data && data.data.user) {
                        AuthHelper.setUser({
                            uid: data.data.user.id,
                            email: data.data.user.email,
                            username: data.data.user.username,
                            role: data.data.user.role,
                            tier: data.data.user.tier
                        });
                    }

                    // Remember email if checkbox is checked
                    if (rememberMe.checked) {
                        localStorage.setItem('supremeai_remember_email', email);
                    } else {
                        localStorage.removeItem('supremeai_remember_email');
                    }

                    showSuccess('✅ Login successful! Redirecting...');
                    // Redirect immediately
                    const urlParams = new URLSearchParams(window.location.search);
                    const redirect = urlParams.get('redirect') || '/admin.html';
                    window.location.href = redirect;
                } else {
                    const errMsg = (data.error && data.error.message) || data.message || 'Authentication failed';
                    throw new Error(errMsg);
                }

            } catch (error) {
                console.error('Login error:', error);
                let errorMsg = 'Login failed';
                if (error.code) {
                    switch (error.code) {
                        case 'auth/user-not-found':
                        case 'auth/wrong-password':
                        case 'auth/invalid-credential':
                            errorMsg = '❌ Invalid email or password';
                            break;
                        case 'auth/invalid-email':
                            errorMsg = '❌ Invalid email format';
                            break;
                        case 'auth/too-many-requests':
                            errorMsg = '❌ Too many attempts. Try again later';
                            break;
                        case 'auth/network-request-failed':
                            errorMsg = '❌ Network error. Check your connection';
                            break;
                        case 'auth/user-disabled':
                            errorMsg = '❌ Account disabled';
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
