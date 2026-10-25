import { useCallback, useEffect, useRef, useState } from 'react';
import { BUBBLE_MS, DEMO_BOTS, QUICK_REPLIES } from '../constants/draft';

// Oyuncuların yanında çıkan konuşma balonlarını yönetir.
// messages: { [playerId]: { id, text } }
export default function useChatBubbles({ players, myId, incomingMessage }) {
  const [messages, setMessages] = useState({});
  const timersRef = useRef({});

  // Bir oyuncunun yanında balon göster, BUBBLE_MS sonra sil
  const showMessage = useCallback((playerId, text) => {
    clearTimeout(timersRef.current[playerId]);
    setMessages((prev) => ({
      ...prev,
      [playerId]: { id: `${Date.now()}-${Math.random()}`, text },
    }));
    timersRef.current[playerId] = setTimeout(() => {
      setMessages((prev) => {
        const next = { ...prev };
        delete next[playerId];
        return next;
      });
    }, BUBBLE_MS);
  }, []);

  // Sayfadan çıkınca bekleyen zamanlayıcıları temizle
  useEffect(() => {
    const timers = timersRef.current;
    return () => Object.values(timers).forEach(clearTimeout);
  }, []);

  // Sunucudan gelen mesaj: { playerId, text }
  useEffect(() => {
    if (incomingMessage) showMessage(incomingMessage.playerId, incomingMessage.text);
  }, [incomingMessage, showMessage]);

  // Geliştirme modunda rakipler ara sıra rastgele hızlı mesaj atar
  useEffect(() => {
    if (!DEMO_BOTS) return;
    let timerId;
    const schedule = () => {
      timerId = setTimeout(() => {
        const others = players.filter((p) => p.id !== myId);
        const bot = others[Math.floor(Math.random() * others.length)];
        const text = QUICK_REPLIES[Math.floor(Math.random() * QUICK_REPLIES.length)];
        if (bot) showMessage(bot.id, text);
        schedule();
      }, 7000 + Math.random() * 6000);
    };
    schedule();
    return () => clearTimeout(timerId);
  }, [players, myId, showMessage]);

  return { messages, showMessage };
}