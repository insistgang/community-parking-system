/**
 * 运行环境判断工具
 * - demo 环境：本地开发调试模式 (npm run dev) 或显式指定 VITE_APP_ENV=demo / ?env=demo
 * - 正式生产环境：生产打包环境 (npm run build) 或显式指定 VITE_APP_ENV=production / ?env=prod
 */

export function isProdEnv() {
  if (typeof window !== 'undefined' && window.location) {
    const params = new URLSearchParams(window.location.search)
    const envParam = params.get('env')
    if (envParam === 'prod' || envParam === 'production') return true
    if (envParam === 'demo' || envParam === 'dev') return false

    const localEnv = localStorage.getItem('APP_ENV')
    if (localEnv === 'production' || localEnv === 'prod') return true
    if (localEnv === 'demo') return false
  }

  const viteEnv = import.meta.env.VITE_APP_ENV
  if (viteEnv === 'production' || viteEnv === 'prod') return true
  if (viteEnv === 'demo' || viteEnv === 'dev') return false

  // 生产构建默认视为正式环境，开发模式默认视为 demo 环境
  return Boolean(import.meta.env.PROD)
}

export function getEnvMode() {
  return isProdEnv() ? 'production' : 'demo'
}
