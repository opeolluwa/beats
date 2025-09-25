<script lang="ts" setup>
import {ref} from "vue"

const value = ref(null)
</script>

<template>
  <div class="flex flex-col items-center align-center justify-center  h-screen">
    <NuxtFileUpload
        :ui="{
      base: 'border-none hover:bg-unset dark:text-white bg-transparent',
      label:'text-2xl leading-16 -mb-4',
      icon:'size-8'
    }"
        class=" h-screen w-screen text-white"
        description=".pdf, .mp3, .mp4. .jpeg, ..."
        label="Drop your files or folder here"
        layout="list"
        multiple
        position="inside"
    />
  </div>
</template>

<style scoped>

</style>