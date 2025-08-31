<script setup lang="ts">
import { ref } from 'vue'
import { QBtn } from 'quasar'

defineProps<{ msg: string }>()

const count = ref(0)

const model = ref(null)
const options = ['apple', 'banana', 'orange']

</script>

<template>
  <div data-testid="x">
    <h1>{{ msg }}</h1>

    <div class="card">
      <q-btn
        color="primary"
        @click="count++"
      >
        <slot name="default">
          count is {{ count }}
        </slot>
      </q-btn>
      <span>
        Selected: {{ model }}
      </span>
      <q-select
        v-model="model"
        :options="options"
        label="Selector"
        data-testid="1"
        name="select"
      />
    </div>
  </div>
</template>
