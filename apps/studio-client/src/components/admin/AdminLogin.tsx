import { useState } from 'react';

interface LoginViewProps {
  adminEmail: string;
  setAdminEmail: (val: string) => void;
  adminPassword: string;
  setAdminPassword: (val: string) => void;
  adminError: string;
  handleAdminLogin: () => void;
  otpRequired: boolean;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
  totpSetupRequired: boolean;
  provisioningUri: string;
}

export function LoginView({
  adminEmail,
  setAdminEmail,
  adminPassword,
  setAdminPassword,
  adminError,
  handleAdminLogin,
  otpRequired,
  adminOtp,
  setAdminOtp,
  totpSetupRequired,
  provisioningUri,
}: LoginViewProps) {
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
          onSubmit={(e) => {
            e.preventDefault();
            handleAdminLogin();
          }}
        >
          {!otpRequired && (
            <>
              <input
                type="email"
                placeholder="Admin Email"
                value={adminEmail}
                onChange={e => setAdminEmail(e.target.value)}
                className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
                required
              />
              <input
                type="password"
                placeholder="Password"
                value={adminPassword}
                onChange={e => setAdminPassword(e.target.value)}
                className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
                required
              />
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
            <input
              type="text"
              placeholder="Enter 6-digit OTP"
              value={adminOtp}
              onChange={e => setAdminOtp(e.target.value)}
              className="w-full text-center bg-[#07090f] border border-slate-800 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-[#00f3ff] transition-all font-mono tracking-widest"
              required
            />
          )}

          {adminError && <div className="text-[#ff4d4f] text-xs mt-1 font-mono">{adminError}</div>}

          <button
            type="submit"
            className="cyber-button w-full uppercase py-3 text-xs tracking-wider font-mono font-bold mt-2"
          >
            {otpRequired ? 'Verify OTP' : 'Authorize Access'}
          </button>
        </form>
      </div>
    </div>
  );
}
