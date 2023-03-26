<script setup lang="ts">
  import { ref } from "vue";
  import axios from "axios";

  const container = ref<HTMLDivElement | null>(null);
  var svg: HTMLElement | null = null;

  const serialized = ref<string | null>(null);

  axios.get("/vite.svg").then((response) => {
    const parser = new DOMParser();
    const document = parser.parseFromString(response.data, "image/svg+xml");

    svg = document.documentElement;

    container.value?.replaceChildren(svg);
  });

  function scaleUp() {
    svg?.setAttribute("width", "16em");
    svg?.setAttribute("height", "16em");
  }

  function scaleDown() {
    svg?.setAttribute("width", "4em");
    svg?.setAttribute("height", "4em");
  }

  function download() {
    if (svg) {
      const copy = svg.cloneNode(true) as HTMLElement;
      const xs = svg.getElementsByTagName("metadata");

      const metadata =
        xs.length === 1
          ? xs[0]
          : (function createMetadata() {
              const el = document.createElementNS(copy.namespaceURI, "metadata");
              copy.appendChild(el);
              return el;
            })();

      const source = document.createElementNS("https://github.com/agebhar1/foo", "source");
      source.textContent = btoa("only test data");
      metadata.appendChild(source);

      const serializer = new XMLSerializer();
      serialized.value = serializer.serializeToString(copy);

      const link = document.createElement("a");
      link.href = URL.createObjectURL(new Blob([serialized.value], { type: "image/svg+xml" }));
      link.download = "vite+embedded.svg";
      link.click();
      URL.revokeObjectURL(link.href);
    }
  }
</script>

<template>
  <div>
    <div ref="container" class="svg"></div>
    <div>
      <button @click="scaleUp">+</button>
      <button @click="scaleDown">-</button>
    </div>
    <div>
      <button @click="download">download</button>
      <pre>{{ serialized }}</pre>
    </div>
  </div>
</template>

<style scoped>
  .svg {
    padding: 1.5em;
  }
</style>
