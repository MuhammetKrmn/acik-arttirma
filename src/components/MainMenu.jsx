import React, { useState } from 'react';

const gameModes = [
  { id: 1, title: "DÜNYA KUPASI", gradient: "linear-gradient(to bottom right, #2563eb, #1e3a8a)" },
  { id: 2, title: "ŞAMPİYONLAR LİGİ", gradient: "linear-gradient(to bottom right, #4f46e5, #581c87)" },
  { id: 3, title: "PREMİER LİG", gradient: "linear-gradient(to bottom right, #dc2626, #7f1d1d)" },
  { id: 4, title: "SÜPER LİG", gradient: "linear-gradient(to bottom right, #059669, #064e3b)" },
];

export default function MainMenu({ onPlayStart }) {
  const [activeMode, setActiveMode] = useState(1);
  const [difficulty, setDifficulty] = useState("Orta");

  const handlePrev = () => setActiveMode((prev) => (prev === 0 ? gameModes.length - 1 : prev - 1));
  const handleNext = () => setActiveMode((prev) => (prev === gameModes.length - 1 ? 0 : prev + 1));

  return (
    <div className="min-h-screen bg-slate-900 text-white font-sans overflow-hidden flex flex-col relative select-none animate-fade-in">
      <div className="absolute top-6 right-8 flex flex-col items-end gap-3 z-50">
        <div className="flex items-center gap-2 bg-slate-800/80 px-4 py-2 rounded-full border border-slate-700 backdrop-blur-sm shadow-lg">
          <svg className="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
          <span className="font-bold tracking-wide">Camsat_123</span>
        </div>
        
        <div className="flex items-center gap-3 bg-slate-800/80 pl-3 pr-4 py-2 rounded-full border border-slate-700 backdrop-blur-sm shadow-lg w-64">
          <svg className="w-6 h-6" style={{ color: "#facc15" }} fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"></path></svg>
          <span className="font-black text-lg w-12 text-center" style={{ color: "#facc15" }}>3131</span>
          <div className="flex-1 h-3 bg-slate-900 rounded-full overflow-hidden border border-slate-700">
            <div className="h-full w-[75%] rounded-full shadow-[0_0_10px_rgba(234,179,8,0.5)]" style={{ background: "linear-gradient(to right, #eab308, #fde047)" }}></div>
          </div>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center gap-4 px-10 relative mt-8">
        <button onClick={handlePrev} className="z-20 p-4 rounded-full bg-slate-800/50 hover:bg-slate-700 transition-all border border-slate-600 hover:scale-110">
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M15 19l-7-7 7-7"></path></svg>
        </button>

        <div className="flex items-center justify-center w-[800px] h-[520px] relative perspective-1000">
          {gameModes.map((mode, index) => {
            let position = "hidden";
            let zIndex = 0;
            let transform = "";
            let opacity = "opacity-0";

            if (index === activeMode) {
              position = "relative";
              zIndex = 30;
              transform = "scale-105 translate-x-0";
              opacity = "opacity-100 shadow-[0_0_50px_rgba(79,70,229,0.4)]";
            } else if (index === (activeMode - 1 + gameModes.length) % gameModes.length) {
              position = "absolute left-0";
              zIndex = 20;
              transform = "scale-75 -translate-x-[40%]";
              opacity = "opacity-40 hover:opacity-60";
            } else if (index === (activeMode + 1) % gameModes.length) {
              position = "absolute right-0";
              zIndex = 20;
              transform = "scale-75 translate-x-[40%]";
              opacity = "opacity-40 hover:opacity-60";
            }

            return (
              <div key={mode.id} style={{ background: mode.gradient }} className={`w-80 h-[460px] rounded-3xl transition-all duration-500 ease-out flex flex-col items-center p-6 border border-white/20 ${position} ${zIndex} ${transform} ${opacity}`}>
                <div className="flex-1 flex flex-col items-center justify-center text-center mt-4">
                  <h2 className="text-4xl font-black uppercase tracking-wider mb-2 drop-shadow-lg text-white">{mode.title}</h2>
                </div>

                {index === activeMode && (
                  <div className="w-full flex flex-col gap-3 mt-auto">
                    <div className="bg-black/30 rounded-2xl p-3 backdrop-blur-sm">
                      <p className="text-center text-xs font-bold text-white/70 mb-2 uppercase tracking-wider">Zorluk Seviyesi</p>
                      <div className="flex justify-between gap-2">
                        {["Kolay", "Orta", "Zor"].map((level) => (
                          <button key={level} onClick={() => setDifficulty(level)} className={`flex-1 py-1.5 rounded-xl font-bold text-sm transition-all ${difficulty === level ? "bg-white text-slate-900 shadow-[0_0_15px_rgba(255,255,255,0.5)] scale-105" : "bg-black/40 text-white/70 hover:bg-black/60"}`}>
                            {level}
                          </button>
                        ))}
                      </div>
                    </div>
                    <button onClick={() => onPlayStart(mode.title, difficulty)} className="w-full bg-slate-900 text-white font-black text-xl py-4 rounded-2xl hover:bg-slate-800 hover:scale-105 transition-all shadow-xl uppercase tracking-widest border border-white/10">
                      Oyuna Gir
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <button onClick={handleNext} className="z-20 p-4 rounded-full bg-slate-800/50 hover:bg-slate-700 transition-all border border-slate-600 hover:scale-110">
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M9 5l7 7-7 7"></path></svg>
        </button>
      </div>

      <div className="relative h-24 flex items-center justify-center mb-4">
        <div className="flex gap-3">
          {gameModes.map((_, idx) => (
            <div key={idx} className={`h-2.5 rounded-full transition-all duration-300 ${activeMode === idx ? "w-8 bg-white shadow-[0_0_8px_white]" : "w-2.5 bg-slate-600"}`}></div>
          ))}
        </div>
      </div>
    </div>
  );
}