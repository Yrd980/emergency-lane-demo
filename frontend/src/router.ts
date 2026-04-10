export type AppRoute = 'overview' | 'events' | 'cases'

const ROUTES: AppRoute[] = ['overview', 'events', 'cases']

function normalizeRoute(value: string | null | undefined): AppRoute {
  return ROUTES.find((route) => route === value) ?? 'overview'
}

export function getRouteFromHash(hash: string = window.location.hash): AppRoute {
  const value = hash.replace(/^#\/?/, '').split(/[/?]/)[0]
  return normalizeRoute(value)
}

export function setRouteHash(route: AppRoute) {
  const nextHash = `#/${route}`
  if (window.location.hash === nextHash) return
  window.location.hash = nextHash
}
