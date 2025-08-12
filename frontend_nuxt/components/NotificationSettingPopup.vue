<template>
  <BasePopup :visible="visible" @close="close">
    <div class="notification-popup">
      <div class="notification-popup-title">🎉 通知设置上线啦</div>
      <div class="notification-popup-text">现在可以在消息 -> 消息设置中调整通知类型</div>
      <div class="notification-popup-actions">
        <div class="notification-popup-close" @click="close">知道了</div>
        <div class="notification-popup-button" @click="gotoSetting">去看看</div>
      </div>
    </div>
  </BasePopup>
</template>

<script>
import BasePopup from '~/components/BasePopup.vue'
import { useRouter } from 'vue-router'

export default {
  name: 'NotificationSettingPopup',
  components: { BasePopup },
  props: {
    visible: { type: Boolean, default: false },
  },
  emits: ['close'],
  setup(props, { emit }) {
    const router = useRouter()
    const gotoSetting = () => {
      emit('close')
      router.push('/message?tab=control')
    }
    const close = () => emit('close')
    return { gotoSetting, close }
  },
}
</script>

<style scoped>
.notification-popup {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  min-width: 200px;
}

.notification-popup-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.notification-popup-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 20px;
}

.notification-popup-button {
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.notification-popup-button:hover {
  background-color: var(--primary-color-hover);
}

.notification-popup-close {
  cursor: pointer;
  color: var(--primary-color);
  display: flex;
  align-items: center;
}

.notification-popup-close:hover {
  text-decoration: underline;
}
</style>
