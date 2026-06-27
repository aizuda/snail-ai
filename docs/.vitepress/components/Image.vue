<template>
  <div>
    <n-image :src="src" :width="width" />
  </div>
</template>
<script>
import { defineComponent } from 'vue'
import pkg from 'naive-ui'
const { NImage } = pkg

export default defineComponent({
  props:{
    src: String,
    width: {
      type: String,
      default: '100%'
    }
  },
  setup() {
    return {
      // src: '/img/core_function.jpg'
    }
  },
  components: {
    NImage
  }
})
</script>
