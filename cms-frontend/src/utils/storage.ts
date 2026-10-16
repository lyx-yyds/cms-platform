/**
 * localStorage 封装（对应文档 utils/storage.ts）
 */
const PREFIX = 'cms_'

export function getStorage<T>(key: string): T | null {
  const raw = localStorage.getItem(PREFIX + key)
  if (raw === null || raw === undefined) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function setStorage<T>(key: string, value: T): void {
  localStorage.setItem(PREFIX + key, JSON.stringify(value))
}

export function removeStorage(key: string): void {
  localStorage.removeItem(PREFIX + key)
}
