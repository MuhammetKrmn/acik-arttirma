import { CHAT_BTN, CHAT_H, CHAT_W, EASE, QUICK_REPLIES } from '../../constants/draft';

// Sağ üstteki hızlı mesaj balonu: tıklayınca aşağıya doğru büyür.
export default function QuickChat({ open, onToggle, onClose, onSend }) {
  return (
    <div className={`absolute top-6 right-6 ${open ? 'z-40' : 'z-30'}`}>
      {/* Boyutu animasyonla değişen kutu */}
      <div
        className="relative"
        style={{
          width: open ? CHAT_W : CHAT_BTN,
          height: open ? CHAT_H : CHAT_BTN,
          transition: `width 450ms ${EASE}, height 450ms ${EASE}`,
        }}
      >
        <div
          className={`absolute inset-0 overflow-hidden border backdrop-blur-sm ${
            open
              ? 'bg-slate-900 border-emerald-500/70 shadow-[0_20px_60px_rgba(0,0,0,0.85)]'
              : 'bg-slate-900/80 border-emerald-500/40 shadow-md hover:border-emerald-400'
          }`}
          style={{ borderRadius: 24, transition: 'background-color 300ms, border-color 300ms, box-shadow 300ms' }}
        >
          {/* KATMAN 1: Kapalıyken sohbet ikonu */}
          <button
            onClick={onToggle}
            inert={open}
            aria-hidden={open}
            aria-label="Hızlı mesaj aç"
            className="absolute top-0 right-0 flex items-center justify-center text-emerald-300"
            style={{
              width: CHAT_BTN,
              height: CHAT_BTN,
              opacity: open ? 0 : 1,
              transform: open ? 'scale(0.8)' : 'scale(1)',
              transition: open
                ? 'opacity 120ms ease, transform 200ms ease'
                : 'opacity 250ms ease 250ms, transform 300ms ease 250ms',
            }}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
              <path d="M21 12a8 8 0 0 1-11.5 7.2L4 20.5l1.3-4.6A8 8 0 1 1 21 12z" />
              <circle cx="8.5" cy="12" r="0.6" fill="currentColor" />
              <circle cx="12" cy="12" r="0.6" fill="currentColor" />
              <circle cx="15.5" cy="12" r="0.6" fill="currentColor" />
            </svg>
          </button>

          {/* KATMAN 2: Açıkken hızlı cevaplar */}
          <div
            inert={!open}
            aria-hidden={!open}
            className="absolute top-0 right-0 w-[254px] p-3 flex flex-col gap-2"
            style={{
              opacity: open ? 1 : 0,
              transform: open ? 'translateY(0)' : 'translateY(-8px)',
              transition: open
                ? 'opacity 300ms ease 180ms, transform 350ms ease 180ms'
                : 'opacity 120ms ease, transform 150ms ease',
            }}
          >
            <div className="flex items-center justify-between px-1">
              <span className="text-[11px] font-bold uppercase tracking-widest text-emerald-400">Hızlı Mesaj</span>
              <button
                onClick={onClose}
                aria-label="Sohbeti kapat"
                className="text-slate-400 hover:text-white font-bold text-sm px-1"
              >
                ✕
              </button>
            </div>

            <div className="flex flex-col gap-1.5">
              {QUICK_REPLIES.map((text) => (
                <button
                  key={text}
                  onClick={() => onSend(text)}
                  className="w-full text-left px-3 py-2 rounded-xl bg-slate-800 hover:bg-emerald-700/70 border border-slate-700 hover:border-emerald-400 text-sm font-bold text-slate-100 transition-colors active:scale-[0.98]"
                >
                  {text}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}