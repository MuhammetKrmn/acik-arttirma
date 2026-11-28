// Arena ekranının bütün durumu. Sadece sunucudan gelen veriyle değişir:
// GET /live cevabı ve WebSocket olayları. Butona basınca durum DEĞİŞMEZ,
// sunucu teklifi kabul edip bid_accepted gönderince değişir.
import { reasonText } from './labels.js';

export const initialArenaState = {
  loaded: false,
  players: [], // { roomPlayerId, userId, seatNumber, username, budget, connected }
  squads: {}, // { [roomPlayerId]: { [slot]: { name, price, type } } }
  current: null, // aktif / son ihale
  processed: 0,
  total: 22,
  phase: 'waiting', // waiting | auction | tier3 | finished
  result: null, // GameResultView
  toast: null, // { id, text }
};

function applyBudgets(players, budgets) {
  if (!budgets) return players;
  return players.map((p) => {
    const b = budgets[p.roomPlayerId] ?? budgets[String(p.roomPlayerId)];
    return b === undefined ? p : { ...p, budget: b };
  });
}

function putInSquad(squads, roomPlayerId, slot, entry) {
  const squad = { ...(squads[roomPlayerId] ?? {}) };
  squad[slot] = entry;
  return { ...squads, [roomPlayerId]: squad };
}

function withToast(state, text) {
  return { ...state, toast: { id: `${Date.now()}-${Math.random()}`, text } };
}

/** Sunucu saatiyle gelen bitiş anını bu bilgisayarın saatine çevirir. */
export function toLocalDeadline(data, receivedAt) {
  if (data?.deadlineMs == null || data?.serverNowMs == null) return null;
  return receivedAt + (data.deadlineMs - data.serverNowMs);
}

function applyLive(state, live, receivedAt) {
  const players = live.players.map((p) => ({
    roomPlayerId: p.roomPlayerId,
    userId: p.userId,
    seatNumber: p.seatNumber,
    username: p.username,
    budget: p.budget,
    connected: p.connected,
  }));

  // /live sadece dolu slot adlarını verir. Önceden bildiğimiz kartları koru,
  // bilmediklerimizi (ör. sayfa yenilendiyse) "?" olarak işaretle.
  let squads = state.squads;
  for (const p of live.players) {
    for (const slot of p.filledSlots ?? []) {
      if (!squads[p.roomPlayerId]?.[slot]) {
        squads = putInSquad(squads, p.roomPlayerId, slot, { name: '?', price: null, type: 'AUCTION' });
      }
    }
  }

  const c = live.current;
  const sameAuction = c && state.current && state.current.sequenceNo === c.sequenceNo;
  const current = c
    ? {
        sequenceNo: c.sequenceNo,
        totalAuctions: live.totalAuctions,
        slot: c.slot,
        tier: c.tier,
        card: c.card,
        price: c.price,
        leaderRoomPlayerId: c.leaderRoomPlayerId,
        deadlineLocal: toLocalDeadline(c, receivedAt),
        withdrawn: sameAuction ? state.current.withdrawn : [],
        status: 'bidding',
        winnerRoomPlayerId: null,
      }
    : state.current;

  return {
    ...state,
    loaded: true,
    players,
    squads,
    current,
    processed: live.processedAuctions,
    total: live.totalAuctions,
    phase: live.finished ? 'finished' : c ? 'auction' : state.phase,
  };
}

function applyEvent(state, event, receivedAt) {
  const { type, data } = event;
  const cur = state.current;
  const isCurrent = (seq) => cur && cur.sequenceNo === seq;

  switch (type) {
    case 'game_started':
      return { ...state, total: data.totalAuctions, phase: 'waiting' };

    case 'auction_started':
      return {
        ...state,
        phase: 'auction',
        total: data.totalAuctions,
        current: {
          sequenceNo: data.sequenceNo,
          totalAuctions: data.totalAuctions,
          slot: data.slot,
          tier: data.tier,
          card: data.card,
          price: data.price,
          leaderRoomPlayerId: data.openerRoomPlayerId,
          deadlineLocal: toLocalDeadline(data, receivedAt),
          withdrawn: [],
          status: 'bidding',
          winnerRoomPlayerId: null,
        },
      };

    case 'bid_accepted':
      if (!isCurrent(data.sequenceNo)) return state;
      return {
        ...state,
        current: {
          ...cur,
          price: data.price,
          leaderRoomPlayerId: data.leaderRoomPlayerId,
          deadlineLocal: toLocalDeadline(data, receivedAt),
        },
      };

    case 'player_withdrew':
      if (!cur || cur.status !== 'bidding' || cur.withdrawn.includes(data.roomPlayerId)) return state;
      return { ...state, current: { ...cur, withdrawn: [...cur.withdrawn, data.roomPlayerId] } };

    case 'auction_closed': {
      const name = isCurrent(data.sequenceNo) ? cur.card.playerName : '?';
      const squads = data.winnerRoomPlayerId
        ? putInSquad(state.squads, data.winnerRoomPlayerId, data.slot, { name, price: data.price, type: 'AUCTION' })
        : state.squads;
      return {
        ...state,
        squads,
        players: applyBudgets(state.players, data.budgets),
        processed: Math.max(state.processed, data.sequenceNo),
        current: isCurrent(data.sequenceNo)
          ? { ...cur, status: 'sold', price: data.price, winnerRoomPlayerId: data.winnerRoomPlayerId, deadlineLocal: null }
          : cur,
      };
    }

    case 'auction_skipped':
      return {
        ...state,
        processed: Math.max(state.processed, data.sequenceNo),
        current: isCurrent(data.sequenceNo) ? { ...cur, status: 'skipped', deadlineLocal: null } : cur,
      };

    case 'auctions_finished':
      return { ...state, phase: 'tier3', players: applyBudgets(state.players, data.budgets) };

    case 'player_disconnected':
    case 'player_reconnected': {
      const connected = type === 'player_reconnected';
      return {
        ...state,
        players: state.players.map((p) => (p.roomPlayerId === data.roomPlayerId ? { ...p, connected } : p)),
      };
    }

    case 'tier3_assigned': {
      let squads = state.squads;
      for (const e of data.entries ?? []) {
        squads = putInSquad(squads, e.roomPlayerId, e.slot, { name: e.card?.playerName ?? '?', price: e.price, type: e.type });
      }
      return { ...state, squads, players: applyBudgets(state.players, data.budgets) };
    }

    case 'game_finished':
      return { ...state, phase: 'finished', result: data, current: cur ? { ...cur, deadlineLocal: null } : cur };

    default:
      return state;
  }
}

export function arenaReducer(state, action) {
  switch (action.type) {
    case 'live':
      return applyLive(state, action.data, action.receivedAt);

    case 'event':
      return applyEvent(state, action.event, action.receivedAt);

    // Sadece bana gelen ret: ekranı sunucunun söylediği fiyat ve lidere göre düzelt.
    case 'rejected': {
      const { reason, price, leaderRoomPlayerId } = action.data ?? {};
      let next = state;
      if (state.current?.status === 'bidding' && price > 0) {
        next = { ...state, current: { ...state.current, price, leaderRoomPlayerId } };
      }
      return withToast(next, reasonText(reason));
    }

    case 'toast':
      return withToast(state, action.text);

    case 'clearToast':
      return state.toast?.id === action.id ? { ...state, toast: null } : state;

    default:
      return state;
  }
}