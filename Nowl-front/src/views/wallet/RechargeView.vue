<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Wallet, Smartphone, CheckCircle2, History, ArrowLeft } from 'lucide-vue-next'
import { createRecharge, getRechargeHistory } from '@/api/modules/recharge'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/feedback'

const router = useRouter()
const userStore = useUserStore()

// 金额选项
const amountOptions = [10, 20, 50, 100, 200]
const customAmount = ref('')
const selectedAmount = ref<number | null>(null)
const paymentMethod = ref<'wechat' | 'alipay'>('wechat')
const isRecharging = ref(false)
const showSuccess = ref(false)
const lastRechargeAmount = ref(0)
const rechargeHistory = ref<Array<{ id: number; amount: number; paymentMethod: string; createTime: string }>>([])

const actualAmount = computed(() => {
  if (selectedAmount.value) return selectedAmount.value
  const num = parseFloat(customAmount.value)
  return isNaN(num) ? 0 : num
})

const canRecharge = computed(() => actualAmount.value > 0 && !isRecharging.value)

const selectAmount = (amount: number) => {
  selectedAmount.value = amount
  customAmount.value = ''
}

const handleRecharge = async () => {
  if (!canRecharge.value) return
  isRecharging.value = true
  try {
    await createRecharge({
      amount: actualAmount.value,
      paymentMethod: paymentMethod.value,
    })
    lastRechargeAmount.value = actualAmount.value
    showSuccess.value = true
    selectedAmount.value = null
    customAmount.value = ''
    // 刷新用户信息以更新余额
    await userStore.fetchUserInfo()
    // 刷新充值记录
    await fetchHistory()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '充值失败')
  } finally {
    isRecharging.value = false
  }
}

const fetchHistory = async () => {
  try {
    const res = await getRechargeHistory()
    rechargeHistory.value = res || []
  } catch {
    // 静默处理
  }
}

const closeSuccess = () => {
  showSuccess.value = false
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchHistory()
})
</script>

<template>
  <div class="max-w-lg mx-auto pb-20 pt-6 px-4 space-y-6 animate-in fade-in duration-500">
    <!-- 头部 -->
    <div class="flex items-center gap-3">
      <button @click="router.back()" class="p-2 rounded-full hover:bg-warm-50 transition-colors">
        <ArrowLeft :size="20" class="text-um-text" />
      </button>
      <div>
        <h1 class="text-xl font-black text-slate-800">账户充值</h1>
        <p class="text-sm text-um-muted">
          当前余额：<span class="text-warm-600 font-bold">￥{{ userStore.userInfo?.money || '0.00' }}</span>
        </p>
      </div>
    </div>

    <!-- 成功弹窗 -->
    <div v-if="showSuccess" class="um-card p-8 text-center space-y-4 animate-in zoom-in duration-300">
      <div class="w-16 h-16 bg-emerald-50 rounded-full flex items-center justify-center mx-auto">
        <CheckCircle2 :size="36" class="text-emerald-500" />
      </div>
      <div>
        <h2 class="text-lg font-bold text-slate-800">充值成功！</h2>
        <p class="text-um-muted mt-1">
          已到账 <span class="text-warm-600 font-bold text-xl">￥{{ lastRechargeAmount }}</span>
        </p>
        <p class="text-sm text-um-muted mt-1">
          当前余额：<span class="font-bold text-slate-800">￥{{ userStore.userInfo?.money || '0.00' }}</span>
        </p>
      </div>
      <button @click="closeSuccess" class="um-btn um-btn-primary w-full py-3">
        继续充值
      </button>
    </div>

    <!-- 充值表单 -->
    <div v-if="!showSuccess" class="space-y-6">
      <!-- 金额选择 -->
      <div class="um-card p-5 space-y-4">
        <h2 class="text-sm font-bold text-slate-700 flex items-center gap-2">
          <Wallet :size="16" class="text-warm-500" /> 选择充值金额
        </h2>
        <div class="grid grid-cols-3 gap-3">
          <button
            v-for="amount in amountOptions"
            :key="amount"
            @click="selectAmount(amount)"
            class="py-3 rounded-xl font-bold text-lg transition-all border-2"
            :class="
              selectedAmount === amount
                ? 'border-warm-500 bg-warm-50 text-warm-600 shadow-sm'
                : 'border-warm-100 bg-white text-slate-600 hover:border-warm-300'
            "
          >
            ￥{{ amount }}
          </button>
        </div>
        <div class="flex items-center bg-white border border-warm-100 rounded-xl overflow-hidden focus-within:border-warm-400 transition-colors">
          <span class="pl-4 pr-2 py-3 text-um-muted font-bold text-sm select-none bg-warm-50/50">￥</span>
          <input
            v-model="customAmount"
            type="number"
            min="0"
            step="0.01"
            placeholder="自定义金额"
            class="flex-1 pr-4 py-3 bg-transparent text-sm outline-none placeholder:text-um-muted/60"
            @input="selectedAmount = null"
          />
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="um-card p-5 space-y-4">
        <h2 class="text-sm font-bold text-slate-700 flex items-center gap-2">
          <Smartphone :size="16" class="text-warm-500" /> 选择支付方式
        </h2>
        <div class="space-y-3">
          <label
            class="flex items-center gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all"
            :class="
              paymentMethod === 'wechat'
                ? 'border-emerald-400 bg-emerald-50/30'
                : 'border-warm-100 bg-white hover:border-warm-300'
            "
          >
            <input
              type="radio"
              v-model="paymentMethod"
              value="wechat"
              class="accent-emerald-500 w-5 h-5"
            />
            <div class="flex items-center gap-3 flex-1">
              <div class="w-10 h-10 bg-emerald-500 rounded-xl flex items-center justify-center">
                <span class="text-white font-black text-lg">微</span>
              </div>
              <div>
                <span class="font-bold text-slate-700 text-sm">微信支付</span>
                <p class="text-xs text-um-muted">模拟支付，点击即到账</p>
              </div>
            </div>
          </label>

          <label
            class="flex items-center gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all"
            :class="
              paymentMethod === 'alipay'
                ? 'border-blue-400 bg-blue-50/30'
                : 'border-warm-100 bg-white hover:border-warm-300'
            "
          >
            <input
              type="radio"
              v-model="paymentMethod"
              value="alipay"
              class="accent-blue-500 w-5 h-5"
            />
            <div class="flex items-center gap-3 flex-1">
              <div class="w-10 h-10 bg-blue-500 rounded-xl flex items-center justify-center">
                <span class="text-white font-black text-lg">支</span>
              </div>
              <div>
                <span class="font-bold text-slate-700 text-sm">支付宝</span>
                <p class="text-xs text-um-muted">模拟支付，点击即到账</p>
              </div>
            </div>
          </label>
        </div>
      </div>

      <!-- 确认充值按钮 -->
      <button
        @click="handleRecharge"
        :disabled="!canRecharge"
        class="w-full py-4 rounded-2xl font-bold text-white text-lg transition-all active:scale-[0.98]"
        :class="
          canRecharge
            ? 'bg-gradient-to-r from-warm-500 to-warm-400 hover:shadow-lg'
            : 'bg-slate-300 cursor-not-allowed'
        "
      >
        {{ isRecharging ? '充值中...' : `确认充值 ￥${actualAmount}` }}
      </button>

      <!-- 充值记录 -->
      <div v-if="rechargeHistory.length > 0" class="um-card p-5 space-y-3">
        <h2 class="text-sm font-bold text-slate-700 flex items-center gap-2">
          <History :size="16" class="text-warm-500" /> 充值记录
        </h2>
        <div
          v-for="record in rechargeHistory"
          :key="record.id"
          class="flex items-center justify-between py-3 border-b border-warm-50 last:border-0"
        >
          <div class="flex items-center gap-3">
            <div
              class="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white"
              :class="record.paymentMethod === '微信支付' ? 'bg-emerald-400' : 'bg-blue-400'"
            >
              {{ record.paymentMethod === '微信支付' ? '微' : '支' }}
            </div>
            <div>
              <span class="text-sm font-medium text-slate-700">{{ record.paymentMethod }}</span>
              <p class="text-xs text-um-muted">{{ formatTime(record.createTime) }}</p>
            </div>
          </div>
          <span class="font-bold text-warm-600">+￥{{ record.amount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
