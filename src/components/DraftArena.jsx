import { useEffect, useReducer, useState } from 'react';
import PlayerCard from './PlayerCard';
import FitToHeight from './draft/FitToHeight';
import OpponentPanel from './draft/OpponentPanel';
import SquadPanel from './draft/SquadPanel';
import AllSquadsOverlay from './draft/AllSquadsOverlay';
import QuickChat from './draft/QuickChat';
import BottomBar from './draft/BottomBar';
import useCountdown from '../hooks/useCountdown';
import useChatBubbles from '../hooks/useChatBubbles';
import { BOTTOM_BAR_HEIGHT } from '../constants/draft';
import { api } from '../api';
import { socketService } from '../SocketService.js';
import { arenaReducer, initialArenaState } from '../game/arenaReducer';
import { avatarForSeat } from '../game/labels';

// Aynı anda sadece bir panel açık olabilir
const PANEL = {
  CHAT: 'chat',
  ALL_SQUADS: 'allSquads',
  squad: (playerId) => `squad:${playerId}`,
};

// Oyun bitince sonuç ekranına geçmeden önce son kadroları görmek için bekleme
const RESULT_DELAY_MS = 3000;

export default function DraftArena({ session, roomId, onFinished }) {
  const [state, dispatch] = useReducer(arenaReducer, initialArenaState);
  const [openPanel, setOpenPanel] = useState(null);

  // Bağlantı ve olaylar. Lobiden gelindiyse bağlantı zaten açık, sadece dinleyiciler değişir.
  useEffect(() => {
    let alive = true;
    let retryId = null;
    const resync = async () => {
      clearTimeout(retryId);
      try {
        const live = await api.getLive(roomId);
        if (alive) dispatch({ type: 'live', data: live, receivedAt: Date.now() });
      } catch (err) {
        if (!alive) return;
        // Oyun sunucuda henüz kurulmadıysa 1 sn sonra tekrar sor.
        if (err.code === 'GAME_NOT_STARTED') retryId = setTimeout(resync, 1000);
        else dispatch({ type: 'toast', text: err.message });
      }
    };

    socketService.connect(session.playerToken, roomId, {
      onRoomEvent: (event) => dispatch({ type: 'event', event, receivedAt: Date.now() }),
      onPersonalEvent: (event) => dispatch({ type: 'rejected', data: event.data }),
      onError: (e) => dispatch({ type: 'toast', text: e.message }),
      onConnected: resync,
    });

    return () => {
      alive = false;
      clearTimeout(retryId);
      socketService.setHandlers({});
    };
  }, [roomId, session.playerToken]);

  // Oyun bitti: kısa bir bekleyişten sonra sonuç ekranı
  useEffect(() => {
    if (!state.result) return undefined;
    const id = setTimeout(() => onFinished(state.result), RESULT_DELAY_MS);
    return () => clearTimeout(id);
  }, [state.result, onFinished]);

  // Uyarı yazısı 3 sn sonra kaybolur
  useEffect(() => {
    if (!state.toast) return undefined;
    const id = setTimeout(() => dispatch({ type: 'clearToast', id: state.toast.id }), 3000);
    return () => clearTimeout(id);
  }, [state.toast]);

  // Esc ile açık paneli kapat
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && setOpenPanel(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  // Ekran bileşenlerinin beklediği oyuncu biçimi: { id, name, avatar, budget, squad }
  const players = state.players.map((p) => ({
    id: p.roomPlayerId,
    name: p.connected ? p.username : `${p.username} (koptu)`,
    avatar: avatarForSeat(p.seatNumber),
    budget: p.budget,
    squad: state.squads[p.roomPlayerId] ?? {},
  }));
  const myId = state.players.find((p) => p.userId === session.userId)?.roomPlayerId ?? null;

  const { messages, showMessage } = useChatBubbles({ players, myId, incomingMessage: null });

  const cur = state.current;
  const secondsLeft = useCountdown(cur?.status === 'bidding' ? cur.deadlineLocal : null);

  const me = players.find((p) => p.id === myId);
  const [leftOpp, rightOpp] = players.filter((p) => p.id !== myId);

  if (!me || !leftOpp || !rightOpp) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex flex-col items-center justify-center gap-4 animate-fade-in">
        <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-slate-400 font-bold uppercase tracking-widest text-sm">Oyun yükleniyor...</p>
      </div>
    );
  }

  const togglePanel = (panel) => setOpenPanel((c) => (c === panel ? null : panel));
  const closePanel = () => setOpenPanel(null);

  const bidding = cur?.status === 'bidding';
  const currentBid = cur?.price ?? 0;
  const amIWinning = bidding && cur.leaderRoomPlayerId === myId;
  const lockedText = !bidding
    ? null
    : cur.withdrawn.includes(myId)
      ? 'PAS GEÇTİN'
      : me.squad[cur.slot]
        ? 'BU MEVKİ DOLU'
        : null;

  let idleText = null;
  if (state.phase === 'finished') idleText = 'OYUN BİTTİ';
  else if (state.phase === 'tier3') idleText = 'KADROLAR TAMAMLANIYOR';
  else if (!cur) idleText = 'İLK İHALE BAŞLIYOR';
  else if (cur.status === 'sold') idleText = 'İHALE SONUÇLANDI';
  else if (cur.status === 'skipped') idleText = 'KART SATILMADI';

  const nameOf = (roomPlayerId) =>
    roomPlayerId === myId ? 'Sen' : players.find((p) => p.id === roomPlayerId)?.name ?? null;
  const leaderLabel = cur ? nameOf(cur.leaderRoomPlayerId) : null;

  // Teklif ve çekilme: sadece sunucuya gönderilir. Ekran, sunucu bid_accepted / player_withdrew
  // gönderince değişir. Böylece üç ekran da her zaman aynı fiyatı ve lideri gösterir.
  const handleBid = (amount) => {
    if (!bidding || lockedText || amIWinning || me.budget < currentBid + amount) return;
    if (!socketService.sendBid(roomId, currentBid, amount)) {
      dispatch({ type: 'toast', text: 'Bağlantı yok, yeniden bağlanılıyor...' });
    }
  };

  const handlePass = () => {
    if (!bidding || lockedText || amIWinning) return;
    socketService.sendWithdraw(roomId);
  };

  // Hızlı mesajlar backend'e gitmez, sadece senin ekranında balon olarak görünür.
  const sendQuickReply = (text) => {
    showMessage(myId, text);
    closePanel();
  };

  const isDangerZone = bidding && secondsLeft !== null && secondsLeft <= 3;

  return (
    <div className={`min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col relative select-none transition-all duration-500 ${isDangerZone ? 'shadow-[inset_0_0_150px_rgba(239,68,68,0.15)]' : ''}`}>

      {/* 1. MERKEZ SAHNE */}
      <div
        className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none z-10"
        style={{ paddingBottom: BOTTOM_BAR_HEIGHT }}
      >
        <FitToHeight>
          {cur ? (
            <>
              <div className={`mb-2 flex flex-col items-center transition-transform duration-300 ${isDangerZone ? 'scale-125' : 'scale-100'}`}>
                <span className="text-slate-400 font-bold uppercase tracking-widest text-xs mb-1">
                  İhale {cur.sequenceNo}/{state.total} · Kalan Süre
                </span>
                <div className={`text-6xl font-black drop-shadow-lg ${isDangerZone ? 'text-red-500 animate-pulse' : 'text-emerald-400'}`}>
                  {bidding ? secondsLeft ?? '-' : 0}
                </div>
              </div>

              <div className="relative">
                <div className="absolute bg-yellow-500/10 blur-[100px] rounded-full" style={{ inset: '-10%' }}></div>

                <PlayerCard
                  card={cur.card}
                  tier={cur.tier}
                  currentBid={currentBid}
                  leaderLabel={leaderLabel}
                />

                {cur.status !== 'bidding' && (
                  <div className="absolute inset-0 flex items-center justify-center z-50 pointer-events-none">
                    <div style={{ transform: 'rotate(-12deg)' }} className="flex flex-col items-center">
                      <div className={`${cur.status === 'sold' ? 'bg-red-600/90' : 'bg-slate-600/90'} text-white text-6xl font-black uppercase tracking-widest py-4 px-8 border-8 border-white rounded-3xl drop-shadow-2xl backdrop-blur-sm animate-bounce`}>
                        {cur.status === 'sold' ? 'SATILDI' : 'SATILMADI'}
                      </div>
                      {cur.status === 'sold' && (
                        <div className="mt-3 bg-slate-900/90 px-4 py-1 rounded-xl text-lg font-black text-yellow-400">
                          {nameOf(cur.winnerRoomPlayerId)} · {cur.price}M €
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center gap-4">
              <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
              <p className="text-slate-300 font-black uppercase tracking-widest">İlk ihale birazdan başlıyor</p>
            </div>
          )}
        </FitToHeight>
      </div>

      {/* 2. SOL VE SAĞ KANAT */}
      <OpponentPanel
        side="left"
        player={leftOpp}
        isBidding={bidding && cur.leaderRoomPlayerId === leftOpp.id}
        currentBid={currentBid}
        message={messages[leftOpp.id]}
        open={openPanel === PANEL.squad(leftOpp.id)}
        onOpen={() => togglePanel(PANEL.squad(leftOpp.id))}
        onClose={closePanel}
      />
      <OpponentPanel
        side="right"
        player={rightOpp}
        isBidding={bidding && cur.leaderRoomPlayerId === rightOpp.id}
        currentBid={currentBid}
        message={messages[rightOpp.id]}
        open={openPanel === PANEL.squad(rightOpp.id)}
        onOpen={() => togglePanel(PANEL.squad(rightOpp.id))}
        onClose={closePanel}
      />

      {/* 3. KENDİ KADRON */}
      <SquadPanel
        open={openPanel === PANEL.squad(myId)}
        title="Senin Kadron"
        squad={me.squad}
        onClose={closePanel}
      />

      {/* 4. ALT KONTROL PANELİ */}
      <BottomBar
        me={me}
        currentBid={currentBid}
        idleText={idleText}
        lockedText={lockedText}
        amIWinning={amIWinning}
        message={messages[myId]}
        squadOpen={openPanel === PANEL.squad(myId)}
        onToggleSquad={() => togglePanel(PANEL.squad(myId))}
        onBid={handleBid}
        onPass={handlePass}
      />

      {/* 5. BÜTÜN KADROLAR BUTONU */}
      <button
        onClick={() => togglePanel(PANEL.ALL_SQUADS)}
        className="absolute top-6 left-6 z-30 flex items-center gap-2 px-4 py-2 rounded-xl border border-emerald-500/40 bg-slate-900/80 hover:bg-slate-800 hover:border-emerald-400 text-emerald-300 text-xs font-bold tracking-wider uppercase backdrop-blur-sm shadow-md transition-all"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <rect x="3" y="4" width="5" height="16" rx="1" />
          <rect x="9.5" y="4" width="5" height="16" rx="1" />
          <rect x="16" y="4" width="5" height="16" rx="1" />
        </svg>
        Bütün Kadrolar
      </button>

      {/* 6. HIZLI MESAJ BALONU */}
      <QuickChat
        open={openPanel === PANEL.CHAT}
        onToggle={() => togglePanel(PANEL.CHAT)}
        onClose={closePanel}
        onSend={sendQuickReply}
      />

      {/* 7. BÜTÜN KADROLAR EKRANI */}
      <AllSquadsOverlay
        open={openPanel === PANEL.ALL_SQUADS}
        onClose={closePanel}
        myId={myId}
        timeLeft={bidding ? secondsLeft ?? 0 : 0}
        currentBid={currentBid}
        participants={[leftOpp, me, rightOpp]}
      />

      {/* 8. UYARI (teklif reddedildi vb.) */}
      {state.toast && (
        <div
          key={state.toast.id}
          className="absolute left-1/2 -translate-x-1/2 z-50 bg-red-950/90 border border-red-500/60 text-red-100 px-6 py-3 rounded-2xl font-bold shadow-xl animate-fade-in"
          style={{ bottom: BOTTOM_BAR_HEIGHT + 20 }}
        >
          ⚠️ {state.toast.text}
        </div>
      )}
    </div>
  );
}