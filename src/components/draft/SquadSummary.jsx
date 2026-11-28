import { getSquadStats } from '../../utils/squad';

// "3/11 oyuncu · 120M harcandı" satırı
export default function SquadSummary({ squad, className = 'text-[11px] text-slate-400' }) {
  const { count, spent } = getSquadStats(squad);

  return (
    <p className={className}>
      {count}/11 oyuncu{count ? ` · ${spent}M harcandı` : ''}
    </p>
  );
}