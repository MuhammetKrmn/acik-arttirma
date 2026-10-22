export default function SquadButton({ onClick, active, label = 'Kadro', compact = false }) {
  return (
    <button
      onClick={onClick}
      aria-pressed={active}
      className={`border flex items-center justify-center gap-2 transition-all group shadow-md ${
        active
          ? 'bg-emerald-950/70 border-emerald-400'
          : 'bg-slate-800 hover:bg-slate-700 border-emerald-500/40 hover:border-emerald-400'
      } ${compact ? 'py-1 px-3 rounded-lg w-fit' : 'w-full py-2 px-3 rounded-xl'}`}
    >
      <div className={`${compact ? 'w-4 h-3' : 'w-5 h-3.5'} border border-emerald-400/70 rounded-sm relative flex items-center justify-center bg-emerald-950/40 group-hover:scale-110 transition-transform`}>
        <div className="absolute w-1.5 h-1.5 border border-emerald-400/70 rounded-full"></div>
        <div className="absolute w-full h-px bg-emerald-400/50"></div>
      </div>
      <span className={`${compact ? 'text-[11px]' : 'text-xs'} font-bold tracking-wider text-emerald-300`}>{label}</span>
    </button>
  );
}