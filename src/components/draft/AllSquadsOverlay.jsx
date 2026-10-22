import Pitch from './Pitch';
import SquadSummary from './SquadSummary';
import { EASE } from '../../constants/draft';

function SquadColumn({ participant, isMe, index, open }) {
  return (
    <div
      className="flex-1 min-w-0 max-w-[480px] flex flex-col gap-3"
      style={{
        opacity: open ? 1 : 0,
        transform: open ? 'translateY(0)' : 'translateY(28px)',
        transition: `opacity 450ms ease, transform 550ms ${EASE}`,
        transitionDelay: open ? `${120 + index * 90}ms` : '0ms',
      }}
    >
      {/* Üst: kimin kadrosu + kalan bütçe */}
      <div className={`flex items-center gap-3 rounded-2xl bg-slate-900 border px-4 py-3 ${isMe ? 'border-emerald-500/70' : 'border-slate-800'}`}>
        <div className="w-12 h-12 shrink-0 bg-slate-800 rounded-full flex items-center justify-center text-2xl border-2 border-slate-600">
          {participant.avatar}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="font-black text-lg text-white truncate">{participant.name}</h3>
            {isMe && (
              <span className="bg-emerald-600 text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 rounded-full">Sen</span>
            )}
          </div>
          <SquadSummary squad={participant.squad} />
        </div>
        <div className="text-right">
          <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Kalan Bütçe</p>
          <p className="text-2xl font-black text-emerald-400 tracking-wider">{participant.budget}M €</p>
        </div>
      </div>

      {/* Saha */}
      <div className="flex-1 min-h-0">
        <Pitch squad={participant.squad} />
      </div>
    </div>
  );
}

// Bütün kadrolar ekranı: üç kişinin sahası yan yana. Esc ile kapanma ana ekranda.
export default function AllSquadsOverlay({ open, onClose, participants, myId, timeLeft, currentBid }) {
  return (
    <div
      inert={!open}
      aria-hidden={!open}
      className={`absolute inset-0 z-50 flex flex-col bg-slate-950/95 backdrop-blur-md transition-opacity duration-300 ${
        open ? 'opacity-100' : 'opacity-0 pointer-events-none'
      }`}
    >
      <header className="flex items-center justify-between px-8 py-5 border-b border-slate-800">
        <div>
          <h2 className="text-2xl font-black tracking-widest uppercase text-white">Bütün Kadrolar</h2>
          <p className="text-xs text-slate-400 mt-0.5">Müzayede arkada devam ediyor. Esc ile geri dönebilirsin.</p>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-xl px-4 py-2">
            <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Süre</span>
            <span className={`text-xl font-black ${timeLeft <= 3 ? 'text-red-500' : 'text-emerald-400'}`}>{timeLeft}</span>
          </div>
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-xl px-4 py-2">
            <span className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Teklif</span>
            <span className="text-xl font-black text-yellow-400">{currentBid}M €</span>
          </div>
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 hover:border-emerald-400 text-sm font-bold tracking-wider text-slate-200 transition-all"
          >
            ✕ Geri Dön
          </button>
        </div>
      </header>

      <div className="flex-1 min-h-0 flex justify-center gap-6 px-8 py-6">
        {participants.map((p, i) => (
          <SquadColumn key={p.id} participant={p} isMe={p.id === myId} index={i} open={open} />
        ))}
      </div>
    </div>
  );
}