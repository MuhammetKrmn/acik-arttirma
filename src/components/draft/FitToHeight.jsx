import { useEffect, useRef, useState } from 'react';

// İçerik bulunduğu alandan uzunsa orantılı küçültüp sığdırır.
// Böylece süre üstten kesilmez, hiçbir şey alt barın altında kalmaz.
export default function FitToHeight({ children }) {
  const outerRef = useRef(null);
  const innerRef = useRef(null);
  const [scale, setScale] = useState(1);

  useEffect(() => {
    const outer = outerRef.current;
    const inner = innerRef.current;
    if (!outer || !inner) return;

    const measure = () => {
      const available = outer.clientHeight - 16; // alt-üst küçük nefes payı
      const needed = inner.offsetHeight;
      setScale(needed > 0 ? Math.min(1, available / needed) : 1);
    };

    const observer = new ResizeObserver(measure);
    observer.observe(outer);
    observer.observe(inner);
    return () => observer.disconnect();
  }, []);

  return (
    <div ref={outerRef} className="flex-1 min-h-0 w-full flex items-center justify-center">
      <div
        ref={innerRef}
        className="flex flex-col items-center shrink-0"
        style={{ transform: `scale(${scale})`, transformOrigin: 'center center' }}
      >
        {children}
      </div>
    </div>
  );
}