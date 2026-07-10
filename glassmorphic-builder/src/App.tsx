import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Sun, Moon, Building2 } from 'lucide-react';
import { Building } from './components/Building';
import { ProgressBar } from './components/ProgressBar';
import { Particles } from './components/Particles';
import { ConfettiEffect } from './components/ConfettiEffect';
import { cn } from './lib/utils';

function App() {
  const [isDay, setIsDay] = useState(true);
  const [progress, setProgress] = useState(0);

  return (
    <div className="relative min-h-screen w-full overflow-hidden transition-colors duration-1000 select-none">
      {/* Gradientes de Fundo Cruzados */}
      <div 
        className={cn(
          "absolute inset-0 transition-opacity duration-1000",
          isDay ? "opacity-100" : "opacity-0"
        )}
        style={{ background: 'linear-gradient(to bottom, #7dd3fc, #e0f2fe)' }}
      />
      <div 
        className={cn(
          "absolute inset-0 transition-opacity duration-1000",
          !isDay ? "opacity-100" : "opacity-0"
        )}
        style={{ background: 'linear-gradient(to bottom, #0f172a, #1e1b4b)' }}
      />

      <Particles isDay={isDay} />
      <ConfettiEffect active={progress === 100} />

      {/* Header */}
      <header className="relative z-20 flex justify-between items-center p-6 md:p-8">
        <div className="flex items-center gap-3">
          <Building2 size={32} className={isDay ? "text-sky-600" : "text-cyan-400"} />
          <h1 className={cn("text-2xl font-bold", isDay ? "text-slate-800" : "text-white")}>
            Construtor <span className={isDay ? "text-sky-500" : "text-cyan-400"}>Glassmorphic</span>
          </h1>
        </div>

        {/* Botão de Toggle */}
        <button 
          onClick={() => setIsDay(!isDay)}
          className={cn(
            "p-3 rounded-full backdrop-blur-md border transition-all duration-300 hover:scale-105 active:scale-95",
            isDay ? "bg-white/30 border-white/50 text-amber-500" : "bg-black/30 border-white/10 text-blue-300"
          )}
        >
          {isDay ? <Sun size={24} /> : <Moon size={24} />}
        </button>
      </header>

      {/* Astro Animatrônico (Sol / Lua) */}
      <div className="absolute top-20 right-20 z-10 pointer-events-none">
        <AnimatePresence mode="popLayout">
          {isDay ? (
            <motion.div
              key="sun"
              initial={{ y: 50, scale: 0.5, opacity: 0 }}
              animate={{ y: 0, scale: 1, opacity: 1 }}
              exit={{ y: 50, scale: 0.5, opacity: 0 }}
              transition={{ type: "spring", stiffness: 100, damping: 15 }}
              className="w-24 h-24 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-200 shadow-[0_0_60px_rgba(251,191,36,0.6)]"
            />
          ) : (
            <motion.div
              key="moon"
              initial={{ y: -50, scale: 0.5, opacity: 0 }}
              animate={{ y: 0, scale: 1, opacity: 1 }}
              exit={{ y: -50, scale: 0.5, opacity: 0 }}
              transition={{ type: "spring", stiffness: 100, damping: 15 }}
              className="w-20 h-20 rounded-full bg-slate-200 shadow-[0_0_40px_rgba(226,232,240,0.4)] relative overflow-hidden"
            >
              {/* Crateras da lua */}
              <div className="absolute top-4 left-4 w-4 h-4 rounded-full bg-slate-300 opacity-50" />
              <div className="absolute top-10 left-10 w-6 h-6 rounded-full bg-slate-300 opacity-50" />
              <div className="absolute top-12 left-4 w-3 h-3 rounded-full bg-slate-300 opacity-50" />
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <main className="relative z-20 flex flex-col items-center justify-center min-h-[calc(100vh-120px)] p-6">
        <p className={cn("mb-8 text-center", isDay ? "text-slate-600" : "text-slate-400")}>
          Arraste a barra para construir a estrutura.
        </p>

        {/* Display do Prédio (Glass Card) */}
        <div className={cn(
          "w-full max-w-lg rounded-3xl p-8 mb-12 backdrop-blur-xl border transition-colors duration-500",
          isDay ? "bg-white/20 border-white/40 shadow-xl" : "bg-white/5 border-white/10 shadow-2xl"
        )}>
          <Building progress={progress} isDay={isDay} />
        </div>

        <ProgressBar progress={progress} setProgress={setProgress} isDay={isDay} />
      </main>
    </div>
  );
}

export default App;
