import { reactive } from 'vue'

const TOKEN_KEY = 'zhidu_token'
const USER_KEY = 'zhidu_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

/** 极简登录态：token 存 localStorage，页面刷新不丢 */
export const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser()
})

export function isLoggedIn() {
  return Boolean(auth.token)
}

export function signIn(resp) {
  auth.token = resp.token
  auth.user = { userId: resp.userId, nickname: resp.nickname, role: resp.role }
  localStorage.setItem(TOKEN_KEY, resp.token)
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
}

export function signOut() {
  auth.token = ''
  auth.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
