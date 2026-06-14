import './assets/main.css'
import 'element-plus/dist/index.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

// ======== 全局Polyfill：兼容旧版浏览器 ========
// 确保 crypto.randomUUID 在旧浏览器中可用
if (typeof crypto === 'undefined') {
  // @ts-ignore - 旧浏览器可能没有crypto对象
  self.crypto = {}
}
// @ts-ignore - randomUUID 可能不存在
if (typeof crypto.randomUUID !== 'function') {
  // @ts-ignore
  crypto.randomUUID = function () {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c: string) {
      var r = (Math.random() * 16) | 0
      var v = c === 'x' ? r : (r & 0x3) | 0x8
      return v.toString(16)
    })
  }
}

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

app.mount('#app')
