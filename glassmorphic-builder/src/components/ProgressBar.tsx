import { useEffect, useState } from 'react';
import { RefreshCcw, Sparkles, Pickaxe } from 'lucide-react';
import { cn } from '../lib/utils';

interface ProgressBarProps {
  progress: number;
  setProgress: (val: number | ((prev: number) => number)) => void;
  isDay: boolean;
}

export const ProgressBar = ({ progress, setProgress, isDay }: ProgressBarProps) => {
  const [isPlaying, setIsPlaying] = useState(false);

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (isPlaying && progress < 100) {
      interval = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            setIsPlaying(false);
            return 100;
          }
          return prev + 1;
        });
      }, 50);
    } else if (progress >= 100) {
      setIsPlaying(false);
    }
    return () => clearInterval(interval);
  }, [isPlaying, progress, setProgress]);

  const togglePlay = () => {
    if (progress === 100) setProgress(0);
    setIsPlaying(!isPlaying);
  };

  return (
    <div className={cn(
      "glass rounded-3xl p-6 w-full max-w-md mx-auto relative overflow-hidden transition-colors duration-500",
      isDay ? "bg-white/40 border-white/50" : "bg-black/40 border-white/10"
    )}>
      <div className="flex justify-between items-center mb-6 text-sm font-semibold tracking-wide">
        <div className={cn("flex items-center gap-2", isDay ? "text-slate-600" : "text-slate-300")}>
          <Pickaxe size={16} />
          <span>Início</span>
        </div>
        <div className={cn("flex items-center gap-2", isDay ? "text-sky-600" : "text-cyan-400")}>
          <Sparkles size={16} />
          <span>Concluído</span>
        </div>
      </div>

      <div className="relative h-4 bg-black/10 rounded-full overflow-hidden backdrop-blur-sm mb-6">
        <div 
          className="absolute top-0 left-0 h-full bg-gradient-to-r from-sky-400 to-indigo-500 transition-all duration-75 ease-linear rounded-full"
          style={{ width: `${progress}%` }}
        />
        <input 
          type="range" 
          min="0" 
          max="100" 
          value={progress}
          onChange={(e) => {
            setProgress(Number(e.target.value));
            setIsPlaying(false);
          }}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
        />
      </div>

      <div className="flex justify-between items-center">
        <button
          onClick={togglePlay}
          className={cn(
            "p-3 rounded-full transition-all active:scale-95",
            isDay ? "bg-sky-500 text-white hover:bg-sky-600" : "bg-cyan-500 text-black hover:bg-cyan-400"
          )}
        >
          <RefreshCcw size={24} className={cn(isPlaying && "animate-spin")} />
        </button>
        
        <div className={cn(
          "text-4xl font-black tabular-nums tracking-tighter",
          isDay ? "text-slate-800" : "text-white"
        )}>
          {progress}%
        </div>
      </div>
    </div>
  );
};
