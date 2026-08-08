<script setup lang="ts">
import MyComponent from '@/components/MyComponent.vue'
</script>

<template>
  <UApp>
    <UHeader>
      <template #title>Nuxt UI</template>
      <template #bottom>
        <UBanner class="sticky"  color="warning" icon="i-lucide-info" title="This is a banner with an icon." />
      </template>
    </UHeader>

    <UMain>
      <UContainer>
        Main
        <my-component v-bind:is="MyComponent" text='Hello World'></my-component>
      </UContainer>
    </UMain>

    <UFooter>
      <template #left>
        <p class="text-muted text-sm">Copyright © {{ new Date().getFullYear() }}</p>
      </template>
    </UFooter>
  </UApp>
</template>
