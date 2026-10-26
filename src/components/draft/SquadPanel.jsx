import Pitch from './Pitch';
import SquadSummary from './SquadSummary';
import { BOTTOM_BAR_HEIGHT } from '../../constants/draft';

// Kendi kadron: alt barın üstünde açılan küçük panel.
export default function SquadPanel({ open, title, squad, onClose }) {
  return (
    <div
      className="absolute z-40 left-12 flex pointer-events-none"
      style={{ bottom: BOTTOM_BAR_HEIGHT + 16 }}
    >
      <div
        inert={!open}
        aria-hidden={!open}
        className={`w-[300px] bg-slate-900 border-2 border-emerald-500 rounded-3xl p-4 shadow-[0_20px_50px_rgba(0,0,0,0.9)] flex flex-col gap-3 transition-all duration-300 ease-out ${
          open ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none translate-y-4'
        }`}
      >
        <div className="flex justify-between items-center border-b border-slate-800 pb-2">
          <div>
            <h4 className="font-black text-emerald-400 tracking-wider text-sm uppercase">{title}</h4>
            <SquadSummary squad={squad} className="text-[11px] text-slate-400 mt-0.5" />
          </div>
          <button onClick={onClose} aria-label="Kadroyu kapat" className="text-slate-400 hover:text-white font-bold text-sm px-2 py-1">✕</button>
        </div>

        <div style={{ height: 'min(380px, 46vh)' }}>
          <Pitch squad={squad} />
        </div>
      </div>
    </div>
  );
}