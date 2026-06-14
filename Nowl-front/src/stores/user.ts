import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, RegisterForm, LoginForm } from '@/types'
import * as userApi from '@/api/modules/user'
import { AuthStatus } from '@/constants'
import { normalizeMediaData } from '@/utils/media'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref<UserInfo | null>(null)
  // 手动切换的校区（用户主动选择查看其他校区的商品），为null时使用默认逻辑
  const manualCampus = ref<{ schoolCode: string; campusCode: string; name: string } | null>(null)

  if (userInfo.value) {
    userInfo.value = normalizeMediaData(userInfo.value)
  }

  // 计算属性：自动根据 userInfo 推导登录状态
  const isLoggedIn = computed(() => !!userInfo.value)

  /**
   * 当前生效的校区筛选条件
   * - 未登录/未认证用户(authStatus!=2): 返回null，不筛选，可看到全部学校商品
   * - 已认证用户(authStatus=2): 默认使用用户自己的学校/校区，可通过switchCampus手动切换
   * - 手动切换后使用manualCampus，确保用户主动选择后不会因userInfo刷新而回退
   */
  const currentCampus = computed(() => {
    // 用户主动手动切换了校区，使用手动选择的值
    if (manualCampus.value) {
      return manualCampus.value
    }
    // 已认证用户默认使用自己的学校/校区
    const ui = userInfo.value
    const hasSchoolInfo = ui?.schoolCode && ui?.campusCode
    const isAuthenticated = ui?.authStatus === AuthStatus.APPROVED
    if (ui && hasSchoolInfo && isAuthenticated) {
      return {
        schoolCode: ui.schoolCode!,
        campusCode: ui.campusCode!,
        name: ui.campusName || '本校',
      }
    }
    // 未认证/待审核用户/游客：不筛选校区，显示全部商品
    return null
  })

  // 登录
  const login = async (loginData: LoginForm) => {
    try {
      const res = await userApi.login(loginData)
      userInfo.value = normalizeMediaData(res.userInfo)
      manualCampus.value = null // 登录时清除手动校区选择
      return res
    } catch (error) {
      console.error('登录失败', error)
      throw error
    }
  }

  // 切换当前查看的校区（用户主动操作，会覆盖默认逻辑）
  const switchCampus = (schoolCode: string, campusCode: string, name: string) => {
    manualCampus.value = { schoolCode, campusCode, name }
  }

  // 重置校区筛选为默认（跟随用户自身学校/校区）
  const resetCampus = () => {
    manualCampus.value = null
  }

  // 注册
  const register = async (data: RegisterForm) => {
    try {
      const res = await userApi.register(data)
      return res
    } catch (error) {
      console.error('注册失败', error)
      throw error
    }
  }

  // 登出
  const clearLocalSession = () => {
    userInfo.value = null
    manualCampus.value = null // 清除手动校区选择
  }

  const logout = async (options?: { notifyServer?: boolean }) => {
    const shouldNotifyServer = options?.notifyServer !== false
    if (shouldNotifyServer) {
      try {
        await userApi.logout()
      } catch (error) {
        console.warn('Backend logout failed, proceeding with local cleanup', error)
      }
    }
    clearLocalSession()
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await userApi.getCurrentUserInfo()
      userInfo.value = normalizeMediaData(res)
      return res
    } catch (error) {
      // 获取失败时清空用户信息（token 可能已过期）
      userInfo.value = null
      console.error('获取用户信息失败', error)
      throw error
    }
  }

  // 更新用户信息
  const updateInfo = async (data: Partial<UserInfo>) => {
    try {
      await userApi.updateUserInfo(data)
      if (userInfo.value) {
        userInfo.value = normalizeMediaData({ ...userInfo.value, ...data })
      }
    } catch (error) {
      console.error('更新用户信息失败', error)
      throw error
    }
  }

  return {
    userInfo,
    isLoggedIn,
    currentCampus,
    manualCampus,
    login,
    register,
    logout,
    fetchUserInfo,
    updateInfo,
    switchCampus,
    resetCampus,
    clearLocalSession,
  }
}, {
  persist: {
    key: 'user-store',
    storage: localStorage,
    pick: ['userInfo', 'manualCampus'], // currentCampus已改为computed，不持久化；手动切换的校区需要持久化
  },
})
