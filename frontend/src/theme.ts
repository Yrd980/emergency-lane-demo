export type ThemeKey = 'android-sync'

export type ThemeDefinition = {
  key: ThemeKey
  label: string
  description: string
  vars: Record<string, string>
}

export const THEME_STORAGE_KEY = 'emergency-lane-theme'

export const THEMES: ThemeDefinition[] = [
  {
    key: 'android-sync',
    label: '移动协同',
    description: '与 Android 端一致的浅紫协同主题',
    vars: {
      '--bg-base': '#f4f0fb',
      '--bg-base-rgb': '244, 240, 251',
      '--bg-layer': '#f8f4ff',
      '--bg-layer-rgb': '248, 244, 255',
      '--bg-panel': 'rgba(248, 244, 255, 0.88)',
      '--bg-panel-strong': '#f1eafe',
      '--bg-panel-soft': 'rgba(255, 255, 255, 0.68)',
      '--bg-hero': 'linear-gradient(135deg, rgba(159, 134, 255, 0.15), rgba(124, 157, 255, 0.12))',
      '--text-primary': '#2f1f46',
      '--text-secondary': '#7a6e92',
      '--text-muted': '#988cac',
      '--accent': '#9f86ff',
      '--accent-strong': '#6f58d9',
      '--accent-blue': '#7c9dff',
      '--accent-soft': 'rgba(159, 134, 255, 0.14)',
      '--accent-soft-strong': 'rgba(159, 134, 255, 0.22)',
      '--border-soft': '#e4dbf5',
      '--border-strong': 'rgba(111, 88, 217, 0.22)',
      '--shadow-color': 'rgba(111, 88, 217, 0.12)',
      '--success': '#48a57c',
      '--warning': '#d58a4b',
      '--danger': '#d76b8f',
    },
  },
]

export function isThemeKey(value: string): value is ThemeKey {
  return THEMES.some((theme) => theme.key === value)
}

export function getTheme(themeKey: ThemeKey): ThemeDefinition {
  return THEMES.find((theme) => theme.key === themeKey) ?? THEMES[0]
}

export function getInitialTheme(): ThemeKey {
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
  return stored && isThemeKey(stored) ? stored : 'android-sync'
}

export function applyTheme(themeKey: ThemeKey) {
  const theme = getTheme(themeKey)
  document.documentElement.dataset.theme = theme.key
  Object.entries(theme.vars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value)
  })
  window.localStorage.setItem(THEME_STORAGE_KEY, theme.key)
}
