export const LEVEL_EXP = [100, 200, 300, 600, 1200, 10000]

export const prevLevelExp = level => {
  if (level <= 0) return 0
  if (level - 1 < LEVEL_EXP.length) return LEVEL_EXP[level - 1]
  return LEVEL_EXP[LEVEL_EXP.length - 1]
}
