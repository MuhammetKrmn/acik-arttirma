import React, { useState, useEffect } from 'react';
import { socketService } from '../SocketService.js';

export default function Lobby({ modeName = "Rastgele Havuz", difficulty = "Standart", onLobbyComplete }) {
  const [players, setPlayers] = useState([]);
  const [roomCode, setRoomCode] = useState("Bağlanıyor...");
  const [roomId, setRoomId] = useState(null);
  const [timeLeft, setTimeLeft] = useState(null); // Oda dolana kadar sayaç durur (null)

  const circleRadius = 40;
  const circleCircumference = 2 * Math.PI * circleRadius;
  
  useEffect(() => {
    let isMounted = true;

    async function initSession() {
      try {
        const token = localStorage.getItem('playerToken');
        const myUsername = localStorage.getItem('playerUsername');
        if (!token) return;

        let roomsRes = await fetch('http://localhost:8080/api/rooms');
        let rooms = await roomsRes.json();
        let targetRoomId = null;

        if (rooms && rooms.length > 0) {
          const lastRoom = rooms[rooms.length - 1];
          targetRoomId = lastRoom.roomId.toString();
        } else {
          let createRes = await fetch('http://localhost:8080/api/rooms', {
            method: 'POST',
            headers: { 'X-Player-Token': token }
          });
          let createData = await createRes.json();
          targetRoomId = createData.roomId.toString();
        }

        setRoomId(targetRoomId);

        // Odaya Katıl
        await fetch(`http://localhost:8080/api/rooms/${targetRoomId}/join`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Player-Token': token }
        });

        const syncRoomState = async () => {
          let detailRes = await fetch(`http://localhost:8080/api/rooms/${targetRoomId}`);
          if (detailRes.ok) {
            let roomView = await detailRes.json();
            if (!isMounted) return;
            setRoomCode(roomView.roomCode || `#${targetRoomId}`);

            if (roomView.players) {
              let mapped = roomView.players.map((p, index) => ({
                id: p.roomPlayerId || p.userId || index,
                name: p.username,
                isMe: p.username === myUsername,
                isReady: p.ready || false,
                avatar: p.username === myUsername ? "👑" : "👤"
              }));
              setPlayers(mapped);

              // KRİTİK KURAL: Oda dolduğunda (örn: 3 oyuncu olduğunda) sayaç otomatik başlar!
              if (mapped.length >= 3) {
                setTimeLeft(prev => (prev === null ? 20 : prev));
              } else {
                setTimeLeft(null); // Oda dolana kadar sayaç başlamaz, "Oyuncu Bekleniyor" yazar
              }
            }
          }
        };

        await syncRoomState();
        const pollInterval = setInterval(syncRoomState, 1000);

        // WebSocket ile oyunun başladığını yakala
        socketService.connect(token, targetRoomId, (event) => {
          if (event.type === 'game_started') {
            if (onLobbyComplete) onLobbyComplete(targetRoomId);
          }
        }, null, console.error);

        return () => clearInterval(pollInterval);
      } catch (err) {
        console.error(err);
      }
    }

    initSession();

    return () => {
      isMounted = false;
      socketService.disconnect();
    };
  }, []);

  // Sadece oda dolduğunda (timeLeft bir sayı olduğunda) ortak sayaç geri sayar
  useEffect(() => {
    if (timeLeft === null || !roomId) return;

    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          fetch(`http://localhost:8080/api/rooms/${roomId}/start`, {
            method: 'POST',
            headers: { 'X-Player-Token': localStorage.getItem('playerToken') }
          }).then(res => {
            if (res.ok && onLobbyComplete) {
              onLobbyComplete(roomId);
            }
          }).catch(() => {});
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [timeLeft, roomId, onLobbyComplete]);

  // Hazırım Butonu İşlevi (Backend'e hazır sinyali gönderir)
  const toggleReady = async () => {
    const token = localStorage.getItem('playerToken');
    if (!roomId || !token) return;

    const me = players.find(p => p.isMe);
    const newReadyState = !me?.isReady;

    try {
      // Backend'e hazır durumunu bildir
      await fetch(`http://localhost:8080/api/rooms/${roomId}/ready`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Player-Token': token },
        body: JSON.stringify({ ready: newReadyState })
      });
    } catch (e) {
      console.error("Hazır durumu gönderilemedi:", e);
    }

    setPlayers(players.map(p => p.isMe ? { ...p, isReady: newReadyState } : p));
  };

  const displayTime = timeLeft !== null ? timeLeft : "Bekleniyor...";
  const strokeDashoffset = timeLeft !== null ? circleCircumference - (timeLeft / 20) * circleCircumference : circleCircumference;

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
          <div className="bg-slate-900 border border-slate-700 px-6 py-3 rounded-2xl shadow-lg flex items-center gap-4">
            <span className="text-slate-400 font-bold uppercase text-sm">Oda Kodu:</span>
            <span className="text-2xl font-black tracking-widest text-indigo-400">{roomCode}</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 gap-8">
        <div className="w-1/2 flex flex-col gap-6">
          {players.map((player) => {
            return (
              <div key={player.id} className={`flex items-center justify-between gap-4 p-4 rounded-3xl transition-all duration-300 ${player.isReady ? 'bg-emerald-900/20 border border-emerald-500/30' : 'bg-slate-900 border border-slate-800'}`}>
                <div className="flex items-center gap-4 flex-1 min-w-0">
                  <div className="relative w-20 h-20 flex items-center justify-center shrink-0">
                    <span className="text-3xl relative z-10">{player.avatar}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <h2 className="text-xl font-bold truncate">{player.name}</h2>
                      {player.isMe && <span className="bg-indigo-600 text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 rounded-full shrink-0">Sen</span>}
                    </div>
                    <p className={`text-xs font-bold uppercase tracking-wider ${player.isReady ? 'text-emerald-400' : 'text-slate-500'}`}>
                      {player.isReady ? 'Hazır Bekliyor' : 'Seçim Yapılıyor...'}
                    </p>
                  </div>
                </div>
                {player.isMe && (
                  <button onClick={toggleReady} className={`shrink-0 px-6 py-3 rounded-2xl font-black text-sm tracking-widest uppercase transition-all shadow-lg hover:scale-105 active:scale-95 ${player.isReady ? 'bg-slate-700 text-white hover:bg-slate-600' : 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-indigo-500/30'}`}>
                    {player.isReady ? 'İptal' : 'Hazırım'}
                  </button>
                )}
              </div>
            );
          })}
        </div>

        <div className="w-1/2 flex flex-col gap-6">
          <div className="p-8 rounded-3xl border text-center transition-colors duration-500 bg-slate-900 border-slate-800">
            <p className="text-slate-400 font-bold tracking-widest uppercase text-sm mb-2">
              {timeLeft !== null ? "Başlamasına Son" : "Oyuncu Bekleniyor"}
            </p>
            <div className={`font-black text-white ${timeLeft !== null ? 'text-7xl' : 'text-3xl text-amber-400 animate-pulse'}`}>
              {displayTime}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}