// Kadrodaki oyuncu sayısı ve harcanan para.
// Reyting oyun bitene kadar gizli olduğu için ortalama reyting gösterilmez.
export function getSquadStats(squad) {
  const players = Object.values(squad);
  const spent = players.reduce((sum, p) => sum + (p.price ?? 0), 0);
  return { count: players.length, spent };
}