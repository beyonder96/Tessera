import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { Leaf, Snowflake } from 'lucide-react';
import { useWindowSize } from 'react-use';

export const Particles = ({ isDay }: { isDay: boolean }) => {
  const { width, height } = useWindowSize();

  const particles = useMemo(() => {
    return Array.from({ length: 25 }).map((_, i) => ({
      id: i,
      startX: Math.random() * (width || 1000),
      size: Math.random() * 10 + 10,
      duration: Math.random() * 10 + 10,
      delay: Math.random() * -20,
    }));
  }, [width]);

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none">
      {particles.map((p) => (
        <motion.div
          key={p.id}
          className="absolute top-0"
          initial={{ x: p.startX, y: -50, rotate: 0 }}
          animate={{
            y: (height || 800) + 50,
            x: p.startX + (Math.random() * 100 - 50),
            rotate: 360,
          }}
          transition={{
            duration: p.duration,
            repeat: Infinity,
            delay: p.delay,
            ease: 'linear',
          }}
        >
          {isDay ? (
            <Leaf size={p.size} className="text-green-500/20" />
          ) : (
            <Snowflake size={p.size} className="text-blue-300/20" />
          )}
        </motion.div>
      ))}
    </div>
  );
};
