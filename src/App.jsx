import React, { useState } from 'react';
import GuestLogin from './components/GuestLogin';
import MainMenu from './components/MainMenu';
import Lobby from './components/Lobby';
import DraftArena from './components/DraftArena';

export default function App() {
  const [screen, setScreen] = useState(() => localStorage.getItem('playerToken') ? 'menu' : 'login');
  const [gameConfig, setGameConfig] = useState({ modeName: 'Şampiyonlar Ligi', difficulty: 'Orta Zorluk' });
  const [activeRoomId, setActiveRoomId] = useState(null);

  const handleLoginSuccess = () => setScreen('menu');

  const handleStartGame = (mode, diff) => {
    setGameConfig({ modeName: mode || 'Şampiyonlar Ligi', difficulty: diff || 'Orta Zorluk' });
    setScreen('lobby');
  };

  const handleLobbyComplete = (roomId) => {
    setActiveRoomId(roomId);
    setScreen('arena');
  };

  return (
    <div>
      {screen === 'login' && <GuestLogin onLoginSuccess={handleLoginSuccess} />}
      {screen === 'menu' && <MainMenu onStartGame={handleStartGame} />}
      {screen === 'lobby' && (
        <Lobby 
          modeName={gameConfig.modeName} 
          difficulty={gameConfig.difficulty} 
          onLobbyComplete={handleLobbyComplete} 
        />
      )}
      {screen === 'arena' && (
        <DraftArena 
          roomId={activeRoomId} 
          onExit={() => setScreen('menu')} 
        />
      )}
    </div>
  );
}