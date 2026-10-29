import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import test from 'node:test'

const component = readFileSync(new URL('../src/views/LoginView.vue', import.meta.url), 'utf8')

function loadLogin(response) {
  const script = component.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')
  const context = {
    ref: value => ({ value }), onUnmounted: () => {}, useRouter: () => ({}), useUserStore: () => ({}),
    api: { post: async () => response }, setInterval: () => 1, clearInterval: () => {}
  }
  runInNewContext(script + `\nglobalThis.testState = {
    handleSendSms, form, errorMsg, smsToast,
    smsCode: typeof smsCode === 'undefined' ? undefined : smsCode,
    fillSmsCode: typeof fillSmsCode === 'undefined' ? undefined : fillSmsCode
  }`, context)
  return context.testState
}

test('demo SMS display and refill both use this response code', async () => {
  const state = loadLogin({ data: { demo_code: '235791' } })
  await state.handleSendSms()
  assert.equal(state.smsCode?.value, '235791')
  assert.equal(state.form.value.verify_code, '235791')
  state.form.value.verify_code = ''
  state.fillSmsCode()
  assert.equal(state.form.value.verify_code, '235791')
  assert.match(component, /\{\{\s*smsCode\s*\}\}/)
  assert.match(component, /@click="fillSmsCode"/)
})

test('missing demo code does not fabricate a fallback or display a message', async () => {
  const state = loadLogin({ data: {} })
  await state.handleSendSms()
  assert.equal(state.form.value.verify_code, '')
  assert.equal(state.smsToast.value, false)
  assert.match(state.errorMsg.value, /验证码/)
})
