import React, { useState, useEffect } from 'react';
import { socketService } from '../SocketService.js';

export default function Lobby({ modeName = "Şampiyonlar Ligi", difficulty = "Orta Zorluk", onLobbyComplete }) {
  // Statik yerine backend'den veya API'den geleceğe hazırlık dinamik state yapısı
  const [players, setPlayers] = useState([
    { id: 1, name: "Kürşat", isMe: true, isReady: false, avatar: "👑" },
    // Diğer oyuncular backend'deki oda verisinden dolacak
  ]);
  
  const [timeLeft, setTimeLeft] = useState(30);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  const circleRadius = 40;
  const circleCircumference = 2 * Math.PI * circleRadius;
  
  // 🟢 MİSAFİR GİRİŞİ VE ODA / TOKEN YÖNETİMİ
  useEffect(() => {
    let isMounted = true;

    async function initSession() {
      try {
        setIsInitializing(true);
        setErrorMsg(null);

        // Backend'e misafir girişi isteği
        const response = await fetch('http://localhost:8080/api/guests', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (!response.ok) {
          throw new Error(`Misafir girişi başarısız oldu! (Kod: ${response.status})`);
        }

        const data = await response.json();
        // Backend'den dönen yapıya göre token ve misafir adını alıyoruz
        const token = data.token || data.accessToken || data;
        const guestName = data.username || data.name || "Kürşat";

        if (!isMounted) return;

        console.log("🎟️ Token Alındı:", token);
        localStorage.setItem('playerToken', token);

        // Kullanıcının kendi adını gelen misafir adına göre güncelleyelim
        setPlayers(prev => prev.map(p => p.isMe ? { ...p, name: guestName } : p));

        const roomId = "1"; // Test odası

        const handleRoomEvent = (event) => {
          console.log("📢 Odadan Mesaj Geldi:", event.type, event.data);
          // Odadaki oyuncu listesi güncellemeleri burada işlenecek
        };

        const handlePersonalEvent = (event) => {
          console.log("📩 Özel Mesaj:", event.type, event.data);
        };

        const handleError = (error) => {
          console.error("🚨 Sistem Hatası:", error);
        };

        // WebSocket tünelini başlat
        socketService.connect(token, roomId, handleRoomEvent, handlePersonalEvent, handleError);
        setIsInitializing(false);

      } catch (err) {
        console.error("🔴 Oturum açma hatası:", err);
        if (isMounted) {
          setErrorMsg(err.message);
          setIsInitializing(false);
        }
      }
    }

    initSession();

    return () => {
      isMounted = false;
      socketService.disconnect();
    };
  }, []);

  // Süre biterse geçiş yap
  useEffect(() => {
    if (isInitializing) return;
    if (timeLeft > 0 && !isTransitioning) {
      const timerId = setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
      return () => clearTimeout(timerId);
    } else if (timeLeft === 0 && !isTransitioning) {
      setIsTransitioning(true);
      if (onLobbyComplete) onLobbyComplete();
    }
  }, [timeLeft, isTransitioning, onLobbyComplete, isInitializing]);

  // Herkes hazırsa geçiş yap
  useEffect(() => {
    if (isInitializing) return;
    const allReady = players.every(p => p.isReady);
    if (allReady && timeLeft > 0 && !isTransitioning) {
      setIsTransitioning(true);
      if (onLobbyComplete) onLobbyComplete();
    }
  }, [players, timeLeft, isTransitioning, onLobbyComplete, isInitializing]);

  const toggleReady = () => {
    setPlayers(players.map(p => p.isMe ? { ...p, isReady: !p.isReady } : p));
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col relative select-none p-8 animate-fade-in">
      <div className="flex justify-between items-center mb-8 border-b border-slate-800 pb-6">
        <div>
          <h1 className="text-4xl font-black tracking-tighter text-slate-100 drop-shadow-md">DRAFT LOBİSİ</h1>
          <p className="text-slate-400 mt-1 font-bold tracking-widest uppercase text-sm">
            {modeName} <span className="text-slate-600 mx-2">|</span> {difficulty}
          </p>
        </div>
        <div className="flex items-center gap-4">
          {errorMsg && (
            <div className="bg-red-950/80 border border-red-500/50 text-red-300 px-4 py-2 rounded-xl text-xs font-bold shadow-lg animate-pulse">
              ⚠️ {errorMsg}
            </div>
          )}
          <div className="bg-slate-900 border border-slate-700 px-6 py-3 rounded-2xl shadow-lg flex items-center gap-4">
            <span className="text-slate-400 font-bold uppercase text-sm">Oda Kodu:</span>
            <span className="text-2xl font-black tracking-widest text-indigo-400">#X7K9P</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 gap-8">
        <div className="w-1/2 flex flex-col gap-6">
          {players.map((player) => {
            const strokeDashoffset = player.isReady ? 0 : circleCircumference - (timeLeft / 30) * circleCircumference;
            const isDangerZone = !player.isReady && timeLeft <= 3 && timeLeft > 0;

            return (
              <div key={player.id} className={`flex items-center gap-6 p-4 rounded-3xl transition-all duration-300 ${player.isReady ? 'bg-emerald-900/20 border border-emerald-500/30' : 'bg-slate-900 border border-slate-800'}`}>
                <div className="relative w-24 h-24 flex items-center justify-center shrink-0">
                  <svg className="absolute inset-0 w-full h-full transform -rotate-90 drop-shadow-xl">
                    <circle cx="48" cy="48" r={circleRadius} stroke="currentColor" strokeWidth="6" fill="transparent" className="text-slate-800" />
                    <circle cx="48" cy="48" r={circleRadius} stroke="currentColor" strokeWidth="6" fill="transparent" strokeDasharray={circleCircumference} strokeDashoffset={strokeDashoffset} strokeLinecap="round"
                      className={`transition-all duration-1000 ease-linear ${player.isReady ? 'text-emerald-500' : isDangerZone ? 'text-red-500 animate-[pulse_0.5s_ease-in-out_infinite]' : 'text-indigo-500'}`}
                    />
                  </svg>
                  <span className="text-4xl relative z-10">{player.avatar}</span>
                  {player.isReady && (
                    <div className="absolute -bottom-2 -right-2 bg-emerald-500 rounded-full p-1.5 shadow-lg shadow-emerald-500/50 animate-bounce">
                      <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" d="M5 13l4 4L19 7"></path></svg>
                    </div>
                  )}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <h2 className="text-2xl font-bold">{player.name}</h2>
                    {player.isMe && <span className="bg-indigo-600 text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 rounded-full">Sen</span>}
                  </div>
                  <p className={`text-sm font-bold uppercase tracking-wider ${player.isReady ? 'text-emerald-400' : 'text-slate-500'}`}>
                    {player.isReady ? 'Hazır Bekliyor' : 'Seçim Yapılıyor...'}
                  </p>
                </div>
                {player.isMe && (
                  <button onClick={toggleReady} className={`px-8 py-4 rounded-2xl font-black tracking-widest uppercase transition-all shadow-lg hover:scale-105 active:scale-95 ${player.isReady ? 'bg-slate-700 text-white hover:bg-slate-600' : 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-indigo-500/30'}`}>
                    {player.isReady ? 'İptal' : 'Hazırım'}
                  </button>
                )}
              </div>
            );
          })}
        </div>

        <div className="w-1/2 flex flex-col gap-6">
          <div className={`p-8 rounded-3xl border text-center transition-colors duration-500 ${timeLeft <= 3 ? 'bg-red-950/30 border-red-500/50' : 'bg-slate-900 border-slate-800'}`}>
            <p className="text-slate-400 font-bold tracking-widest uppercase text-sm mb-2">Başlamasına Son</p>
            <div className={`text-7xl font-black ${timeLeft <= 3 ? 'text-red-500' : 'text-white'}`}>{timeLeft}</div>
            <p className="text-slate-500 text-xs mt-4">Kimse hazır vermese bile süre dolduğunda oyun otomatik başlar.</p>
          </div>
          <div className="flex-1 bg-slate-900 border border-slate-800 rounded-3xl p-6 flex flex-col relative overflow-hidden">
            <div className="absolute inset-0 opacity-10 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-indigo-500 via-transparent to-transparent pointer-events-none"></div>
            <h3 className="font-bold text-slate-300 uppercase tracking-widest text-sm mb-4 border-b border-slate-800 pb-2 z-10">Oda Sohbeti</h3>
            <div className="flex-1 flex flex-col gap-3 overflow-y-auto z-10">
              <div className="bg-slate-800/80 w-fit px-4 py-2 rounded-2xl rounded-tl-sm text-sm border border-slate-700">
                <span className="font-bold text-emerald-400 mr-2">Sistem:</span>Lobiye bağlandınız, iyi eğlenceler!
              </div>
            </div>
            <div className="mt-4 flex gap-2 z-10">
              <input type="text" placeholder="Gözdağı ver..." className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-indigo-500 transition-colors" />
              <button className="bg-slate-800 p-3 rounded-xl hover:bg-slate-700 transition-colors">
                <svg className="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path></svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}