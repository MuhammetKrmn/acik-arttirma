import Pitch from './Pitch';
import SquadButton from './SquadButton';
import SquadSummary from './SquadSummary';
import {
  BOTTOM_BAR_HEIGHT,
  BUBBLE_MS,
  COLLAPSED_H,
  COLLAPSED_W,
  EASE,
  EXPANDED_H,
  EXPANDED_W,
} from '../../constants/draft';

// Rakip kartı: kapalıyken profil, açılınca kadro ekranına dönüşür.
export default function OpponentPanel({ side, player, isBidding, currentBid, message, open, onOpen, onClose }) {
  const { name, avatar, budget, squad } = player;
  const isLeft = side === 'left';
  // Kart dış kenarına sabit kalır, içeri doğru büyür
  const anchor = isLeft ? 'left-0' : 'right-0';

  return (
    <div
      className={`absolute top-0 ${isLeft ? 'left-8' : 'right-8'} flex items-center pointer-events-none ${open ? 'z-40' : 'z-20'}`}
      style={{ bottom: BOTTOM_BAR_HEIGHT }}
    >
      {/* Boyutu animasyonla değişen kutu */}
      <div
        className="relative pointer-events-auto"
        style={{
          width: open ? EXPANDED_W : COLLAPSED_W,
          height: open ? EXPANDED_H : COLLAPSED_H,
          transition: `width 550ms ${EASE}, height 550ms ${EASE}`,
        }}
      >
        <div
          className={`absolute inset-0 overflow-hidden rounded-3xl border backdrop-blur-sm ${
            open
              ? 'bg-slate-900 border-emerald-500/70 shadow-[0_20px_60px_rgba(0,0,0,0.85)]'
              : 'bg-slate-900/80 border-slate-700 shadow-xl'
          }`}
          style={{ transition: 'background-color 400ms, border-color 400ms, box-shadow 400ms' }}
        >
          {/* KATMAN 1: Profil (kapalıyken görünür) */}
          <div
            inert={open}
            aria-hidden={open}
            className={`absolute top-0 bottom-0 ${anchor} w-[142px] p-4 flex flex-col items-center justify-center gap-3`}
            style={{
              opacity: open ? 0 : 1,
              transform: open ? 'scale(0.92)' : 'scale(1)',
              transition: open
                ? 'opacity 150ms ease, transform 250ms ease'
                : 'opacity 250ms ease 300ms, transform 300ms ease 300ms',
            }}
          >
            <div className="w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center text-3xl border-2 border-slate-600 shadow-inner">{avatar}</div>
            <div className="text-center">
              <h3 className="font-bold text-slate-200">{name}</h3>
              <p className="text-emerald-400 font-black tracking-wider text-sm mt-1">{budget}M</p>
            </div>
            <SquadButton onClick={onOpen} active={false} />
          </div>

          {/* KATMAN 2: Kadro ekranı (açıkken görünür) */}
          <div
            inert={!open}
            aria-hidden={!open}
            className={`absolute top-0 bottom-0 ${anchor} w-[318px] p-4 flex flex-col gap-3`}
            style={{
              opacity: open ? 1 : 0,
              transform: open ? 'translateY(0)' : 'translateY(10px)',
              transition: open
                ? 'opacity 350ms ease 250ms, transform 400ms ease 250ms'
                : 'opacity 150ms ease, transform 200ms ease',
            }}
          >
            {/* Üst: avatar, isim, kapat */}
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 shrink-0 bg-slate-800 rounded-full flex items-center justify-center text-xl border-2 border-slate-600">{avatar}</div>
              <div className="flex-1 min-w-0">
                <h4 className="font-black text-white text-lg leading-tight truncate">{name}</h4>
                <SquadSummary squad={squad} />
              </div>
              <button
                onClick={onClose}
                aria-label="Kadroyu kapat"
                className="text-slate-400 hover:text-white font-bold text-sm px-2 py-1"
              >
                ✕
              </button>
            </div>

            {/* Kalan bütçe */}
            <div className="flex items-center justify-between px-3 py-2 rounded-xl bg-slate-950/60 border border-slate-800">
              <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Kalan Bütçe</span>
              <span className="text-xl font-black text-emerald-400 tracking-wider">{budget}M €</span>
            </div>

            {/* Saha: kalan alanı doldurur */}
            <div className="flex-1 min-h-0">
              <Pitch squad={squad} />
            </div>
          </div>
        </div>

        {/* Teklif balonu: kutunun dışında, kırpılmaz */}
        {isBidding && (
          <div className={`absolute -top-12 inset-x-0 mx-auto w-fit bg-white text-slate-900 px-4 py-1.5 rounded-2xl ${isLeft ? 'rounded-bl-none' : 'rounded-br-none'} font-black text-lg shadow-lg whitespace-nowrap`}>
            {currentBid}M!
          </div>
        )}

        {/* Sohbet balonu: kartın ekran ortasına bakan yanında çıkar */}
        {message && (
          <div
            key={message.id}
            className={`absolute top-4 z-50 bg-white text-slate-900 px-4 py-2 rounded-2xl font-black text-base shadow-lg whitespace-nowrap pointer-events-none ${
              isLeft ? 'left-full ml-3 rounded-bl-none' : 'right-full mr-3 rounded-br-none'
            }`}
            style={{ animation: `chat-bubble ${BUBBLE_MS}ms ease both` }}
          >
            {message.text}
          </div>
        )}
      </div>
    </div>
  );
}