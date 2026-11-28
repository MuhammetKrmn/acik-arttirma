import { useCallback, useEffect, useState } from 'react';
import LoginScreen from './components/LoginScreen';
import MainMenu from './components/MainMenu';
import Lobby from './components/Lobby';
import DraftArena from './components/DraftArena';
import ResultScreen from './components/ResultScreen';
import { api } from './api';
import { socketService } from './SocketService.js';
import { loadActiveRoomId, loadSession, saveActiveRoomId, saveSession } from './session';

// Ekranlar: login -> menu -> lobby -> arena -> result
// Sayfa yenilenirse devam eden oyuna (veya lobiye) geri dönülür.
export default function App() {
  const [session, setSession] = useState(loadSession);
  const [screen, setScreen] = useState(() => (loadSession() ? (loadActiveRoomId() ? 'resume' : 'menu') : 'login'));
  const [room, setRoom] = useState(null); // RoomView (lobi için) veya { roomId }
  const [result, setResult] = useState(null);

  const goMenu = useCallback(() => {
    socketService.disconnect();
    saveActiveRoomId(null);
    setRoom(null);
    setResult(null);
    setScreen('menu');
  }, []);

  // Yenilemeden sonra: kayıtlı oda hâlâ sürüyor mu?
  useEffect(() => {
    if (screen !== 'resume' || !session) return undefined;
    let alive = true;
    const roomId = loadActiveRoomId();

    (async () => {
      try {
        const lobby = await api.getLobby(roomId);
        const mine = lobby.players.some((p) => p.userId === session.userId);
        if (!alive) return;
        if (!mine) {
          goMenu();
        } else if (lobby.status === 'LOBBY' || lobby.status === 'SETUP') {
          setRoom({ roomId, status: lobby.status, conceptName: lobby.conceptName, players: lobby.players });
          setScreen('lobby');
        } else if (lobby.status === 'FINISHED') {
          const res = await api.getResult(roomId);
          if (!alive) return;
          setResult(res);
          setScreen('result');
        } else {
          setRoom({ roomId });
          setScreen('arena');
        }
      } catch {
        if (alive) goMenu();
      }
    })();

    return () => {
      alive = false;
    };
  }, [screen, session, goMenu]);

  const handleLoggedIn = (s) => {
    saveSession(s);
    setSession(s);
    setScreen('menu');
  };

  const handleLogout = useCallback(() => {
    socketService.disconnect();
    saveSession(null);
    saveActiveRoomId(null);
    setSession(null);
    setRoom(null);
    setScreen('login');
  }, []);

  const handleJoined = (roomView) => {
    saveActiveRoomId(roomView.roomId);
    setRoom(roomView);
    setScreen('lobby');
  };

  const handleGameStarted = useCallback(() => setScreen('arena'), []);

  const handleFinished = useCallback((res) => {
    setResult(res);
    setScreen('result');
  }, []);

  if (!session || screen === 'login') return <LoginScreen onLoggedIn={handleLoggedIn} />;

  if (screen === 'resume') {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-400 flex items-center justify-center font-bold uppercase tracking-widest text-sm">
        Yükleniyor...
      </div>
    );
  }

  if (screen === 'lobby' && room) {
    return <Lobby session={session} room={room} onGameStarted={handleGameStarted} />;
  }
  if (screen === 'arena' && room) {
    return <DraftArena session={session} roomId={room.roomId} onFinished={handleFinished} />;
  }
  if (screen === 'result' && result) {
    return <ResultScreen result={result} session={session} onBackToMenu={goMenu} />;
  }
  return <MainMenu session={session} onJoined={handleJoined} onLogout={handleLogout} />;
}