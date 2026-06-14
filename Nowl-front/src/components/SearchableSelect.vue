<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Search, ChevronDown } from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
  disabled?: boolean
}

interface Props {
  modelValue: string | number | undefined
  options: Option[]
  placeholder?: string
  disabled?: boolean
  searchable?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string | number | undefined): void
  (e: 'change'): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择',
  disabled: false,
  searchable: true,
})

const emit = defineEmits<Emits>()

const isOpen = ref(false)
const searchText = ref('')
const dropdownRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()

const selectedLabel = computed(() => {
  const selected = props.options.find(opt => opt.value === props.modelValue)
  return selected?.label || props.placeholder
})

const filteredOptions = computed(() => {
  if (!props.searchable || !searchText.value.trim()) {
    return props.options
  }
  const keyword = searchText.value.trim().toLowerCase()
  return props.options.filter(opt =>
    opt.label.toLowerCase().includes(keyword)
  )
})

const toggleDropdown = () => {
  if (props.disabled) return
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    searchText.value = ''
    setTimeout(() => inputRef.value?.focus(), 100)
  }
}

const selectOption = (option: Option) => {
  if (option.disabled) return
  emit('update:modelValue', option.value)
  isOpen.value = false
  searchText.value = ''
  emit('change')
}

const handleClickOutside = (event: MouseEvent) => {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    isOpen.value = false
    searchText.value = ''
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div ref="dropdownRef" class="searchable-select relative">
    <!-- 触发器 -->
    <button
      type="button"
      @click="toggleDropdown"
      :disabled="disabled"
      class="w-full flex items-center justify-between px-3 h-[38px] border rounded-xl text-left text-sm transition-colors"
      :class="[
        disabled
          ? 'bg-slate-100 text-slate-400 cursor-not-allowed border-slate-200'
          : 'bg-white hover:border-warm-300 focus:outline-none cursor-pointer border-slate-200',
        isOpen ? 'border-warm-400 ring-2 ring-warm-100' : ''
      ]"
    >
      <span :class="modelValue !== undefined ? 'text-slate-700' : 'text-slate-400'">
        {{ selectedLabel }}
      </span>
      <ChevronDown
        :size="16"
        class="text-slate-400 transition-transform shrink-0 ml-2"
        :class="{ 'rotate-180': isOpen }"
      />
    </button>

    <!-- 下拉面板 -->
    <Transition name="dropdown">
      <div
        v-if="isOpen"
        class="absolute z-50 mt-1 w-full bg-white border border-slate-200 rounded-xl shadow-lg overflow-hidden"
      >
        <!-- 搜索输入框 -->
        <div v-if="searchable && options.length > 10" class="p-2 border-b border-slate-100">
          <div class="relative">
            <Search :size="14" class="absolute left-2.5 top-2.5 text-slate-400" />
            <input
              ref="inputRef"
              v-model="searchText"
              type="text"
              placeholder="搜索..."
              class="w-full pl-8 pr-3 py-1.5 text-sm border border-slate-200 rounded-lg focus:outline-none focus:border-warm-400"
              @click.stop
            />
          </div>
        </div>

        <!-- 选项列表 -->
        <div class="max-h-60 overflow-y-auto overscroll-contain">
          <div
            v-if="filteredOptions.length === 0"
            class="px-3 py-4 text-sm text-slate-400 text-center"
          >
            无匹配选项
          </div>
          <button
            v-for="option in filteredOptions"
            :key="option.value"
            type="button"
            @click="selectOption(option)"
            :disabled="option.disabled"
            class="w-full text-left px-3 py-2.5 text-sm transition-colors"
            :class="[
              option.disabled
                ? 'text-slate-300 cursor-not-allowed'
                : modelValue === option.value
                  ? 'bg-warm-50 text-warm-600 font-semibold'
                  : 'text-slate-700 hover:bg-slate-50',
            ]"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.dropdown-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-leave-active {
  transition: opacity 0.1s ease, transform 0.1s ease;
}
.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.searchable-select {
  min-width: 0;
}
</style>