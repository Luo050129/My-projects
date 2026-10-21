// 统一 API 层：优先请求后端 Spring Boot（/api 由 Vite 代理到 :8081），
// 任何网络错误或后端未启动时自动回退到本地模拟数据，保证页面始终可用。
import {
  mockShopTypes,
  getMockShops,
  getMockShopById,
  mockReviews,
  getMockReviewsByShop
} from './mock'

const BASE = '/api'

async function request(path) {
  const res = await fetch(BASE + path, { headers: { Accept: 'application/json' } })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const body = await res.json()
  // 后端统一返回 Result: { success, msg, data }
  if (body && body.success === false) throw new Error(body.msg || '业务失败')
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body
}

export async function fetchShopTypes() {
  try {
    return await request('/shop-type/list')
  } catch (e) {
    console.warn('[api] /shop-type/list 回退模拟数据:', e.message)
    return mockShopTypes
  }
}

export async function fetchShops({ typeId, name, current = 1 } = {}) {
  const params = new URLSearchParams()
  if (typeId) params.set('typeId', typeId)
  if (name) params.set('name', name)
  if (current) params.set('current', current)
  try {
    const path = '/shop/of/type' + (params.toString() ? '?' + params.toString() : '')
    return await request(path)
  } catch (e) {
    console.warn('[api] /shop/of/type 回退模拟数据:', e.message)
    return getMockShops({ typeId, name })
  }
}

export async function fetchShopById(id) {
  try {
    return await request('/shop/' + id)
  } catch (e) {
    console.warn('[api] /shop/' + id + ' 回退模拟数据:', e.message)
    return getMockShopById(id)
  }
}

export async function fetchHotReviews() {
  try {
    return await request('/blog/hot')
  } catch (e) {
    console.warn('[api] /blog/hot 回退模拟数据:', e.message)
    return mockReviews
  }
}

export async function fetchReviewsByShop(shopId) {
  try {
    const list = await request('/blog/hot')
    return Array.isArray(list) ? list.filter((r) => String(r.shopId) === String(shopId)) : []
  } catch (e) {
    console.warn('[api] 商户点评回退模拟数据:', e.message)
    return getMockReviewsByShop(shopId)
  }
}
