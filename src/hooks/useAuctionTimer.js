import { useCallback, useEffect, useState } from 'react';
import { ROUND_TIME } from '../constants/draft';

// Müzayede süresi. Süre 0'a inince ihale sonuçlanmış sayılır.
export default function useAuctionTimer() {
  const [timeLeft, setTimeLeft] = useState(ROUND_TIME);

  useEffect(() => {
    if (timeLeft <= 0) return;
    const timerId = setTimeout(() => setTimeLeft((t) => t - 1), 1000);
    return () => clearTimeout(timerId);
  }, [timeLeft]);

  const resetTimer = useCallback(() => setTimeLeft(ROUND_TIME), []);
  const auctionStatus = timeLeft === 0 ? 'sold' : 'bidding';

  return { timeLeft, auctionStatus, resetTimer };
}