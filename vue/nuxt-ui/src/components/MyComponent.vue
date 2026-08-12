<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from '@nuxt/ui/composables'

defineProps({
  text: String,
})

const count = ref(0)
const toast = useToast()

function showToast() {
  toast.add({
    title: 'Toast title',
    description: 'Toast description',
    actions: [{
      icon: 'i-lucide-lightbulb',
      label: 'Toast action label',
      color: 'primary',
    }],
  })
}

function increment() {
  count.value++
}
</script>

<template>
  <div>
    <h1>{{ text }}</h1>
    <div>Count: {{ count }}</div><UButton @click="increment">++</UButton>
    <div><UButton data-test-id="toast" @click="showToast()">Toast</UButton></div>
  </div>
</template>
