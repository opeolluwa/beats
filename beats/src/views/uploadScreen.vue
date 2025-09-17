<script lang="ts" setup>
import {ref} from "vue"

const value = ref(null)
</script>

<template>
  <div class="flex flex-col items-center align-center justify-center  h-screen">
    <NuxtFileUpload
        :ui="{
      base: 'min-h-64 w-96'
    }"
        class="w-96"
        description="SVG, PNG, JPG or GIF (max. 2MB)"
        label="Drop your images here"
        layout="list"
        multiple
        position="inside"
    />
  </div>
</template>

<style scoped>

</style>