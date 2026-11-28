// Ekranda gösterilen sabit yazılar ve koltuk avatarları.

// Backend avatar tutmaz; koltuk numarasına göre veriyoruz (1, 2, 3).
export const SEAT_AVATARS = ['👑', '🦅', '🐺'];
export const avatarForSeat = (seatNumber) => SEAT_AVATARS[(seatNumber - 1) % SEAT_AVATARS.length];

// bid_rejected / withdraw_rejected sebepleri (docs/olay-sozlesmesi.md, bölüm 5)
export const REJECT_REASONS = {
  STALE_PRICE: 'Fiyat değişti, tekrar dene',
  INSUFFICIENT_BUDGET: 'Bütçen bu teklife yetmiyor',
  SLOT_FULL: 'Bu mevki kadronda zaten dolu',
  ALREADY_LEADER: 'En yüksek teklif zaten sende',
  PLAYER_WITHDRAWN: 'Bu ihaleden çekildin',
  PLAYER_DISCONNECTED: 'Bağlantın koptuğu için bu ihaleye giremezsin, sonrakine girebilirsin',
  AUCTION_CLOSED: 'İhale kapandı',
  INVALID_INCREMENT: 'Geçersiz artış',
  UNKNOWN_PLAYER: 'Bu odanın oyuncusu değilsin',
  LEADER_CANNOT_WITHDRAW: 'En yüksek teklif sendeyken çekilemezsin',
  ALREADY_WITHDRAWN: 'Zaten çekildin',
};

export const reasonText = (reason) => REJECT_REASONS[reason] ?? `İşlem reddedildi (${reason})`;

// Sonuç ekranında kartın nasıl alındığı
export const ACQUISITION_LABELS = {
  AUCTION: null,
  TIER3_AUTO: 'T3',
  U19_PENALTY: 'U19',
};

// Kadro sırası (saha dizilişi değil, liste sırası)
export const SLOT_ORDER = ['GK', 'LB', 'CB_1', 'CB_2', 'RB', 'CDM', 'CM', 'CAM', 'LW', 'ST', 'RW'];
export const slotLabel = (slot) => slot.replace(/_\d$/, '');