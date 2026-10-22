// Sunucu bağlanana kadar kullanılan örnek veriler.
// Gerçek veri gelince bu dosya silinebilir.

export const MOCK_PLAYER = {
  name: "L. MESSI",
  rating: 94,
  position: "RW",
  country: "🇦🇷",
  stats: { pac: 85, dri: 95, sho: 92, def: 38, pas: 91, phy: 68 }
};

// id'si myId ile eşleşen kişi "Sen"dir, diğer ikisi sol ve sağ panele yerleşir.
export const DEFAULT_PLAYERS = [
  { id: 'me', name: 'Camsat_123', avatar: '👑', budget: 100, squad: { RW: { name: "L. MESSI", rating: 94 } } },
  { id: 'emir', name: 'Emir', avatar: '🦅', budget: 100, squad: { CM: { name: "K. DE BRUYNE", rating: 91 } } },
  { id: 'eren', name: 'Eren', avatar: '🐺', budget: 100, squad: { ST: { name: "E. HAALAND", rating: 91 } } },
];