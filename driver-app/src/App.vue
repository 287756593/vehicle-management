<template>
  <div id="app" class="app-shell">
    <router-view />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

const themeQuery = typeof window !== 'undefined'
  ? window.matchMedia('(prefers-color-scheme: dark)')
  : null

const applyTheme = (isDark) => {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.dataset.theme = isDark ? 'dark' : 'light'
  document.documentElement.style.colorScheme = isDark ? 'dark' : 'light'
}

const handleThemeChange = (event) => {
  applyTheme(event.matches)
}

if (themeQuery) {
  applyTheme(themeQuery.matches)
}

onMounted(() => {
  if (!themeQuery) {
    return
  }
  if (themeQuery.addEventListener) {
    themeQuery.addEventListener('change', handleThemeChange)
  } else {
    themeQuery.addListener(handleThemeChange)
  }
})

onBeforeUnmount(() => {
  if (!themeQuery) {
    return
  }
  if (themeQuery.removeEventListener) {
    themeQuery.removeEventListener('change', handleThemeChange)
  } else {
    themeQuery.removeListener(handleThemeChange)
  }
})
</script>
