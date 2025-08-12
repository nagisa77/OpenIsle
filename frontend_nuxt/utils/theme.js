import { reactive } from 'vue'
import { toast } from '~/main'

export const ThemeMode = {
  SYSTEM: 'system',
  LIGHT: 'light',
  DARK: 'dark',
}

const THEME_KEY = 'theme-mode'

export const themeState = reactive({
  mode: ThemeMode.SYSTEM,
})

function apply(mode) {
  if (!process.client) return
  const root = document.documentElement
  if (mode === ThemeMode.SYSTEM) {
    root.dataset.theme = window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light'
  } else {
    root.dataset.theme = mode
  }
}

export function initTheme() {
  if (!process.client) return
  const saved = localStorage.getItem(THEME_KEY)
  if (saved && Object.values(ThemeMode).includes(saved)) {
    themeState.mode = saved
  }
  apply(themeState.mode)
}

export function setTheme(mode) {
  if (!process.client) return
  if (!Object.values(ThemeMode).includes(mode)) return
  themeState.mode = mode
  localStorage.setItem(THEME_KEY, mode)
  apply(mode)
}

export function cycleTheme() {
  if (!process.client) return
  const modes = [ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK]
  const index = modes.indexOf(themeState.mode)
  const next = modes[(index + 1) % modes.length]
  if (next === ThemeMode.SYSTEM) {
    toast.success('💻 已经切换到系统主题')
  } else if (next === ThemeMode.LIGHT) {
    toast.success('🌞 已经切换到明亮主题')
  } else {
    toast.success('🌙 已经切换到暗色主题')
  }
  setTheme(next)
}

if (process.client && window.matchMedia) {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (themeState.mode === ThemeMode.SYSTEM) {
      apply(ThemeMode.SYSTEM)
    }
  })
}
