import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api';
import { socketService } from '../SocketService.js';
import useCountdown from '../hooks/useCountdown';
import { avatarForSeat } from '../game/labels';

const ROOM_SIZE = 3;
const COUNTDOWN_S = 30; // sunucudaki lobby.countdown-ms ile aynı (halka animasyonu için)

function fromRoom(room) {
  return {
    roomId: room.roomId,
    status: room.status,
    conceptName: room.conceptName,
    players: room.players.map((p) => ({ ...p, ready: false })),
    countdownDeadlineMs: null,
  };
}

// Lobi. Oyuncu listesi, hazır durumları ve sayaç sunucudan gelir (lobby_updated).
// Oyunu sunucu başlatır: herkes hazır olunca ya da sayaç bitince game_started gelir.
export default function Lobby({ session, room, onGameStarted }) {
  const roomId = room.roomId;
  const [lobby, setLobby] = useState(() => fromRoom(room));
  const [deadlineLocal, setDeadlineLocal] = useState(null);
  const [log, setLog] = useState([{ id: 'hello', text: 'Lobiye bağlandın. Oyuncular bekleniyor.' }]);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);

  const lobbyRef = useRef(lobby);
  const startedRef = useRef(false);
  const onStartRef = useRef(onGameStarted);
  useEffect(() => {
    onStartRef.current = onGameStarted;
  }, [onGameStarted]);

  const secondsLeft = useCountdown(deadlineLocal);

  const startGame = useCallback(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    onStartRef.current?.();
  }, []);

  // Gelen lobi durumunu ekrana uygula, değişiklikleri sistem mesajı olarak yaz.
  const applyLobby = useCallback((view, receivedAt) => {
    const prev = lobbyRef.current;
    const notes = [];
    for (const p of view.players) {
      const old = prev.players.find((x) => x.userId === p.userId);
      if (!old) notes.push(`${p.username} lobiye katıldı`);
      else if (old.ready !== p.ready) notes.push(p.ready ? `${p.username} hazır` : `${p.username} hazırlığını iptal etti`);
    }
    lobbyRef.current = view;
    setLobby(view);
    setDeadlineLocal(view.countdownDeadlineMs ? receivedAt + (view.countdownDeadlineMs - view.serverNowMs) : null);
    if (notes.length) {
      setLog((l) => [...l, ...notes.map((text, i) => ({ id: `${receivedAt}-${i}`, text }))].slice(-30));
    }
    if (view.status !== 'LOBBY' && view.status !== 'SETUP') startGame();
  }, [startGame]);

  useEffect(() => {
    let alive = true;
    const refresh = async () => {
      try {
        const view = await api.getLobby(roomId);
        if (alive) applyLobby(view, Date.now());
      } catch (err) {
        if (alive) setError(err.message);
      }
    };

    socketService.connect(session.playerToken, roomId, {
      onRoomEvent: (event) => {
        if (event.type === 'lobby_updated') applyLobby(event.data, Date.now());
        else if (event.type === 'game_started') startGame();
      },
      onError: (e) => setError(e.message),
      onConnected: refresh,
    });

    return () => {
      alive = false;
      socketService.setHandlers({});
    };
  }, [roomId, session.playerToken, applyLobby, startGame]);

  const me = lobby.players.find((p) => p.userId === session.userId);

  const toggleReady = async () => {
    if (!me || pending) return;
    setPending(true);
    setError(null);
    try {
      const view = await api.setReady(roomId, session.playerToken, !me.ready);
      applyLobby(view, Date.now());
    } catch (err) {
      if (err.code !== 'GAME_ALREADY_STARTED') setError(err.message);
    } finally {
      setPending(false);
    }
  };

  const circleRadius = 40;
  const circleCircumference = 2 * Math.PI * circleRadius;
  const full = lobby.players.length >= ROOM_SIZE;
  const danger = secondsLeft !== null && secondsLeft <= 3;
  const seats = Array.from({ length: ROOM_SIZE }, (_, i) => lobby.players.find((p) => p.seatNumber === i + 1) ?? null);

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col relative select-none p-8 animate-fade-in">
      <div className="flex justify-between items-center mb-8 border-b border-slate-800 pb-6">
        <div>
          <h1 className="text-4xl font-black tracking-tighter text-slate-100 drop-shadow-md">DRAFT LOBİSİ</h1>
          <p className="text-slate-400 mt-1 font-bold tracking-widest uppercase text-sm">
            {lobby.conceptName} <span className="text-slate-600 mx-2">|</span> 1000M € bütçe
          </p>
        </div>
        <div className="flex items-center gap-4">
          {error && (
            <div className="bg-red-950/80 border border-red-500/50 text-red-300 px-4 py-2 rounded-xl text-xs font-bold shadow-lg">
              ⚠️ {error}
            </div>
          )}
          <div className="bg-slate-900 border border-slate-700 px-6 py-3 rounded-2xl shadow-lg flex items-center gap-4">
            <span className="text-slate-400 font-bold uppercase text-sm">Oda Kodu:</span>
            <span className="text-2xl font-black tracking-widest text-indigo-400">#{roomId}</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 gap-8">
        <div className="w-1/2 flex flex-col gap-6">
          {seats.map((player, i) => {
            if (!player) {
              return (
                <div key={`empty-${i}`} className="flex items-center gap-6 p-4 rounded-3xl bg-slate-900/40 border border-dashed border-slate-700">
                  <div className="w-24 h-24 rounded-full border-4 border-dashed border-slate-700 flex items-center justify-center text-3xl text-slate-600 animate-pulse">?</div>
                  <p className="text-slate-500 font-bold uppercase tracking-wider text-sm">Oyuncu bekleniyor...</p>
                </div>
              );
            }
            const isMe = player.userId === session.userId;
            const ratio = secondsLeft === null ? 1 : secondsLeft / COUNTDOWN_S;
            const strokeDashoffset = player.ready ? 0 : circleCircumference - ratio * circleCircumference;
            const isDangerZone = !player.ready && danger && secondsLeft > 0;

            return (
              <div key={player.userId} className={`flex items-center gap-6 p-4 rounded-3xl transition-all duration-300 ${player.ready ? 'bg-emerald-900/20 border border-emerald-500/30' : 'bg-slate-900 border border-slate-800'}`}>
                <div className="relative w-24 h-24 flex items-center justify-center shrink-0">
                  <svg className="absolute inset-0 w-full h-full transform -rotate-90 drop-shadow-xl">
                    <circle cx="48" cy="48" r={circleRadius} stroke="currentColor" strokeWidth="6" fill="transparent" className="text-slate-800" />
                    <circle cx="48" cy="48" r={circleRadius} stroke="currentColor" strokeWidth="6" fill="transparent" strokeDasharray={circleCircumference} strokeDashoffset={strokeDashoffset} strokeLinecap="round"
                      className={`transition-all duration-1000 ease-linear ${player.ready ? 'text-emerald-500' : isDangerZone ? 'text-red-500 animate-[pulse_0.5s_ease-in-out_infinite]' : 'text-indigo-500'}`}
                    />
                  </svg>
                  <span className="text-4xl relative z-10">{avatarForSeat(player.seatNumber)}</span>
                  {player.ready && (
                    <div className="absolute -bottom-2 -right-2 bg-emerald-500 rounded-full p-1.5 shadow-lg shadow-emerald-500/50 animate-bounce">
                      <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" d="M5 13l4 4L19 7"></path></svg>
                    </div>
                  )}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <h2 className="text-2xl font-bold">{player.username}</h2>
                    {isMe && <span className="bg-indigo-600 text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 rounded-full">Sen</span>}
                  </div>
                  <p className={`text-sm font-bold uppercase tracking-wider ${player.ready ? 'text-emerald-400' : 'text-slate-500'}`}>
                    {player.ready ? 'Hazır Bekliyor' : 'Hazırlanıyor...'}
                  </p>
                </div>
                {isMe && (
                  <button
                    onClick={toggleReady}
                    disabled={pending}
                    className={`px-8 py-4 rounded-2xl font-black tracking-widest uppercase transition-all shadow-lg hover:scale-105 active:scale-95 disabled:opacity-60 ${player.ready ? 'bg-slate-700 text-white hover:bg-slate-600' : 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-indigo-500/30'}`}
                  >
                    {player.ready ? 'İptal' : 'Hazırım'}
                  </button>
                )}
              </div>
            );
          })}
        </div>

        <div className="w-1/2 flex flex-col gap-6">
          <div className={`p-8 rounded-3xl border text-center transition-colors duration-500 ${danger ? 'bg-red-950/30 border-red-500/50' : 'bg-slate-900 border-slate-800'}`}>
            {full && secondsLeft !== null ? (
              <>
                <p className="text-slate-400 font-bold tracking-widest uppercase text-sm mb-2">Başlamasına Son</p>
                <div className={`text-7xl font-black ${danger ? 'text-red-500' : 'text-white'}`}>{secondsLeft}</div>
                <p className="text-slate-500 text-xs mt-4">Herkes hazır olunca hemen başlar. Kimse hazır demese de süre dolunca otomatik başlar.</p>
              </>
            ) : (
              <>
                <p className="text-slate-400 font-bold tracking-widest uppercase text-sm mb-2">Oyuncular Bekleniyor</p>
                <div className="text-7xl font-black text-white">{lobby.players.length}/{ROOM_SIZE}</div>
                <p className="text-slate-500 text-xs mt-4">Oda dolunca {COUNTDOWN_S} saniyelik geri sayım başlar.</p>
              </>
            )}
          </div>
          <div className="flex-1 bg-slate-900 border border-slate-800 rounded-3xl p-6 flex flex-col relative overflow-hidden">
            <div className="absolute inset-0 opacity-10 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-indigo-500 via-transparent to-transparent pointer-events-none"></div>
            <h3 className="font-bold text-slate-300 uppercase tracking-widest text-sm mb-4 border-b border-slate-800 pb-2 z-10">Oda Olayları</h3>
            <div className="flex-1 flex flex-col gap-3 overflow-y-auto z-10">
              {log.map((m) => (
                <div key={m.id} className="bg-slate-800/80 w-fit px-4 py-2 rounded-2xl rounded-tl-sm text-sm border border-slate-700">
                  <span className="font-bold text-emerald-400 mr-2">Sistem:</span>{m.text}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}