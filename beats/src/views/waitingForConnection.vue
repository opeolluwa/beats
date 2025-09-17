<script lang="ts" setup>

</script>

<template>
  <div class="flex flex-col items-center align-center justify-center  h-screen">
    <div class="loader">

    </div>
    <h1 class="text-2xl mt-16 dark:text-red-500">
      Waiting for connection ...
    </h1>
  </div>
</template>

<style scoped>
/* HTML: <div class="loader"></div> */
.loader {
  width: 50px;
  aspect-ratio: 1;
  border-radius: 50%;
  background: #CC5E3D;
  box-shadow: 0 0 0 0 #E0694444;
  animation: l2 1.5s infinite ease-in-out;
  position: relative;
}

.loader:before,
.loader:after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow: 0 0 0 0 #0004;
  animation: inherit;
  animation-delay: -0.5s;
}

.loader:after {
  animation-delay: -1s;
}

@keyframes l2 {
  100% {
    box-shadow: 0 0 0 70px #0000
  }
}
</style>