<script setup lang="ts">
import { ref } from 'vue'

defineProps({
  text: String,
})

const count = ref(0)

function increment() {
  count.value++
}
</script>

<template>
  <div>
    <h1>{{ text }}</h1>
    <div>Count: {{ count }}</div><UButton @click="increment">++</UButton>
  </div>
</template>
