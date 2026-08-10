import { useState, useRef, useEffect } from 'react';

interface LoginViewProps {
  adminEmail: string;
  setAdminEmail: (val: string) => void;
  adminError: string;
  handleAdminLogin: (password?: string) => void;
  otpRequired: boolean;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
  totpSetupRequired: boolean;
  provisioningUri: string;
}

const MAX_ATTEMPTS = 5;
const LOCKOUT_MS = 15 * 60 * 1000; // 15 minutes

export function LoginView({
  adminEmail,
  setAdminEmail,
  adminError,
  handleAdminLogin,
  otpRequired,
  adminOtp,
  setAdminOtp,
  totpSetupRequired,
  provisioningUri,
}: LoginViewProps) {
  const [localPassword, setLocalPassword] = useState('');
  const [localError, setLocalError] = useState('');
  const [attempts, setAttempts] = useState(0);
  const [lockedUntil, setLockedUntil] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const lockoutTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [currentTime, setCurrentTime] = useState(() => Date.now());

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (lockedUntil !== null) {
      interval = setInterval(() => setCurrentTime(Date.now()), 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [lockedUntil]);

  const isLocked = lockedUntil !== null && currentTime < lockedUntil;
  const lockoutRemaining = lockedUntil ? Math.max(0, Math.ceil((lockedUntil - currentTime) / 1000)) : 0;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Rate limiting check
    if (isLocked) {
      setLocalError(`Too many failed attempts. Please wait ${lockoutRemaining} seconds.`);
      return;
    }

    // Email validation
    if (!otpRequired) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(adminEmail)) {
        setLocalError('Please enter a valid email address.');
        return;
      }
      if (localPassword.length < 8) {
        setLocalError('Password must be at least 8 characters.');
        return;
      }
    }

    // OTP validation
    if (otpRequired) {
      if (!/^\d{6}$/.test(adminOtp)) {
        setLocalError('OTP must be exactly 6 digits.');
        return;
      }
    }

    setLocalError('');
    setIsSubmitting(true);

    try {
      handleAdminLogin(localPassword);
      if (!otpRequired) {
        setLocalPassword('');
      }
    } catch {
      // Increment attempt counter for rate limiting
      const newAttempts = attempts + 1;
      setAttempts(newAttempts);
      if (newAttempts >= MAX_ATTEMPTS) {
        const lockUntil = Date.now() + LOCKOUT_MS;
        setLockedUntil(lockUntil);
        setAttempts(0);
        setLocalError(`Too many failed attempts. Account locked for 15 minutes.`);
        if (lockoutTimerRef.current) clearTimeout(lockoutTimerRef.current);
        lockoutTimerRef.current = setTimeout(() => setLockedUntil(null), LOCKOUT_MS);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-1 flex items-center justify-center p-6">
      <div className="w-full max-w-md glass-card text-center flex flex-col gap-6 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#00f3ff] to-[#bc13fe]"></div>
        <div>
          <span className="text-5xl block mb-2 drop-shadow-[0_0_12px_#bc13fe]">👑</span>
          <h2 className="text-xl font-bold font-['Space_Grotesk'] tracking-widest uppercase">
            SupremeAI <span className="text-[#00f3ff]">Admin Gate</span>
          </h2>
          <p className="text-slate-400 text-xs mt-1">Authorized access only. Authentication protocol required.</p>
        </div>

        <form
          className="flex flex-col gap-3.5"
          onSubmit={handleSubmit}
          aria-label="Admin authentication form"
        >
          {!otpRequired && (
            <>
              <div>
                <label htmlFor="admin-email" className="sr-only">Admin Email</label>
                <input
                  id="admin-email"
                  type="email"
                  placeholder="Admin Email"
                  value={adminEmail}
                  onChange={e => setAdminEmail(e.target.value)}
                  className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
                  required
                  autoComplete="email"
                  aria-required="true"
                  aria-invalid={localError ? 'true' : 'false'}
                />
              </div>
              <div>
                <label htmlFor="admin-password" className="sr-only">Password</label>
                <input
                  id="admin-password"
                  type="password"
                  placeholder="Password"
                  value={localPassword}
                  onChange={e => setLocalPassword(e.target.value)}
                  className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
                  required
                  autoComplete="current-password"
                  aria-required="true"
                  aria-invalid={localError ? 'true' : 'false'}
                />
              </div>
            </>
          )}

          {totpSetupRequired && provisioningUri && (
            <div className="flex flex-col items-center gap-2 mb-2 bg-[#07090f] border border-slate-800 rounded-xl p-4">
              <p className="text-xs text-[#00f3ff] font-mono mb-1 text-center">SCAN TO SETUP 2FA (TOTP)</p>
              <img
                src={`https://chart.googleapis.com/chart?chs=200x200&chld=M|0&cht=qr&chl=${encodeURIComponent(provisioningUri)}`}
                alt="TOTP QR Code"
                className="rounded-lg w-40 h-40"
              />
              <p className="text-[10px] text-slate-500 font-mono mt-1 text-center">Scan with Google Authenticator or Authy</p>
            </div>
          )}

          {otpRequired && (
            <div>
              <label htmlFor="admin-otp" className="sr-only">Enter 6-digit OTP</label>
              <input
                id="admin-otp"
                type="text"
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
                placeholder="Enter 6-digit OTP"
                value={adminOtp}
                onChange={e => setAdminOtp(e.target.value.replace(/\D/g, ''))}
                className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
                required
                aria-required="true"
                aria-invalid={localError ? 'true' : 'false'}
              />
            </div>
          )}

          {(adminError || localError) && (
            <div className="text-[#ff4d4f] text-xs mt-1 font-mono" role="alert">
              {localError || adminError}
            </div>
          )}

          {isLocked && (
            <div className="text-amber-400 text-xs mt-1 font-mono" role="alert">
              🔒 Account temporarily locked. Retry in {lockoutRemaining}s.
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting || isLocked}
            className="cyber-button w-full uppercase py-3 text-xs tracking-wider font-mono font-bold mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Verifying...' : otpRequired ? 'Verify OTP' : 'Authorize Access'}
          </button>
        </form>
      </div>
    </div>
  );
}