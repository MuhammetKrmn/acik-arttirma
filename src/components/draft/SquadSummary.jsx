import { getSquadStats } from '../../utils/squad';

// "3/11 oyuncu · Ortalama 92" satırı
export default function SquadSummary({ squad, className = 'text-[11px] text-slate-400' }) {
  const { count, avg } = getSquadStats(squad);

  return (
    <p className={className}>
      {count}/11 oyuncu{avg ? ` · Ortalama ${avg}` : ''}
    </p>
  );
}