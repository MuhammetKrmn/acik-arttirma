import { FORMATION_POSITIONS } from '../../constants/draft';

// Saha + oyuncular. Bulunduğu kutuyu tamamen doldurur (w-full h-full).
export default function Pitch({ squad }) {
  return (
    <div className="w-full h-full bg-gradient-to-b from-emerald-900 to-emerald-950 rounded-2xl border-2 border-emerald-600/60 relative overflow-hidden shadow-inner">
      <div className="absolute inset-0 opacity-15 bg-[linear-gradient(to_right,#808080_1px,transparent_1px),linear-gradient(to_bottom,#808080_1px,transparent_1px)] bg-[size:24px_24px]"></div>

      <div className="absolute top-1/2 w-full h-px bg-emerald-500/40"></div>
      <div className="absolute inset-0 m-auto w-20 h-20 border border-emerald-500/40 rounded-full"></div>
      <div className="absolute bottom-0 inset-x-0 mx-auto w-28 h-10 border-t-2 border-l-2 border-r-2 border-emerald-500/50 rounded-t-lg"></div>
      <div className="absolute top-0 inset-x-0 mx-auto w-28 h-10 border-b-2 border-l-2 border-r-2 border-emerald-500/50 rounded-b-lg"></div>

      {FORMATION_POSITIONS.map((pos) => {
        const playerInSlot = squad[pos.id] || (pos.id === 'CB1' ? squad['CB'] : null);
        const label = pos.id.replace(/[0-9]/g, '');

        return (
          <div
            key={pos.id}
            style={{ top: pos.top, left: pos.left, transform: 'translate(-50%, -50%)' }}
            className="absolute flex flex-col items-center z-10"
          >
            {playerInSlot ? (
              <div className="bg-slate-950/90 border border-yellow-500 px-2 py-1 rounded-lg flex flex-col items-center shadow-lg w-20 text-center">
                <span className="text-[10px] font-black text-yellow-400 leading-none">{playerInSlot.rating} {label}</span>
                <span className="text-[9px] font-bold text-white truncate w-full">{playerInSlot.name}</span>
              </div>
            ) : (
              <div className="w-8 h-8 rounded-full bg-emerald-950/80 border border-dashed border-emerald-400/60 flex items-center justify-center shadow-md">
                <span className="text-[10px] font-black text-emerald-300">{label}</span>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}