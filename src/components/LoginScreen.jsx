import { useState } from 'react';
import { api } from '../api';

// Uygulama açılınca ilk ekran: misafir girişi.
export default function LoginScreen({ onLoggedIn }) {
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const trimmed = username.trim();
  const valid = trimmed.length >= 2 && trimmed.length <= 20;

  const submit = async (e) => {
    e.preventDefault();
    if (!valid || loading) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.guestLogin(trimmed);
      onLoggedIn({ userId: data.userId, username: data.username, playerToken: data.playerToken });
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white font-sans flex items-center justify-center p-6 select-none animate-fade-in">
      <form
        onSubmit={submit}
        className="w-full max-w-md rounded-3xl border border-white/20 p-8 flex flex-col gap-6 shadow-[0_0_50px_rgba(79,70,229,0.4)]"
        style={{ background: 'linear-gradient(to bottom right, #4f46e5, #581c87)' }}
      >
        <div className="text-center">
          <h1 className="text-4xl font-black uppercase tracking-wider drop-shadow-lg">Açık Arttırma</h1>
          <p className="text-white/70 font-bold text-sm mt-2 tracking-wide">3 oyuncu · 1000M € bütçe · 11 kişilik kadro</p>
        </div>

        <div className="bg-black/30 rounded-2xl p-4 backdrop-blur-sm flex flex-col gap-2">
          <label htmlFor="username" className="text-xs font-bold text-white/70 uppercase tracking-wider">
            Kullanıcı adın
          </label>
          <input
            id="username"
            autoFocus
            maxLength={20}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="ör. Kursat"
            className="bg-slate-950/70 border border-white/10 rounded-xl px-4 py-3 text-lg font-bold focus:outline-none focus:border-white/60"
          />
          <p className="text-[11px] text-white/50">2-20 karakter. Hesap gerekmez, misafir olarak girersin.</p>
        </div>

        {error && (
          <div className="bg-red-950/80 border border-red-500/50 text-red-200 px-4 py-2 rounded-xl text-sm font-bold">
            ⚠️ {error}
          </div>
        )}

        <button
          type="submit"
          disabled={!valid || loading}
          className="w-full bg-slate-900 text-white font-black text-xl py-4 rounded-2xl hover:bg-slate-800 hover:scale-105 transition-all shadow-xl uppercase tracking-widest border border-white/10 disabled:opacity-50 disabled:hover:scale-100"
        >
          {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
        </button>
      </form>
    </div>
  );
}