import SquadButton from './SquadButton';
import { BID_STEPS, BOTTOM_BAR_HEIGHT, BUBBLE_MS } from '../../constants/draft';

const BID_BUTTON_CLASS =
  "px-8 py-4 rounded-2xl bg-slate-800 text-white hover:bg-emerald-600 font-black text-2xl border border-slate-700 transition-all hover:scale-105 active:scale-95 shadow-lg disabled:opacity-50 disabled:hover:scale-100 disabled:hover:bg-slate-800";

const PILL_CLASS = "px-12 py-4 rounded-2xl font-black text-xl tracking-widest uppercase border";

// Müzayede sürerken sağdaki butonlar: kilitli (pas geçtin / mevki dolu) / teklif sende / teklif ver
function BidActions({ budget, currentBid, lockedText, amIWinning, onBid, onPass }) {
  if (lockedText) {
    return <div className={`${PILL_CLASS} bg-slate-800 text-slate-500 border-slate-700`}>{lockedText}</div>;
  }

  if (amIWinning) {
    return (
      <div className={`${PILL_CLASS} bg-emerald-900/40 text-emerald-500 border-emerald-800/50 flex items-center gap-3`}>
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path></svg>
        EN YÜKSEK TEKLİF SENDE
      </div>
    );
  }

  return (
    <>
      <button onClick={onPass} className="px-8 py-4 rounded-2xl bg-red-950/50 text-red-500 hover:bg-red-900 hover:text-white font-black text-xl tracking-widest uppercase border border-red-900/50 transition-all active:scale-95 shadow-lg">
        ÇEKİL
      </button>
      <div className="w-px h-16 bg-slate-800 mx-2"></div>
      {BID_STEPS.map((amount) => (
        <button
          key={amount}
          onClick={() => onBid(amount)}
          disabled={budget < currentBid + amount}
          className={BID_BUTTON_CLASS}
        >
          +{amount}M
        </button>
      ))}
    </>
  );
}

// idleText: aktif ihale yokken sağda görünen yazı (ör. "İHALE SONUÇLANDI"). Boşsa teklif butonları görünür.
export default function BottomBar({
  me,
  currentBid,
  idleText,
  lockedText,
  amIWinning,
  message,
  squadOpen,
  onToggleSquad,
  onBid,
  onPass,
}) {
  return (
    <div
      className="absolute bottom-0 w-full bg-slate-900/90 border-t border-slate-800 flex items-center justify-between px-12 z-30 backdrop-blur-lg shadow-[0_-10px_40px_rgba(0,0,0,0.5)]"
      style={{ height: BOTTOM_BAR_HEIGHT }}
    >
      <div className="flex items-center gap-6">
        {/* Profil avatarı + konuşma balonu */}
        <div className="relative">
          <div className="w-20 h-20 bg-slate-800 rounded-2xl flex items-center justify-center text-4xl border border-slate-700 shadow-inner">{me.avatar}</div>

          {message && (
            <div
              key={message.id}
              className="absolute left-10 bottom-full mb-8 bg-white text-slate-900 px-4 py-2 rounded-2xl rounded-bl-none font-black text-base shadow-lg whitespace-nowrap pointer-events-none"
              style={{ animation: `chat-bubble ${BUBBLE_MS}ms ease both` }}
            >
              {message.text}
            </div>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <div>
            <p className="text-slate-400 font-bold uppercase tracking-widest text-xs">Kalan Bütçen</p>
            <p className={`text-3xl font-black tracking-wider ${me.budget < currentBid ? 'text-red-500' : 'text-emerald-400'}`}>
              {me.budget}M €
            </p>
          </div>

          <SquadButton compact label="Kadrom" onClick={onToggleSquad} active={squadOpen} />
        </div>
      </div>

      {idleText ? (
        <div className={`${PILL_CLASS} bg-slate-800 text-yellow-500 border-slate-700 animate-fade-in`}>
          {idleText}
        </div>
      ) : (
        <div className="flex gap-4">
          <BidActions
            budget={me.budget}
            currentBid={currentBid}
            lockedText={lockedText}
            amIWinning={amIWinning}
            onBid={onBid}
            onPass={onPass}
          />
        </div>
      )}
    </div>
  );
}