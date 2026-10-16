/**
 * CMS 认证接口 Mock 后端（联调用，零依赖 Node 原生 http）
 *
 * 用途：后端 Spring Boot 开发完成前，提供与文档 §5.1 一致的认证接口契约，
 *       供 cms-frontend 真实请求链路联调（登录/刷新/退出/用户信息 + 401 场景）。
 * 后端就绪后：停止本服务（npm run mock:stop 或 Ctrl+C），前端自动切到真实后端。
 *
 * 启动：node mock-server/server.mjs   （监听 8080）
 */
import http from 'node:http'
import crypto from 'node:crypto'

const PORT = 8080

/** 内存用户表：联调默认账号 admin / admin123 */
const users = {
  admin: { id: 1, username: 'admin', nickname: '主编', roles: ['ADMIN'] }
}

/** 签发假令牌（mock 不校验签名，仅用于链路联调） */
function signToken(payload) {
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url')
  const sig = crypto.createHash('sha256').update(body + 'cms-mock').digest('hex').slice(0, 16)
  return `${body}.${sig}`
}

function parseToken(token) {
  try {
    const body = token.split('.')[0]
    return JSON.parse(Buffer.from(body, 'base64url').toString('utf8'))
  } catch {
    return null
  }
}

function json(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(body))
}

function readBody(req) {
  return new Promise((resolve) => {
    let raw = ''
    req.on('data', (chunk) => (raw += chunk))
    req.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {})
      } catch {
        resolve({})
      }
    })
  })
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`)
  const path = url.pathname
  const method = req.method

  // ---------- POST /api/auth/login ----------
  if (method === 'POST' && path === '/api/auth/login') {
    const body = await readBody(req)
    if (!body.username || !body.password) {
      return json(res, 400, { code: 400, message: '用户名和密码不能为空' })
    }
    if (!users[body.username] || body.password !== 'admin123') {
      return json(res, 401, { code: 401, message: '用户名或密码错误' })
    }
    const user = users[body.username]
    return json(res, 200, {
      code: 200,
      message: 'ok',
      data: {
        accessToken: signToken({ sub: user.id, type: 'access', exp: Date.now() + 3600_000 }),
        refreshToken: signToken({ sub: user.id, type: 'refresh', exp: Date.now() + 7 * 24 * 3600_000 }),
        tokenType: 'Bearer',
        expiresIn: 3600
      }
    })
  }

  // ---------- POST /api/auth/refresh ----------
  if (method === 'POST' && path === '/api/auth/refresh') {
    const body = await readBody(req)
    if (!body.refreshToken) {
      return json(res, 400, { code: 400, message: '缺少 refreshToken' })
    }
    const payload = parseToken(body.refreshToken)
    if (!payload || payload.type !== 'refresh') {
      return json(res, 401, { code: 401, message: 'refreshToken 无效' })
    }
    return json(res, 200, {
      code: 200,
      message: 'ok',
      data: {
        accessToken: signToken({ sub: payload.sub, type: 'access', exp: Date.now() + 3600_000 }),
        refreshToken: body.refreshToken,
        tokenType: 'Bearer',
        expiresIn: 3600
      }
    })
  }

  // ---------- POST /api/auth/logout ----------
  if (method === 'POST' && path === '/api/auth/logout') {
    return json(res, 200, { code: 200, message: 'ok', data: null })
  }

  // ---------- GET /api/user/info（401 场景可测） ----------
  if (method === 'GET' && path === '/api/user/info') {
    const auth = req.headers.authorization || ''
    if (!auth.startsWith('Bearer ')) {
      return json(res, 401, { code: 401, message: '未登录' })
    }
    const payload = parseToken(auth.slice(7))
    if (!payload || payload.type !== 'access') {
      return json(res, 401, { code: 401, message: 'AccessToken 无效或已过期' })
    }
    const user = users.admin
    return json(res, 200, {
      code: 200,
      message: 'ok',
      data: { ...user, avatar: '', email: 'admin@cms.local', permissions: ['*'] }
    })
  }

  // ---------- 其余：404 ----------
  return json(res, 404, { code: 404, message: `Not Found: ${method} ${path}` })
})

server.listen(PORT, () => {
  console.log(`[cms-mock] 认证接口 Mock 服务已启动: http://localhost:${PORT}`)
  console.log('[cms-mock] 账号: admin / admin123')
  console.log('[cms-mock] 接口: /api/auth/login | /api/auth/refresh | /api/auth/logout | /api/user/info')
})
