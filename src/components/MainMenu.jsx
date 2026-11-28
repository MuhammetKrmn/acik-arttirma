import { useState } from 'react';
import { api } from '../api';

// Ana menü. Konsept ve zorluk seçimi yok: konsepti oda kurulurken sunucu rastgele seçer.
export default function MainMenu({ session, onJoined, onLogout }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const play = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);
    try {
      const room = await api.quickJoin(session.playerToken);
      onJoined(room);
    } catch (err) {
      // Veritabanı sıfırlandıysa eski token geçersiz olur: yeniden giriş gerekir.
      if (err.code === 'TOKEN_INVALID' || err.code === 'TOKEN_REQUIRED') {
        onLogout();
        return;
      }
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-white font-sans overflow-hidden flex flex-col relative select-none animate-fade-in">
      <div className="absolute top-6 right-8 flex items-center gap-3 z-50">
        <div className="flex items-center gap-2 bg-slate-800/80 px-4 py-2 rounded-full border border-slate-700 backdrop-blur-sm shadow-lg">
          <svg className="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
          <span className="font-bold tracking-wide">{session.username}</span>
        </div>
        <button
          onClick={onLogout}
          className="px-4 py-2 rounded-full bg-slate-800/80 border border-slate-700 text-slate-300 hover:text-white hover:border-slate-500 text-sm font-bold transition-colors"
        >
          Çıkış
        </button>
      </div>

      <div className="flex-1 flex items-center justify-center px-10">
        <div
          className="w-80 h-[460px] rounded-3xl flex flex-col items-center p-6 border border-white/20 scale-105 shadow-[0_0_50px_rgba(79,70,229,0.4)]"
          style={{ background: 'linear-gradient(to bottom right, #4f46e5, #581c87)' }}
        >
          <div className="flex-1 flex flex-col items-center justify-center text-center mt-4 gap-4">
            <h2 className="text-4xl font-black uppercase tracking-wider drop-shadow-lg">Hızlı Oyun</h2>
            <div className="text-white/80 text-sm font-bold flex flex-col gap-1">
              <span>3 oyuncu · 1000M € bütçe</span>
              <span>22 ihale · 11 kişilik kadro</span>
              <span>Konsept rastgele seçilir</span>
            </div>
          </div>

          {error && (
            <div className="w-full bg-red-950/80 border border-red-500/50 text-red-200 px-3 py-2 rounded-xl text-xs font-bold mb-3 text-center">
              ⚠️ {error}
            </div>
          )}

          <button
            onClick={play}
            disabled={loading}
            className="w-full bg-slate-900 text-white font-black text-xl py-4 rounded-2xl hover:bg-slate-800 hover:scale-105 transition-all shadow-xl uppercase tracking-widest border border-white/10 disabled:opacity-60 disabled:hover:scale-100"
          >
            {loading ? 'Oda aranıyor...' : 'Oyuna Gir'}
          </button>
        </div>
      </div>
    </div>
  );
}