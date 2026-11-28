// Misafir oturumu ve devam eden oda localStorage'da tutulur.
// Not: aynı tarayıcının sekmeleri localStorage'ı paylaşır. 3 oyuncuyla denerken
// normal pencere, gizli pencere ve başka bir tarayıcı kullanın.
const SESSION_KEY = 'acikArttirma.session';
const ROOM_KEY = 'acikArttirma.activeRoomId';

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    if (value === null || value === undefined) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    // Gizli modda veya depolama kapalıysa sessizce geç: oturum sadece bellekte kalır.
  }
}

/** { userId, username, playerToken } veya null */
export function loadSession() {
  const raw = read(SESSION_KEY);
  if (!raw) return null;
  try {
    const s = JSON.parse(raw);
    return s && s.playerToken && s.userId ? s : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  write(SESSION_KEY, session ? JSON.stringify(session) : null);
}

export function loadActiveRoomId() {
  const raw = read(ROOM_KEY);
  return raw ? Number(raw) : null;
}

export function saveActiveRoomId(roomId) {
  write(ROOM_KEY, roomId ? String(roomId) : null);
}