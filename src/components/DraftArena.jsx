import { useEffect, useState } from 'react';
import PlayerCard from './PlayerCard';
import FitToHeight from './draft/FitToHeight';
import OpponentPanel from './draft/OpponentPanel';
import SquadPanel from './draft/SquadPanel';
import AllSquadsOverlay from './draft/AllSquadsOverlay';
import QuickChat from './draft/QuickChat';
import BottomBar from './draft/BottomBar';
import useAuctionTimer from '../hooks/useAuctionTimer';
import useChatBubbles from '../hooks/useChatBubbles';
import { BOTTOM_BAR_HEIGHT } from '../constants/draft';
import { DEFAULT_PLAYERS, MOCK_PLAYER } from '../constants/mockData';

// Aynı anda sadece bir panel açık olabilir: açılan panelin adı tek state'te tutulur.
const PANEL = {
  CHAT: 'chat',
  ALL_SQUADS: 'allSquads',
  squad: (playerId) => `squad:${playerId}`,
};

export default function DraftArena({
  players = DEFAULT_PLAYERS, // [{ id, name, avatar, budget, squad }]
  myId = 'me',               // bu cihazdaki oyuncunun id'si
  incomingMessage = null,    // sunucudan gelen mesaj: { playerId, text }
  onChatSend,                // ben mesaj gönderdim: (text) => ...
  onBid,                     // ben teklif verdim: (amount) => ...
  onPass,                    // ben çekildim: () => ...
}) {
  const { timeLeft, auctionStatus, resetTimer } = useAuctionTimer();
  const { messages, showMessage } = useChatBubbles({ players, myId, incomingMessage });

  const [currentBid, setCurrentBid] = useState(10);
  const [highestBidder, setHighestBidder] = useState(null); // oyuncu id'si
  const [isPassed, setIsPassed] = useState(false);
  const [openPanel, setOpenPanel] = useState(null);

  // Esc ile açık paneli kapat
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && setOpenPanel(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  // Bu cihazdaki oyuncu ve diğer ikisi
  const me = players.find((p) => p.id === myId);
  const [leftOpp, rightOpp] = players.filter((p) => p.id !== myId);

  // Oyuncu listesi eksikse (örn. sunucudan henüz gelmediyse) hiçbir şey çizme
  if (!me || !leftOpp || !rightOpp) return null;

  const togglePanel = (panel) => setOpenPanel((cur) => (cur === panel ? null : panel));
  const closePanel = () => setOpenPanel(null);

  const handleBid = (amount) => {
    if (isPassed || highestBidder === myId || me.budget < currentBid + amount) return;

    setCurrentBid((prev) => prev + amount);
    setHighestBidder(myId);
    resetTimer();
    if (onBid) onBid(amount);
  };

  const handlePass = () => {
    setIsPassed(true);
    setHighestBidder(null);
    if (onPass) onPass();
  };

  const sendQuickReply = (text) => {
    showMessage(myId, text);
    if (onChatSend) onChatSend(text);
    closePanel();
  };

  const isDangerZone = timeLeft <= 3 && auctionStatus === 'bidding';
  const highestBidderLabel = highestBidder
    ? (highestBidder === myId ? 'Sen' : players.find((p) => p.id === highestBidder)?.name ?? null)
    : null;

  return (
    <div className={`min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col relative select-none transition-all duration-500 ${isDangerZone ? 'shadow-[inset_0_0_150px_rgba(239,68,68,0.15)]' : ''}`}>

      {/* 1. MERKEZ SAHNE (içerik taşarsa otomatik küçülür) */}
      <div
        className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none z-10"
        style={{ paddingBottom: BOTTOM_BAR_HEIGHT }}
      >
        <FitToHeight>
          <div className={`mb-2 flex flex-col items-center transition-transform duration-300 ${isDangerZone ? 'scale-125' : 'scale-100'}`}>
            <span className="text-slate-400 font-bold uppercase tracking-widest text-xs mb-1">Kalan Süre</span>
            <div className={`text-6xl font-black drop-shadow-lg ${isDangerZone ? 'text-red-500 animate-pulse' : 'text-emerald-400'}`}>
              {timeLeft}
            </div>
          </div>

          <div className="relative">
            <div className="absolute bg-yellow-500/10 blur-[100px] rounded-full" style={{ inset: '-10%' }}></div>

            <PlayerCard
              player={MOCK_PLAYER}
              currentBid={currentBid}
              highestBidder={highestBidderLabel}
            />

            {auctionStatus === 'sold' && (
              <div className="absolute inset-0 flex items-center justify-center z-50 pointer-events-none">
                <div style={{ transform: 'rotate(-12deg)' }}>
                  <div className="bg-red-600/90 text-white text-6xl font-black uppercase tracking-widest py-4 px-8 border-8 border-white rounded-3xl drop-shadow-2xl backdrop-blur-sm animate-bounce">
                    SATILDI
                  </div>
                </div>
              </div>
            )}
          </div>
        </FitToHeight>
      </div>

      {/* 2. SOL VE SAĞ KANAT (rakipler) */}
      <OpponentPanel
        side="left"
        player={leftOpp}
        isBidding={highestBidder === leftOpp.id}
        currentBid={currentBid}
        message={messages[leftOpp.id]}
        open={openPanel === PANEL.squad(leftOpp.id)}
        onOpen={() => togglePanel(PANEL.squad(leftOpp.id))}
        onClose={closePanel}
      />
      <OpponentPanel
        side="right"
        player={rightOpp}
        isBidding={highestBidder === rightOpp.id}
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
        auctionStatus={auctionStatus}
        isPassed={isPassed}
        amIWinning={highestBidder === myId}
        message={messages[myId]}
        squadOpen={openPanel === PANEL.squad(myId)}
        onToggleSquad={() => togglePanel(PANEL.squad(myId))}
        onBid={handleBid}
        onPass={handlePass}
      />

      {/* 5. BÜTÜN KADROLAR BUTONU (sol üst) */}
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

      {/* 6. HIZLI MESAJ BALONU (sağ üst) */}
      <QuickChat
        open={openPanel === PANEL.CHAT}
        onToggle={() => togglePanel(PANEL.CHAT)}
        onClose={closePanel}
        onSend={sendQuickReply}
      />

      {/* 7. BÜTÜN KADROLAR EKRANI (en üstte) */}
      <AllSquadsOverlay
        open={openPanel === PANEL.ALL_SQUADS}
        onClose={closePanel}
        myId={myId}
        timeLeft={timeLeft}
        currentBid={currentBid}
        participants={[leftOpp, me, rightOpp]}
      />
    </div>
  );
}