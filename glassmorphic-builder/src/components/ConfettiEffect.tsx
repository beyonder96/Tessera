import Confetti from 'react-confetti'
import { useWindowSize } from 'react-use'

export const ConfettiEffect = ({ active }: { active: boolean }) => {
  const { width, height } = useWindowSize()

  if (!active) return null

  return (
    <Confetti
      width={width}
      height={height}
      recycle={false}
      numberOfPieces={500}
      gravity={0.2}
      colors={['#38bdf8', '#818cf8', '#c084fc', '#f472b6', '#fbbf24']}
    />
  )
}
