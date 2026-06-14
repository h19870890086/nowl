<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  src: string
  alt?: string
  placeholder?: string
  lazy?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  alt: '',
  placeholder: '',
  lazy: true,
})

// 内置base64占位图（灰色背景+图片图标），避免依赖外部CDN
const FALLBACK_PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGRvbS1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTRiM2Q4IiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiIgZm9udC1zaXplPSIxNiI+Tm8gSW1hZ2U8L3RleHQ+PC9zdmc+'

const resolvedPlaceholder = computed(() => props.placeholder || FALLBACK_PLACEHOLDER)

const emit = defineEmits<{
  (e: 'load', event: Event): void
  (e: 'error', event: Event): void
}>()

const imageRef = ref<HTMLImageElement | null>(null)
const isLoaded = ref(false)
const hasError = ref(false)
const actualSrc = ref<string | undefined>(props.lazy ? undefined : props.src)
let observer: IntersectionObserver | null = null

// 图片加载完成
const handleLoad = (event: Event) => {
  isLoaded.value = true
  emit('load', event)
}

// 图片加载失败，尝试用占位图兜底
const handleError = (event: Event) => {
  const img = event.target as HTMLImageElement
  // 如果当前不是占位图，尝试用占位图兜底一次
  if (img && img.src !== resolvedPlaceholder.value) {
    img.src = resolvedPlaceholder.value
    return
  }
  // 占位图也加载失败，显示错误状态
  hasError.value = true
  emit('error', event)
}

// 使用IntersectionObserver实现懒加载
onMounted(() => {
  if (props.lazy && imageRef.value) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            if (imageRef.value) {
              imageRef.value.src = props.src
            }
            observer?.unobserve(entry.target)
          }
        })
      },
      {
        rootMargin: '100px',
        threshold: 0
      }
    )
    observer.observe(imageRef.value)
  }
})

// 当src变化时重新加载（例如商品切换时）
watch(() => props.src, (newSrc) => {
  if (newSrc && imageRef.value) {
    isLoaded.value = false
    hasError.value = false
    imageRef.value.src = newSrc
  }
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<template>
  <div class="image-container">
    <!-- 占位图/加载中状态 -->
    <div
      v-if="!isLoaded && !hasError"
      class="image-placeholder"
      :style="{ backgroundImage: `url(${resolvedPlaceholder})` }"
    >
      <div v-if="lazy" class="loading-spinner"></div>
    </div>

    <!-- 实际图片 -->
    <img
      v-if="!hasError"
      ref="imageRef"
      :alt="alt"
      :class="['image', { 'image-loaded': isLoaded }]"
      @load="handleLoad"
      @error="handleError"
      :src="actualSrc"
    />

    <!-- 加载失败状态 -->
    <div v-if="hasError" class="image-error">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="48"
        height="48"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
        <circle cx="8.5" cy="8.5" r="1.5" />
        <polyline points="21 15 16 10 5 21" />
      </svg>
      <span>图片加载失败</span>
    </div>
  </div>
</template>

<style scoped>
.image-container {
  position: relative;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #e5e7eb;
  border-top-color: #8D6E63;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
}

.image-loaded {
  opacity: 1;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
  color: #9ca3af;
  gap: 8px;
  min-height: 100px;
}

.image-error svg {
  opacity: 0.5;
}
</style>
