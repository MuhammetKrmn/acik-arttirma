import React from 'react';

export default function PlayerCard({ player, currentBid, highestBidder }) {
  return (
    <div className="flex flex-col items-center justify-center p-4 relative z-10 pointer-events-auto">
      
      {/* 1. Grade ve Mevki */}
      <div className="text-2xl font-black text-emerald-400 tracking-widest mb-1 drop-shadow-md">
        {player.rating} {player.position}
      </div>
      
      {/* 2. Oyuncu Adı */}
      <h2 className="text-5xl font-black text-white tracking-widest mb-6 uppercase drop-shadow-[0_0_15px_rgba(255,255,255,0.3)]">
        {player.name}
      </h2>
      
      {/* 3. Oyuncu Fotoğrafı (Sıfır Çerçeve, Sadece Görsel) */}
      <div className="w-56 h-56 mb-8 flex justify-center items-center">
         {/* Ömer'in DB'sinden resim gelene kadar varsayılan bir silüet */}
         <img 
           src={player.image || "https://cdn-icons-png.flaticon.com/512/8450/8450700.png"} 
           alt={player.name} 
           className="w-full h-full object-contain drop-shadow-[0_20px_30px_rgba(0,0,0,0.8)]"
         />
      </div>
      
      {/* 4. Dinamik Fiyat ve Sahip Alanı (Çizimindeki oval yapı) */}
      <div className="bg-slate-900/90 px-12 py-4 rounded-full flex flex-col items-center justify-center shadow-[0_10px_30px_rgba(0,0,0,0.6)] backdrop-blur-md">
        <span className="text-4xl font-black text-yellow-400 drop-shadow-md">
          {currentBid}M €
        </span>
        <span className="text-xs font-bold text-slate-400 uppercase tracking-widest mt-2">
          {highestBidder ? `TEKLİF: ${highestBidder}` : 'BAŞLANGIÇ FİYATI'}
        </span>
      </div>
      
    </div>
  );
}