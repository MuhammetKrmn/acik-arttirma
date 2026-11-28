import { Client } from '@stomp/stompjs';
import { WS_URL } from './api';

// Tek bir STOMP bağlantısı. Lobi ve arena AYNI bağlantıyı kullanır:
// ekran değişince bağlantı kopmaz, sadece olayları dinleyen fonksiyonlar (handlers) değişir.
// Bağlantı koparsa kütüphane 3 sn'de bir yeniden bağlanır ve onConnected tekrar çağrılır
// (ekran bu anda kendini REST ile senkronlar).
class SocketService {
  constructor() {
    this.client = null;
    this.key = null;
    this.handlers = {};
  }

  /**
   * handlers: { onRoomEvent, onPersonalEvent, onError, onConnected }
   * Aynı token ve oda için zaten bağlıysa yeniden bağlanmaz, sadece handlers'ı günceller.
   */
  connect(playerToken, roomId, handlers = {}) {
    this.handlers = handlers;
    const key = `${playerToken}|${roomId}`;
    if (this.client && this.key === key) {
      if (this.client.connected) this.handlers.onConnected?.();
      return;
    }
    this.disconnect();
    this.key = key;

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { 'X-Player-Token': playerToken },
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/rooms/${roomId}`, (m) => this.handlers.onRoomEvent?.(JSON.parse(m.body)));
        client.subscribe('/user/queue/bids', (m) => this.handlers.onPersonalEvent?.(JSON.parse(m.body)));
        client.subscribe('/user/queue/errors', (m) => this.handlers.onError?.(JSON.parse(m.body)));
        this.handlers.onConnected?.();
      },
      onStompError: (frame) => {
        console.error('STOMP hatası:', frame.headers['message']);
      },
    });
    this.client = client;
    client.activate();
  }

  /** Ekran değişince olayları yeni ekrana yönlendirmek için. */
  setHandlers(handlers = {}) {
    this.handlers = handlers;
  }

  sendBid(roomId, expectedPrice, increment) {
    if (!this.client?.connected) return false;
    this.client.publish({
      destination: `/app/rooms/${roomId}/bid`,
      body: JSON.stringify({ expectedPrice, increment }),
    });
    return true;
  }

  sendWithdraw(roomId) {
    if (!this.client?.connected) return false;
    this.client.publish({ destination: `/app/rooms/${roomId}/withdraw` });
    return true;
  }

  /** Sadece oyundan tamamen çıkarken (ana menüye dönüş, çıkış) çağrılır. */
  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.key = null;
    }
  }
}

export const socketService = new SocketService();