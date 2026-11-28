// Alt kontrol barının yüksekliği (px).
// Orta sahne, yan paneller ve kadro paneli buna göre hizalanır.
export const BOTTOM_BAR_HEIGHT = 128;

// Müzayede. Süreyi sunucu yönetir (son tekliften 10 sn sonra kapanır), burada süre sabiti yok.
export const BID_STEPS = [1, 5, 10];

// Sohbet
export const QUICK_REPLIES = [
  "Bu oyuncu benim! 😎",
  "Bu fiyata mı? 😅",
  "Bütçem sağlam 💪",
  "Güzel hamle 👏",
  "Süre daralıyor ⏳",
];
export const BUBBLE_MS = 3500; // konuşma balonunun ekranda kalma süresi

// Rakiplerin rastgele mesaj atması (sadece tasarım denemesi içindi). Gerçek oyunda kapalı.
export const DEMO_BOTS = false;

// Animasyon
export const EASE = 'cubic-bezier(0.22, 1, 0.36, 1)';

// Rakip kartı ölçüleri
export const COLLAPSED_W = 144;
export const COLLAPSED_H = 208;
export const EXPANDED_W = 320;
export const EXPANDED_H = `min(560px, calc(100vh - ${BOTTOM_BAR_HEIGHT}px - 2.5rem))`;

// Sohbet paneli ölçüleri
export const CHAT_BTN = 48;
export const CHAT_W = 256;
export const CHAT_H = 280;

// 4-3-3'e benzeyen saha dizilişi (yüzde olarak konum). id'ler backend'in slot adlarıyla aynı.
export const FORMATION_POSITIONS = [
  { id: 'ST', top: '10%', left: '50%' },
  { id: 'LW', top: '25%', left: '20%' },
  { id: 'RW', top: '25%', left: '80%' },
  { id: 'CM', top: '45%', left: '30%' },
  { id: 'CAM', top: '45%', left: '70%' },
  { id: 'CDM', top: '60%', left: '50%' },
  { id: 'LB', top: '70%', left: '15%' },
  { id: 'RB', top: '70%', left: '85%' },
  { id: 'CB_1', top: '80%', left: '35%' },
  { id: 'CB_2', top: '80%', left: '65%' },
  { id: 'GK', top: '90%', left: '50%' },
];