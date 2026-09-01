import { motion } from 'framer-motion';

export const Building = ({ progress, isDay }: { progress: number, isDay: boolean }) => {
  // Cálculos Matemáticos para as alturas e animações baseados na barra (0 a 100)
  const foundationHeight = Math.min(progress * 2, 20); // 0 a 10%
  const coreHeight = Math.max(0, Math.min((progress - 10) * 4, 200)); // 10% a 60%
  const glassHeight = Math.max(0, Math.min((progress - 30) * 3.5, 240)); // 30% a 100%
  const antennaHeight = Math.max(0, Math.min((progress - 80) * 2, 40)); // 80% a 100%

  return (
    <div className="relative w-full max-w-sm mx-auto h-[400px] flex items-end justify-center">
      {/* Luzes de Fundo (Orbes em Blur) */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <motion.div 
          className="absolute w-64 h-64 rounded-full mix-blend-screen filter blur-[80px] opacity-60"
          animate={{
            backgroundColor: isDay ? '#38bdf8' : '#818cf8',
            scale: 1 + (progress / 100) * 0.5,
          }}
          transition={{ duration: 0.5 }}
        />
        <motion.div 
          className="absolute w-48 h-48 rounded-full mix-blend-screen filter blur-[60px] opacity-40 translate-x-10 translate-y-10"
          animate={{
            backgroundColor: isDay ? '#fbbf24' : '#c084fc',
            scale: 1 + (progress / 100) * 0.3,
          }}
          transition={{ duration: 0.5 }}
        />
      </div>

      <svg width="300" height="350" viewBox="0 0 300 350" className="relative z-10 drop-shadow-2xl overflow-visible">
        <defs>
          <linearGradient id="glassGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="rgba(255, 255, 255, 0.5)" />
            <stop offset="100%" stopColor="rgba(255, 255, 255, 0.1)" />
          </linearGradient>
          <linearGradient id="coreGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={isDay ? "#64748b" : "#334155"} />
            <stop offset="100%" stopColor={isDay ? "#334155" : "#0f172a"} />
          </linearGradient>
          <filter id="glow">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
        </defs>

        {/* Antena */}
        <g transform={`translate(150, ${320 - foundationHeight - coreHeight})`}>
          <rect x="-2" y={-antennaHeight} width="4" height={antennaHeight} fill="#94a3b8" />
          {antennaHeight > 38 && (
            <circle cx="0" cy={-antennaHeight} r="4" fill="#ef4444" filter="url(#glow)">
              <animate attributeName="opacity" values="1;0.5;1" dur="2s" repeatCount="indefinite" />
            </circle>
          )}
        </g>

        {/* Core (Núcleo do prédio) */}
        <rect 
          x="120" 
          y={320 - foundationHeight - coreHeight} 
          width="60" 
          height={coreHeight} 
          fill="url(#coreGrad)" 
        />
        
        {/* Linhas do Core para dar textura */}
        <g opacity="0.2">
          {Array.from({ length: 10 }).map((_, i) => (
            <line 
              key={i}
              x1="120" y1={320 - foundationHeight - (coreHeight / 10) * i} 
              x2="180" y2={320 - foundationHeight - (coreHeight / 10) * i} 
              stroke="#fff" strokeWidth="1"
            />
          ))}
        </g>

        {/* Fachada de Vidro (Glassmorphism) */}
        <g transform={`translate(0, ${320 - foundationHeight - glassHeight})`}>
          {/* Main Glass block */}
          <rect 
            x="80" y="0" width="140" height={glassHeight} 
            fill="url(#glassGrad)" 
            stroke="rgba(255,255,255,0.4)" strokeWidth="2"
            rx="8"
            className="backdrop-blur-md"
            style={{ backdropFilter: 'blur(12px)' }}
          />
          {/* Reflexo / Highligh edge */}
          <line x1="82" y1="2" x2="218" y2="2" stroke="#fff" strokeWidth="2" opacity="0.6" />
          
          {/* Side glass wings */}
          <rect x="60" y={glassHeight * 0.1} width="20" height={glassHeight * 0.9} fill="url(#glassGrad)" stroke="rgba(255,255,255,0.3)" strokeWidth="1" rx="4" />
          <rect x="220" y={glassHeight * 0.1} width="20" height={glassHeight * 0.9} fill="url(#glassGrad)" stroke="rgba(255,255,255,0.3)" strokeWidth="1" rx="4" />
          
          {/* Glass Grid */}
          <g opacity="0.3">
            {Array.from({ length: 5 }).map((_, i) => (
              <line key={`v-${i}`} x1={80 + (140/6)*(i+1)} y1="0" x2={80 + (140/6)*(i+1)} y2={glassHeight} stroke="#fff" strokeWidth="1" />
            ))}
            {Array.from({ length: 10 }).map((_, i) => (
               <line key={`h-${i}`} x1="80" y1={(glassHeight/11)*(i+1)} x2="220" y2={(glassHeight/11)*(i+1)} stroke="#fff" strokeWidth="1" />
            ))}
          </g>
        </g>

        {/* Fundação */}
        <rect 
          x="70" 
          y={320 - foundationHeight} 
          width="160" 
          height={foundationHeight} 
          fill={isDay ? "#475569" : "#1e293b"}
          stroke="rgba(255,255,255,0.2)" strokeWidth="1"
          rx="4"
        />

        {/* Textos Flutuantes Indicadores */}
        <motion.text 
          x="50" y="325" fill={isDay ? "#475569" : "#94a3b8"} fontSize="12" fontWeight="bold"
          initial={{ opacity: 0, x: 40 }}
          animate={{ opacity: progress > 5 ? 1 : 0, x: progress > 5 ? 50 : 40 }}
        >
          Fundação
        </motion.text>
        <motion.text 
          x="40" y="150" fill={isDay ? "#0ea5e9" : "#38bdf8"} fontSize="12" fontWeight="bold"
          initial={{ opacity: 0, x: 30 }}
          animate={{ opacity: progress > 40 ? 1 : 0, x: progress > 40 ? 40 : 30 }}
        >
          Fachada de Vidro
        </motion.text>

      </svg>
    </div>
  );
};
