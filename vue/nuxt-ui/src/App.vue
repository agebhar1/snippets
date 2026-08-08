<script setup lang="ts">

</script>

<template>
  <UApp>
    <UHeader>
      <template #title>Nuxt UI</template>
    </UHeader>

    <UBanner color="warning" icon="i-lucide-info" title="This is a banner with an icon." />

    <UMain>
      <UContainer>
        Main
      </UContainer>
    </UMain>

    <UFooter>
      <template #left>
        <p class="text-muted text-sm">Copyright © {{ new Date().getFullYear() }}</p>
      </template>
    </UFooter>
  </UApp>
</template>
