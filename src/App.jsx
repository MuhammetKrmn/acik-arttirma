import React, { useState } from 'react';
import MainMenu from './components/MainMenu';
import Lobby from './components/Lobby';
import DraftArena from './components/DraftArena';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('menu'); // menu, lobby, arena
  const [selectedLobbyData, setSelectedLobbyData] = useState({ mode: "", difficulty: "" });

  const handlePlayStart = (modeName, difficulty) => {
    setSelectedLobbyData({ mode: modeName, difficulty: difficulty });
    setCurrentScreen('lobby');
  };

  // Lobideki süre bittiğinde bu fonksiyon tetiklenecek
  const handleLobbyComplete = () => {
    setCurrentScreen('arena');
  };

  return (
    <>
      {currentScreen === 'menu' && <MainMenu onPlayStart={handlePlayStart} />}
      {currentScreen === 'lobby' && <Lobby modeName={selectedLobbyData.mode} difficulty={selectedLobbyData.difficulty} onLobbyComplete={handleLobbyComplete} />}
      {currentScreen === 'arena' && <DraftArena />}
    </>
  );
}