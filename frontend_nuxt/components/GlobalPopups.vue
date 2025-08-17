<template>
  <div>
    <ActivityPopup
      :visible="showInvitePointsPopup"
      :icon="invitePointsIcon"
      text="邀请码送积分活动火热进行中，快来邀请好友吧！"
      @close="closeInvitePointsPopup"
    />
    <ActivityPopup
      :visible="showMilkTeaPopup"
      :icon="milkTeaIcon"
      text="建站送奶茶活动火热进行中，快来参与吧！"
      @close="closeMilkTeaPopup"
    />
    <NotificationSettingPopup :visible="showNotificationPopup" @close="closeNotificationPopup" />
    <MedalPopup :visible="showMedalPopup" :medals="newMedals" @close="closeMedalPopup" />
  </div>
</template>

<script setup>
import ActivityPopup from '~/components/ActivityPopup.vue'
import MedalPopup from '~/components/MedalPopup.vue'
import NotificationSettingPopup from '~/components/NotificationSettingPopup.vue'
import { authState } from '~/utils/auth'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const showInvitePointsPopup = ref(false)
const invitePointsIcon = ref('')
const showMilkTeaPopup = ref(false)
const milkTeaIcon = ref('')
const showNotificationPopup = ref(false)
const showMedalPopup = ref(false)
const newMedals = ref([])

onMounted(async () => {
  await checkInvitePointsActivity()
  if (showInvitePointsPopup.value) return

  await checkMilkTeaActivity()
  if (showMilkTeaPopup.value) return

  await checkNotificationSetting()
  if (showNotificationPopup.value) return

  await checkNewMedals()
})

const checkMilkTeaActivity = async () => {
  if (!process.client) return
  if (localStorage.getItem('milkTeaActivityPopupShown')) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/activities`)
    if (res.ok) {
      const list = await res.json()
      const a = list.find((i) => i.type === 'MILK_TEA' && !i.ended)
      if (a) {
        milkTeaIcon.value = a.icon
        showMilkTeaPopup.value = true
      }
    }
  } catch (e) {
    // ignore network errors
  }
}
const closeMilkTeaPopup = () => {
  if (!process.client) return
  localStorage.setItem('milkTeaActivityPopupShown', 'true')
  showMilkTeaPopup.value = false
  checkNotificationSetting()
}
const checkInvitePointsActivity = async () => {
  if (!process.client) return
  if (localStorage.getItem('invitePointsActivityPopupShown')) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/activities`)
    if (res.ok) {
      const list = await res.json()
      const a = list.find((i) => i.type === 'INVITE_POINTS' && !i.ended)
      if (a) {
        invitePointsIcon.value = a.icon
        showInvitePointsPopup.value = true
      }
    }
  } catch (e) {
    // ignore network errors
  }
}
const closeInvitePointsPopup = () => {
  if (!process.client) return
  localStorage.setItem('invitePointsActivityPopupShown', 'true')
  showInvitePointsPopup.value = false
  checkMilkTeaActivity()
}
const checkNotificationSetting = async () => {
  if (!process.client) return
  if (!authState.loggedIn) return
  if (localStorage.getItem('notificationSettingPopupShown')) return
  showNotificationPopup.value = true
}
const closeNotificationPopup = () => {
  if (!process.client) return
  localStorage.setItem('notificationSettingPopupShown', 'true')
  showNotificationPopup.value = false
  checkNewMedals()
}
const checkNewMedals = async () => {
  if (!process.client) return
  if (!authState.loggedIn || !authState.userId) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/medals?userId=${authState.userId}`)
    if (res.ok) {
      const medals = await res.json()
      const seen = JSON.parse(localStorage.getItem('seenMedals') || '[]')
      const m = medals.filter((i) => i.completed && !seen.includes(i.type))
      if (m.length > 0) {
        newMedals.value = m
        showMedalPopup.value = true
      }
    }
  } catch (e) {
    // ignore errors
  }
}
const closeMedalPopup = () => {
  if (!process.client) return
  const seen = new Set(JSON.parse(localStorage.getItem('seenMedals') || '[]'))
  newMedals.value.forEach((m) => seen.add(m.type))
  localStorage.setItem('seenMedals', JSON.stringify([...seen]))
  showMedalPopup.value = false
}
</script>
