export type ThemeKey = 'night-command' | 'amber-alert' | 'jade-ops'

export type ThemeDefinition = {
  key: ThemeKey
  label: string
  description: string
  vars: Record<string, string>
}

export const THEME_STORAGE_KEY = 'emergency-lane-theme'

export const THEMES: ThemeDefinition[] = [
  {
    key: 'night-command',
    label: '夜航指挥',
    description: '极简暗色',
    vars: {
      '--bg-base': '#0a0a0a',
      '--bg-layer': '#141414',
      '--bg-panel': 'transparent',
      '--bg-panel-strong': 'transparent',
      '--bg-hero': '#0a0a0a',
      '--text-primary': '#ffffff',
      '--text-secondary': '#a1a1aa',
      '--text-muted': '#71717a',
      '--accent': '#ffffff',
      '--accent-strong': '#ffffff',
      '--accent-soft': 'rgba(255, 255, 255, 0.1)',
      '--border-soft': 'rgba(255, 255, 255, 0.1)',
      '--shadow-color': 'transparent',
      '--success': '#10b981',
      '--warning': '#f59e0b',
      '--danger': '#ef4444',
    },
  }
]

export function isThemeKey(value: string): value is ThemeKey {
  return THEMES.some((theme) => theme.key === value)
}

export function getTheme(themeKey: ThemeKey): ThemeDefinition {
  return THEMES.find((theme) => theme.key === themeKey) ?? THEMES[0]
}

export function getInitialTheme(): ThemeKey {
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
  return stored && isThemeKey(stored) ? stored : 'night-command'
}

export function applyTheme(themeKey: ThemeKey) {
  const theme = getTheme(themeKey)
  document.documentElement.dataset.theme = theme.key
  Object.entries(theme.vars).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value)
  })
  window.localStorage.setItem(THEME_STORAGE_KEY, theme.key)
}
