// Backend ile REST konuşmasının tek yeri.
// Adresler .env ile değiştirilebilir: VITE_API_URL=http://localhost:8080
export const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
export const WS_URL = import.meta.env.VITE_WS_URL ?? `${API_BASE.replace(/^http/, 'ws')}/ws`;

// Backend hataları { code, message } biçiminde gelir (docs/olay-sozlesmesi.md, "Hata biçimi").
export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function request(path, { method = 'GET', token, body } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['X-Player-Token'] = token;

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Sunucuya ulaşılamadı. Backend açık mı?');
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }
  if (!res.ok) {
    throw new ApiError(res.status, data?.code ?? `HTTP_${res.status}`, data?.message ?? `İstek başarısız (${res.status})`);
  }
  return data;
}

export const api = {
  /** { userId, username, playerToken } */
  guestLogin: (username) => request('/api/guests', { method: 'POST', body: { username } }),
  /** Açık odaya sokar, yoksa oda kurar. RoomView döner. */
  quickJoin: (token) => request('/api/rooms/quick-join', { method: 'POST', token }),
  /** LobbyView */
  getLobby: (roomId) => request(`/api/rooms/${roomId}/lobby`),
  /** Hazırım / İptal. LobbyView döner. */
  setReady: (roomId, token, ready) => request(`/api/rooms/${roomId}/ready`, { method: 'POST', token, body: { ready } }),
  /** LiveStateView (oyun sürerken) */
  getLive: (roomId) => request(`/api/rooms/${roomId}/live`),
  /** GameResultView (oyun bitince) */
  getResult: (roomId) => request(`/api/rooms/${roomId}/result`),
};