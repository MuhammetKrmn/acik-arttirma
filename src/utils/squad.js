// Kadrodaki oyuncu sayısı ve ortalama reyting
export function getSquadStats(squad) {
  const players = Object.values(squad);
  const avg = players.length
    ? Math.round(players.reduce((sum, p) => sum + p.rating, 0) / players.length)
    : null;
  return { count: players.length, avg };
}