// İhaledeki kart. Reyting ve fiziksel değerler oyun bitene kadar gizli (oyun kuralı),
// bu yüzden sadece mevki, isim, kulüp, uyruk ve fotoğraf gösterilir.
const FALLBACK_PHOTO = 'https://cdn-icons-png.flaticon.com/512/8450/8450700.png';

export default function PlayerCard({ card, tier, currentBid, leaderLabel }) {
  return (
    <div className="flex flex-col items-center justify-center p-4 relative z-10 pointer-events-auto">

      {/* 1. Mevki ve kart seviyesi (T1 / T2) */}
      <div className="text-2xl font-black text-emerald-400 tracking-widest mb-1 drop-shadow-md">
        {card.position}
        <span className="text-slate-400 text-base ml-3">{tier}</span>
      </div>

      {/* 2. Oyuncu adı */}
      <h2 className="text-5xl font-black text-white tracking-widest mb-2 uppercase drop-shadow-[0_0_15px_rgba(255,255,255,0.3)] text-center">
        {card.playerName}
      </h2>
      <p className="text-sm font-bold text-slate-300 tracking-wide mb-6 text-center max-w-md">
        {card.nationality}{card.club ? ` · ${card.club}` : ''}
      </p>

      {/* 3. Fotoğraf. Dosya yoksa silüet gösterilir. */}
      <div className="w-56 h-56 mb-8 flex justify-center items-center">
        <img
          key={card.cardId}
          src={card.photoUrl || FALLBACK_PHOTO}
          onError={(e) => {
            if (e.currentTarget.src !== FALLBACK_PHOTO) e.currentTarget.src = FALLBACK_PHOTO;
          }}
          alt={card.playerName}
          className="w-full h-full object-contain drop-shadow-[0_20px_30px_rgba(0,0,0,0.8)]"
        />
      </div>

      {/* 4. Güncel fiyat ve en yüksek teklif sahibi */}
      <div className="bg-slate-900/90 px-12 py-4 rounded-full flex flex-col items-center justify-center shadow-[0_10px_30px_rgba(0,0,0,0.6)] backdrop-blur-md">
        <span className="text-4xl font-black text-yellow-400 drop-shadow-md">
          {currentBid}M €
        </span>
        <span className="text-xs font-bold text-slate-400 uppercase tracking-widest mt-2">
          {leaderLabel ? `TEKLİF: ${leaderLabel}` : `AÇILIŞ: ${card.startingPrice}M €`}
        </span>
      </div>

    </div>
  );
}