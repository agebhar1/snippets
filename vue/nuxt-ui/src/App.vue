<script setup lang="ts">
import MyComponent from '@/components/MyComponent.vue'
</script>

<template>
  <UApp :toaster="{ position: 'top-right' }">
    <UBanner icon="i-lucide-construction" title="Under Construction"/>
    <UHeader title="Nuxt UI"/>

    <UMain>
      <UContainer>Main</UContainer>
      <UContainer>
        <my-component v-bind:is="MyComponent" text='Hello World'/>
      </UContainer>
    </UMain>

    <UFooter>
      <template #left>
        <p class="text-muted text-sm">Copyright © {{ new Date().getFullYear() }}</p>
      </template>
    </UFooter>
  </UApp>
</template>
