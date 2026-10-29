import { useEffect, useState, useRef } from 'react';
import PlayerCard from './PlayerCard';
import FitToHeight from './draft/FitToHeight';
import OpponentPanel from './draft/OpponentPanel';
import SquadPanel from './draft/SquadPanel';
import AllSquadsOverlay from './draft/AllSquadsOverlay';
import QuickChat from './draft/QuickChat';
import BottomBar from './draft/BottomBar';
import useChatBubbles from '../hooks/useChatBubbles';
import { BOTTOM_BAR_HEIGHT } from '../constants/draft';
import { socketService } from '../SocketService.js';

const PANEL = {
  CHAT: 'chat',
  ALL_SQUADS: 'allSquads',
  squad: (playerId) => `squad:${playerId}`,
};

export default function DraftArena({
  roomId,
  incomingMessage = null,
  onChatSend,
  onBid,
  onPass,
}) {
  const [players, setPlayers] = useState([
    { id: 1, name: "Bağlanıyor...", budget: 1000, squad: [], connected: true }
  ]);
  const [myId, setMyId] = useState(null);
  const [currentAuction, setCurrentAuction] = useState(null);

  // Yerel sayaç YOK. Sadece backend'den gelen canlı süre ekrana yazılır.
  const [timeLeft, setTimeLeft] = useState(15);
  const [auctionStatus, setAuctionStatus] = useState('bidding');

  const { messages, showMessage } = useChatBubbles({ players, myId, incomingMessage });

  const [currentBid, setCurrentBid] = useState(10);
  const [highestBidder, setHighestBidder] = useState(null);
  const [isPassed, setIsPassed] = useState(false);
  const [openPanel, setOpenPanel] = useState(null);

  const currentBidRef = useRef(10);

  // CANLI OYUN DÖNGÜSÜ: Süre, Fiyat, Bütçe, Kadro %100 Backend'den beslenir
  useEffect(() => {
    let isMounted = true;

    async function fetchLiveGameState() {
      if (!roomId) return; 
      
      try {
        let token = localStorage.getItem('playerToken');
        if (!token) return;

        const liveRes = await fetch(`http://localhost:8080/api/rooms/${roomId}/live`);
        if (liveRes.ok) {
          const state = await liveRes.json();
          if (!isMounted) return;

          if (state && state.players && state.players.length > 0) {
            const mappedPlayers = state.players.map(p => ({
              id: p.roomPlayerId || p.userId,
              name: p.username,
              budget: p.budget,
              squad: p.filledSlots || p.squad || [],
              connected: p.connected
            }));
            setPlayers(mappedPlayers);
            
            const myUsername = localStorage.getItem('playerUsername');
            const meObj = mappedPlayers.find(p => p.name === myUsername);
            if (meObj) {
              setMyId(meObj.id);
            } else if (mappedPlayers.length > 0) {
              setMyId(mappedPlayers[0].id);
            }

            if (state.status) {
              setAuctionStatus(state.status);
            }

            // SÜRE DOĞRUDAN BACKEND'DEN (Tüm sekmelerde milisaniyesine aynı olur)
            if (state.timeLeft !== undefined && state.timeLeft !== null) {
              setTimeLeft(state.timeLeft);
            }

            // Fiyat ve Lider Bilgisi
            if (state.current) {
              const cardData = state.current.card || state.current.footballer || state.current.player || state.current;
              setCurrentAuction(cardData);
              
              const dbPrice = state.current.price || state.current.startingPrice || state.current.currentPrice || 10;
              if (dbPrice >= currentBidRef.current) {
                currentBidRef.current = dbPrice;
                setCurrentBid(dbPrice);
                setHighestBidder(state.current.leaderRoomPlayerId);
              }
            }
          }
        }

        const handleRoomEvent = (event) => {
          if (event.type === 'auction_started') {
            const cardData = event.data.card || event.data.footballer || event.data.player || event.data;
            setCurrentAuction(cardData);
            
            const startPrice = event.data.price || event.data.startingPrice || 10;
            currentBidRef.current = startPrice;
            setCurrentBid(startPrice);
            
            setHighestBidder(null);
            setIsPassed(false);
            setAuctionStatus('bidding');
          } 
          else if (event.type === 'bid_accepted') {
            const newPrice = event.data.price;
            if (newPrice >= currentBidRef.current) {
              currentBidRef.current = newPrice;
              setCurrentBid(newPrice);
              setHighestBidder(event.data.leaderRoomPlayerId);
            }
          } 
          else if (event.type === 'auction_closed') {
            setAuctionStatus('sold');
          }
        };

        const handlePersonalEvent = (event) => {
          if (event.type === 'bid_rejected') {
            console.warn("Teklif reddedildi:", event.data.reason);
          }
        };

        socketService.connect(token, roomId, handleRoomEvent, handlePersonalEvent, console.error);

      } catch (err) {
        console.error("Canlı oyun verisi çekilemedi:", err);
      }
    }

    fetchLiveGameState();
    // 300 milisaniyede bir backend'i yokla -> Süreler ve fiyatlar cam gibi senkronize olur
    const interval = setInterval(fetchLiveGameState, 300);

    return () => {
      isMounted = false;
      clearInterval(interval);
      socketService.disconnect();
    };
  }, [roomId]);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && setOpenPanel(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const me = players.find((p) => String(p.id) === String(myId)) || players[0];
  const opponents = players.filter((p) => String(p.id) !== String(me.id));
  const leftOpp = opponents[0] || null;
  const rightOpp = opponents[1] || null;

  const togglePanel = (panel) => setOpenPanel((cur) => (cur === panel ? null : panel));
  const closePanel = () => setOpenPanel(null);

  const handleBid = (amount) => {
    if (isPassed || String(highestBidder) === String(me.id) || me.budget < currentBidRef.current + amount) return;
    
    const newBid = currentBidRef.current + amount;

    currentBidRef.current = newBid;
    setCurrentBid(newBid);
    setHighestBidder(me.id);

    try {
      if (socketService.client && socketService.client.connected) {
        socketService.client.publish({
          destination: `/app/rooms/${roomId}/bid`,
          body: JSON.stringify({ amount: newBid, expectedPrice: newBid, price: newBid })
        });
      }
    } catch (e) {}

    if (onBid) onBid(amount);
  };

  const handlePass = () => {
    setIsPassed(true);
    try {
      if (socketService.client && socketService.client.connected) {
        socketService.client.publish({
          destination: `/app/rooms/${roomId}/withdraw`,
          body: JSON.stringify({})
        });
      }
    } catch (e) {}

    if (onPass) onPass();
  };

  const sendQuickReply = (text) => {
    showMessage(me.id, text);
    if (onChatSend) onChatSend(text);
    closePanel();
  };

  const isDangerZone = timeLeft <= 3 && auctionStatus === 'bidding';
  
  const amIWinning = highestBidder !== null && String(highestBidder) === String(me.id);
  const highestBidderLabel = highestBidder
    ? (amIWinning ? 'Sen' : players.find((p) => String(p.id) === String(highestBidder))?.name ?? "Bilinmiyor")
    : null;

  const activeCardPlayer = currentAuction ? {
    playerName: currentAuction.playerName || currentAuction.name || currentAuction.footballerName || currentAuction.title || "Futbolcu",
    position: currentAuction.position || currentAuction.pos || "---",
    nationality: currentAuction.nationality || currentAuction.nation || currentAuction.country || "--",
    club: currentAuction.club || currentAuction.team || currentAuction.squad || "---",
    overallRating: currentAuction.overallRating || currentAuction.rating || currentAuction.overall || currentAuction.score || 85,
    photoUrl: currentAuction.photoUrl || currentAuction.image || currentAuction.url || currentAuction.picture || null
  } : {
    playerName: "Kart Bekleniyor...",
    position: "---",
    nationality: "--",
    club: "---",
    overallRating: 0
  };

  return (
    <div className={`min-h-screen bg-slate-950 text-white font-sans overflow-hidden flex flex-col relative select-none transition-all duration-500 ${isDangerZone ? 'shadow-[inset_0_0_150px_rgba(239,68,68,0.15)]' : ''}`}>

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
              player={activeCardPlayer}
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

      {leftOpp && (
        <OpponentPanel
          side="left"
          player={leftOpp}
          isBidding={String(highestBidder) === String(leftOpp.id)}
          currentBid={currentBid}
          message={messages[leftOpp.id]}
          open={openPanel === PANEL.squad(leftOpp.id)}
          onOpen={() => togglePanel(PANEL.squad(leftOpp.id))}
          onClose={closePanel}
        />
      )}

      {rightOpp && (
        <OpponentPanel
          side="right"
          player={rightOpp}
          isBidding={String(highestBidder) === String(rightOpp.id)}
          currentBid={currentBid}
          message={messages[rightOpp.id]}
          open={openPanel === PANEL.squad(rightOpp.id)}
          onOpen={() => togglePanel(PANEL.squad(rightOpp.id))}
          onClose={closePanel}
        />
      )}

      <SquadPanel
        open={openPanel === PANEL.squad(me.id)}
        title="Senin Kadron"
        squad={me.squad}
        onClose={closePanel}
      />

      <BottomBar
        me={me}
        currentBid={currentBid}
        auctionStatus={auctionStatus}
        isPassed={isPassed}
        amIWinning={amIWinning}
        message={messages[me.id]}
        squadOpen={openPanel === PANEL.squad(me.id)}
        onToggleSquad={() => togglePanel(PANEL.squad(me.id))}
        onBid={handleBid}
        onPass={handlePass}
      />

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

      <QuickChat
        open={openPanel === PANEL.CHAT}
        onToggle={() => togglePanel(PANEL.CHAT)}
        onClose={closePanel}
        onSend={sendQuickReply}
      />

      <AllSquadsOverlay
        open={openPanel === PANEL.ALL_SQUADS}
        onClose={closePanel}
        myId={me.id}
        timeLeft={timeLeft}
        currentBid={currentBid}
        participants={players}
      />
    </div>
  );
}