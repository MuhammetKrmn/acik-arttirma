import { useEffect, useState } from 'react';

// Sunucunun verdiği bitiş anına göre geri sayım (saniye).
// deadlineLocal: bu bilgisayarın saatine çevrilmiş bitiş anı (ms) veya null.
// Çevirme: deadlineLocal = mesajın geldiği an + (deadlineMs - serverNowMs).
export default function useCountdown(deadlineLocal) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!deadlineLocal) return undefined;
    const id = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(id);
  }, [deadlineLocal]);

  if (!deadlineLocal) return null;
  return Math.max(0, Math.ceil((deadlineLocal - now) / 1000));
}