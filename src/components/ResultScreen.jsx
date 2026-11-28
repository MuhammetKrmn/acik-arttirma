import { ACQUISITION_LABELS, SLOT_ORDER, avatarForSeat, slotLabel } from '../game/labels';

// Oyun sonu: GameResultView (game_finished olayı veya GET /api/rooms/{id}/result).
// Reytingler burada ilk kez görünür.
function PlayerColumn({ player, isMe }) {
  const squad = [...player.squad].sort((a, b) => SLOT_ORDER.indexOf(a.slot) - SLOT_ORDER.indexOf(b.slot));

  return (
    <div className={`flex-1 min-w-0 max-w-[420px] self-start flex flex-col gap-3 rounded-3xl border p-5 ${player.winner ? 'bg-yellow-500/10 border-yellow-500/60 shadow-[0_0_40px_rgba(234,179,8,0.25)]' : 'bg-slate-900 border-slate-800'}`}>
      <div className="flex items-center gap-3">
        <div className="w-14 h-14 shrink-0 bg-slate-800 rounded-full flex items-center justify-center text-3xl border-2 border-slate-600">
          {avatarForSeat(player.seatNumber)}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="font-black text-xl text-white truncate">{player.username}</h3>
            {isMe && <span className="bg-emerald-600 text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 rounded-full">Sen</span>}
          </div>
          {player.winner && <p className="text-yellow-400 font-black text-xs uppercase tracking-widest">🏆 Kazanan</p>}
        </div>
        <div className="text-right">
          <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Toplam Reyting</p>
          <p className="text-3xl font-black text-yellow-400">{player.totalRating}</p>
        </div>
      </div>

      <div className="flex justify-between text-xs font-bold text-slate-400 px-1">
        <span>Harcanan: <span className="text-white">{player.totalSpent}M €</span></span>
        <span>Kalan: <span className="text-emerald-400">{player.remainingBudget}M €</span></span>
      </div>

      <div className="flex flex-col gap-1.5">
        {squad.map((s) => {
          const badge = ACQUISITION_LABELS[s.acquisitionType];
          return (
            <div key={s.slot} className="flex items-center gap-2 bg-slate-950/60 border border-slate-800 rounded-xl px-3 py-1.5">
              <span className="w-10 text-[11px] font-black text-emerald-300">{slotLabel(s.slot)}</span>
              <span className="flex-1 min-w-0 truncate text-sm font-bold">{s.playerName}</span>
              {badge && <span className="text-[10px] font-black bg-slate-700 text-slate-200 px-1.5 py-0.5 rounded">{badge}</span>}
              <span className="w-12 text-right text-xs text-slate-400">{s.paidPrice}M</span>
              <span className="w-8 text-right text-sm font-black text-yellow-400">{s.overallRating}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function ResultScreen({ result, session, onBackToMenu }) {
  const players = [...result.players].sort((a, b) => a.seatNumber - b.seatNumber);
  const winners = players.filter((p) => p.winner).map((p) => p.username);

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans flex flex-col p-8 gap-6 animate-fade-in">
      <div className="flex items-center justify-between border-b border-slate-800 pb-6">
        <div>
          <h1 className="text-4xl font-black tracking-tighter">OYUN BİTTİ</h1>
          <p className="text-slate-400 mt-1 font-bold tracking-widest uppercase text-sm">
            {result.draw ? 'Berabere!' : `Kazanan: ${winners.join(', ')}`}
          </p>
        </div>
        <button
          onClick={onBackToMenu}
          className="px-8 py-4 rounded-2xl bg-indigo-600 hover:bg-indigo-500 font-black tracking-widest uppercase shadow-lg transition-all hover:scale-105"
        >
          Ana Menü
        </button>
      </div>

      <div className="flex-1 flex justify-center gap-6">
        {players.map((p) => (
          <PlayerColumn key={p.roomPlayerId} player={p} isMe={p.userId === session.userId} />
        ))}
      </div>

      <p className="text-center text-xs text-slate-500">
        Sıralama: toplam reyting, eşitse kalan bütçe. T3 = otomatik Tier 3 kartı, U19 = bütçe yetmediği için verilen ceza kartı.
      </p>
    </div>
  );
}