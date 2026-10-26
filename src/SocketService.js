import { Client } from '@stomp/stompjs';

class SocketService {
  constructor() {
    this.client = null;
  }

  // Canlı ihaleye bağlanma fonksiyonu
  connect(playerToken, roomId, onRoomEvent, onPersonalEvent, onError) {
    if (!playerToken) {
      console.error("🔴 HATA: Player Token bulunamadı! Misafir girişi (guest login) yapıldı mı?");
      return;
    }

    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws', // Backend ekibinin verdiği adres
      connectHeaders: {
        'X-Player-Token': playerToken, // Güvenlik başlığı
      },
      reconnectDelay: 5000, // Koparsa 5 saniyede bir tekrar dener
      
      onConnect: () => {
        console.log("🟢 Canlı İhale Sunucusuna Bağlanıldı!");

        // 1. Odadaki HERKESE giden genel olaylar (game_started, bid_accepted vb.)
        this.client.subscribe(`/topic/rooms/${roomId}`, (message) => {
          const data = JSON.parse(message.body);
          onRoomEvent(data);
        });

        // 2. SADECE SANA gelen özel ret mesajları (bid_rejected, withdraw_rejected)
        this.client.subscribe('/user/queue/bids', (message) => {
          const data = JSON.parse(message.body);
          onPersonalEvent(data);
        });

        // 3. SADECE SANA gelen sistem hataları
        this.client.subscribe('/user/queue/errors', (message) => {
          const data = JSON.parse(message.body);
          onError(data);
        });
      },
      
      onStompError: (frame) => {
        console.error('🔴 STOMP Hatası:', frame.headers['message']);
      },
    });

    this.client.activate(); // Motoru çalıştır
  }

  // Fiyat artırma (Teklif) gönderme fonksiyonu
  sendBid(roomId, expectedPrice, increment) {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination: `/app/rooms/${roomId}/bid`,
        body: JSON.stringify({ expectedPrice, increment }),
      });
    } else {
      console.warn("Bağlantı yok, teklif gönderilemedi!");
    }
  }

  // İhaleden çekilme fonksiyonu
  sendWithdraw(roomId) {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination: `/app/rooms/${roomId}/withdraw`,
      });
    }
  }

  // Sayfadan çıkınca bağlantıyı temizleme
  disconnect() {
    if (this.client) {
      this.client.deactivate();
      console.log("⚪ Bağlantı koparıldı.");
    }
  }
}

// Tüm projenin kullanacağı tek bir servis kopyası oluşturuyoruz
export const socketService = new SocketService();