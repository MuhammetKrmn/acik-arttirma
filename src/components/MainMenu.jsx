import React, { useState, useEffect } from 'react';

export default function MainMenu(props) {
  const [username, setUsername] = useState("Bağlanıyor...");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openRoomsCount, setOpenRoomsCount] = useState(0);

  useEffect(() => {
    let isMounted = true;

    async function initPlayerAndServerStatus() {
      try {
        setIsLoading(true);
        setError(null);

        // Daha önce alınmış geçerli bir oturum var mı kontrol et
        let savedToken = localStorage.getItem('playerToken');
        let savedUsername = localStorage.getItem('playerUsername');

        if (!savedToken || !savedUsername) {
          // Yoksa Backend'den gerçek misafir kaydı oluştur
          const randomGuestName = `Guest_${Math.floor(Math.random() * 100000)}`;
          const res = await fetch('http://localhost:8080/api/guests', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: randomGuestName }),
          });

          if (!res.ok) {
            throw new Error(`Sunucu hatası (Kod: ${res.status})`);
          }

          const data = await res.json();
          savedToken = data.playerToken || data.token;
          savedUsername = data.username || randomGuestName;

          localStorage.setItem('playerToken', savedToken);
          localStorage.setItem('playerUsername', savedUsername);
        }

        if (isMounted) {
          setUsername(savedUsername);
        }

        // Backend RoomController'daki GET /api/rooms uç noktasından açık oda sayısını çek
        const roomsRes = await fetch('http://localhost:8080/api/rooms');
        if (roomsRes.ok && isMounted) {
          const rooms = await roomsRes.json();
          setOpenRoomsCount(Array.isArray(rooms) ? rooms.length : 0);
        }

      } catch (err) {
        console.error("Ana menü backend bağlantı hatası:", err);
        if (isMounted) {
          setError("Sunucuya bağlanılamadı");
          setUsername("Çevrimdışı");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    initPlayerAndServerStatus();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleQuickPlay = () => {
    if (isLoading || error) return;
    
    // Rastgele mod seçimi (kullanıcı seçmeden arka planda atanır)
    const modes = ["Şampiyonlar Ligi", "Premier Lig", "Dünya Kupası"];
    const randomMode = modes[Math.floor(Math.random() * modes.length)];
    
    // App.jsx'ten gelen prop'lar içindeki ilk fonksiyonu otomatik bul (ismi ne olursa olsun)
    const triggerTransition = Object.values(props).find(val => typeof val === 'function');
    
    if (triggerTransition) {
      triggerTransition(randomMode, "Orta Zorluk");
    } else {
      console.warn("App.jsx içindeki <MainMenu /> etiketine hiçbir fonksiyon prop olarak verilmemiş! Gelen proplar:", props);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col justify-between relative select-none p-8">
      
      {/* Arka Plan Işık Efektleri */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-indigo-600/10 blur-[140px] rounded-full pointer-events-none"></div>
      <div className="absolute bottom-0 right-0 w-[400px] h-[400px] bg-emerald-600/5 blur-[120px] rounded-full pointer-events-none"></div>

      {/* Üst Bar: Logo ve Gerçek Backend Oyuncu Profili */}
      <div className="flex justify-between items-center z-10 border-b border-slate-800/80 pb-6">
        <div className="flex items-center gap-3">
          <div className="w-3 h-8 bg-indigo-500 rounded-full shadow-[0_0_15px_rgba(99,102,241,0.6)]"></div>
          <span className="text-2xl font-black tracking-tighter text-slate-100">
            DRAFT<span className="text-indigo-500">ARENA</span>
          </span>
        </div>

        {/* Oyuncu Adı Kartı */}
        <div className="flex items-center gap-4 bg-slate-900/90 border border-slate-800 px-5 py-2.5 rounded-2xl shadow-lg backdrop-blur-md">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-xl shadow-inner">
            👑
          </div>
          <div className="flex flex-col">
            <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">
              Oyuncu Profili
            </span>
            <div className="flex items-center gap-2">
              <span className="text-base font-black tracking-wide text-white">
                {username}
              </span>
              <span className={`w-2 h-2 rounded-full ${error ? 'bg-red-500' : isLoading ? 'bg-amber-400 animate-ping' : 'bg-emerald-400 shadow-[0_0_8px_#34d399]'}`}></span>
            </div>
          </div>
        </div>
      </div>

      {/* Orta Alan: Odak Noktası ve Hızlı Oyuna Gir Butonu */}
      <div className="flex-1 flex flex-col items-center justify-center z-10 my-auto">
        <div className="relative flex flex-col items-center max-w-lg w-full text-center">
          
          {/* Canlı Sunucu Durum Rozeti */}
          <div className="mb-6 inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-900 border border-slate-800 text-xs font-bold tracking-widest uppercase text-slate-400 shadow-sm">
            <span className="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span>
            {error ? (
              <span className="text-red-400">{error}</span>
            ) : (
              <span>Açık Odalar: <strong className="text-white">{openRoomsCount}</strong></span>
            )}
          </div>

          <h1 className="text-6xl font-black tracking-tight text-white mb-4 drop-shadow-2xl">
            KADRONU <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-emerald-400">KUR</span>
          </h1>
          
          <p className="text-slate-400 text-sm font-medium max-w-md mb-10 leading-relaxed">
            Canlı açık arttırmada bütçeni en iyi şekilde yönet, rakiplerinin hamlelerini boşa çıkar ve en güçlü 11'i sahaya sür.
          </p>

          {/* Oyuna Gir Butonu */}
          <button
            onClick={handleQuickPlay}
            disabled={isLoading || !!error}
            className={`group relative w-full max-w-xs py-5 px-8 rounded-2xl font-black text-xl tracking-widest uppercase transition-all duration-300 shadow-2xl overflow-hidden ${
              isLoading || error
                ? 'bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-700'
                : 'bg-indigo-600 hover:bg-indigo-500 text-white hover:scale-105 active:scale-95 shadow-indigo-500/25 border border-indigo-400/30'
            }`}
          >
            <span className="relative z-10 flex items-center justify-center gap-3">
              {isLoading ? 'Bağlanıyor...' : 'Oyuna Gir'}
              {!isLoading && !error && (
                <svg className="w-6 h-6 transition-transform duration-300 group-hover:translate-x-1" fill="none" stroke="currentColor" strokeWidth="3" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              )}
            </span>
          </button>
        </div>
      </div>

      {/* Alt Bilgi Barı: Oyun Kuralları Özeti */}
      <div className="grid grid-cols-3 gap-6 max-w-3xl mx-auto w-full z-10 pt-6 border-t border-slate-900">
        <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-4 text-center">
          <div className="text-indigo-400 font-black text-lg mb-0.5">1000M €</div>
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Başlangıç Bütçesi</div>
        </div>
        <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-4 text-center">
          <div className="text-emerald-400 font-black text-lg mb-0.5">Canlı İhale</div>
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Gerçek Zamanlı Teklif</div>
        </div>
        <div className="bg-slate-900/40 border border-slate-800/60 rounded-2xl p-4 text-center">
          <div className="text-amber-400 font-black text-lg mb-0.5">Rastgele Havuz</div>
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Otomatik Eşleştirme</div>
        </div>
      </div>

    </div>
  );
}