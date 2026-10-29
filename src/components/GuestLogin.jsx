import React, { useState, useEffect } from 'react';

export default function GuestLogin({ onLoginSuccess }) {
  const [username, setUsername] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Sayfa açıldığında otomatik havalı bir misafir ismi üret
  useEffect(() => {
    const randomName = `Guest_${Math.floor(Math.random() * 90000) + 10000}`;
    setUsername(randomName);
  }, []);

  const handleGuestLogin = async (e) => {
    e.preventDefault();
    if (!username.trim()) return;

    setIsLoading(true);
    setError(null);

    try {
      // Backend GuestController /api/guests uç noktasına istek atıyoruz
      const response = await fetch('http://localhost:8080/api/guests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim() }),
      });

      if (!response.ok) {
        throw new Error(`Giriş başarısız oldu! (Kod: ${response.status})`);
      }

      const data = await response.json();
      const token = data.playerToken || data.token;
      const registeredUsername = data.username || username.trim();

      // Token ve kullanıcı adını localStorage'a güvenle kaydediyoruz
      localStorage.setItem('playerToken', token);
      localStorage.setItem('playerUsername', registeredUsername);

      console.log("🎟️ Misafir Girişi Başarılı, Token:", token);

      if (onLoginSuccess) {
        onLoginSuccess(registeredUsername, token);
      }
    } catch (err) {
      console.error("Misafir girişi hatası:", err);
      setError("Sunucuya bağlanılamadı. Backend'in açık olduğundan emin ol.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefreshName = () => {
    const randomName = `Guest_${Math.floor(Math.random() * 90000) + 10000}`;
    setUsername(randomName);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans flex items-center justify-center relative select-none p-6">
      {/* Arka Plan Işık Efektleri */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-600/10 blur-[120px] rounded-full pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-emerald-600/10 blur-[120px] rounded-full pointer-events-none"></div>

      <div className="relative z-10 max-w-md w-full bg-slate-900/80 border border-slate-800 rounded-3xl p-8 shadow-2xl backdrop-blur-xl">
        
        {/* Başlık */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 text-3xl mb-4 shadow-inner">
            ⚡
          </div>
          <h1 className="text-3xl font-black tracking-tight text-white">
            DRAFT<span className="text-indigo-500">ARENA</span>
          </h1>
          <p className="text-slate-400 text-sm mt-2">
            Hızlıca misafir olarak giriş yap ve hemen açık arttırmaya başla!
          </p>
        </div>

        {/* Hata Mesajı */}
        {error && (
          <div className="mb-6 bg-red-950/80 border border-red-500/50 text-red-300 px-4 py-3 rounded-2xl text-xs font-bold shadow-lg animate-pulse">
            ⚠️ {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleGuestLogin} className="space-y-6">
          <div>
            <label className="block text-xs font-bold uppercase tracking-widest text-slate-400 mb-2">
              Oyuncu Adın (Misafir)
            </label>
            <div className="relative flex items-center">
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                maxLength={20}
                className="w-full bg-slate-950 border border-slate-800 rounded-2xl px-5 py-4 text-white font-bold tracking-wide focus:outline-none focus:border-indigo-500 transition-colors shadow-inner"
                placeholder="İsim girin..."
              />
              <button
                type="button"
                onClick={handleRefreshName}
                title="Rastgele İsim Üret"
                className="absolute right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-xl text-slate-300 transition-colors"
              >
                🔄
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading || !username.trim()}
            className="w-full py-4 px-6 rounded-2xl font-black text-base tracking-widest uppercase bg-indigo-600 hover:bg-indigo-500 text-white transition-all shadow-lg shadow-indigo-500/25 hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed border border-indigo-400/30 flex items-center justify-center gap-2"
          >
            {isLoading ? (
              <span>Bağlanıyor...</span>
            ) : (
              <>
                <span>Hızlı Giriş Yap</span>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="3" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </>
            )}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-[11px] text-slate-500 font-medium">
            Kayıt gerektirmez. Token'ın tarayıcında güvenle saklanır.
          </p>
        </div>

      </div>
    </div>
  );
}