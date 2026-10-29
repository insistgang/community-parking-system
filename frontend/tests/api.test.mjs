import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { runInNewContext } from 'node:vm'
import test from 'node:test'

const sourcePath = process.env.PARKING_TEST_API_SOURCE || fileURLToPath(new URL('../src/api/index.js', import.meta.url))

function loadApi() {
  const handlers = {}
  const stored = new Map([['parking_token', 'signed-session'], ['parking_user', '{"id":4}']])
  const location = { pathname: '/', replace(path) { this.redirectedTo = path } }
  const axios = {
    create: () => ({ interceptors: {
      request: { use: handler => { handlers.request = handler } },
      response: { use: (success, failure) => { handlers.failure = failure } }
    } })
  }
  const source = readFileSync(sourcePath, 'utf8').replace("import axios from 'axios'", '').replace('export default api', '')
  runInNewContext(source, {
    axios, localStorage: { getItem: key => stored.get(key), removeItem: key => stored.delete(key) },
    window: { location }, console, Promise, Error
  })
  return { handlers, stored, location }
}

test('requests authenticate with the session token and send no caller-selected user id', () => {
  const { handlers } = loadApi()
  const config = handlers.request({ headers: {} })
  assert.equal(config.headers.Authorization, 'Bearer signed-session')
  assert.equal(config.headers['X-User-Id'], undefined)
})

test('expired sessions clear both stored credentials and return to login', async () => {
  const { handlers, stored, location } = loadApi()
  await assert.rejects(handlers.failure({ response: { status: 401, data: { detail: 'expired' } } }), /expired/)
  assert.equal(stored.has('parking_token'), false)
  assert.equal(stored.has('parking_user'), false)
  assert.equal(location.redirectedTo, '/login')
})
