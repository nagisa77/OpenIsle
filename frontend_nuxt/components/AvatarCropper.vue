<template>
  <div v-if="show" class="cropper-modal">
    <div class="cropper-body">
      <div class="cropper-wrapper">
        <img ref="image" :src="src" alt="to crop" />
      </div>
      <div class="cropper-actions">
        <button class="cropper-btn" @click="$emit('close')">取消</button>
        <button class="cropper-btn primary" @click="onConfirm">确定</button>
      </div>
    </div>
  </div>
</template>

<script>
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

export default {
  name: 'AvatarCropper',
  props: {
    src: {
      type: String,
      required: true,
    },
    show: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['close', 'crop'],
  data() {
    return { cropper: null }
  },
  watch: {
    show(val) {
      if (val) {
        this.$nextTick(() => this.init())
      } else {
        this.destroy()
      }
    },
  },
  mounted() {
    if (this.show) {
      this.init()
    }
  },
  methods: {
    init() {
      const image = this.$refs.image
      this.cropper = new Cropper(image, {
        aspectRatio: 1,
        viewMode: 1,
        autoCropArea: 1,
        responsive: true,
      })
    },
    destroy() {
      if (this.cropper) {
        this.cropper.destroy()
        this.cropper = null
      }
    },
    onConfirm() {
      if (!this.cropper) return
      this.cropper.getCroppedCanvas({ width: 256, height: 256 }).toBlob((blob) => {
        const file = new File([blob], 'avatar.png', { type: 'image/png' })
        const url = URL.createObjectURL(blob)
        this.$emit('crop', { file, url })
        this.$emit('close')
        this.destroy()
      })
    },
  },
}
</script>

<style scoped>
.cropper-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  opacity: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.cropper-body {
  background: var(--background-color);
  padding: 10px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cropper-wrapper {
  width: 80vw;
  height: 80vw;
  max-width: 400px;
  max-height: 400px;
}

.cropper-wrapper img {
  max-width: 100%;
}

.cropper-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.cropper-btn {
  padding: 6px 12px;
  border-radius: 4px;
  color: var(--primary-color);
  border: none;
  background: transparent;
  cursor: pointer;
}

.cropper-btn.primary {
  background: var(--primary-color);
  color: var(--text-color);
  border-color: var(--primary-color);
}

@media (min-width: 768px) {
  .cropper-wrapper {
    width: 400px;
    height: 400px;
  }
}
</style>
