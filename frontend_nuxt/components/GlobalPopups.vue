<template>
  <div>
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

<script>
import ActivityPopup from '~/components/ActivityPopup.vue'
import MedalPopup from '~/components/MedalPopup.vue'
import NotificationSettingPopup from '~/components/NotificationSettingPopup.vue'
import { API_BASE_URL } from '~/main'
import { authState } from '~/utils/auth'

export default {
  name: 'GlobalPopups',
  components: { ActivityPopup, MedalPopup, NotificationSettingPopup },
  data() {
    return {
      showMilkTeaPopup: false,
      milkTeaIcon: '',
      showNotificationPopup: false,
      showMedalPopup: false,
      newMedals: [],
    }
  },
  async mounted() {
    await this.checkMilkTeaActivity()
    if (this.showMilkTeaPopup) return

    await this.checkNotificationSetting()
    if (this.showNotificationPopup) return

    await this.checkNewMedals()
  },
  methods: {
    async checkMilkTeaActivity() {
      if (!process.client) return
      if (localStorage.getItem('milkTeaActivityPopupShown')) return
      try {
        const res = await fetch(`${API_BASE_URL}/api/activities`)
        if (res.ok) {
          const list = await res.json()
          const a = list.find((i) => i.type === 'MILK_TEA' && !i.ended)
          if (a) {
            this.milkTeaIcon = a.icon
            this.showMilkTeaPopup = true
          }
        }
      } catch (e) {
        // ignore network errors
      }
    },
    closeMilkTeaPopup() {
      if (!process.client) return
      localStorage.setItem('milkTeaActivityPopupShown', 'true')
      this.showMilkTeaPopup = false
      this.checkNotificationSetting()
    },
    async checkNotificationSetting() {
      if (!process.client) return
      if (!authState.loggedIn) return
      if (localStorage.getItem('notificationSettingPopupShown')) return
      this.showNotificationPopup = true
    },
    closeNotificationPopup() {
      if (!process.client) return
      localStorage.setItem('notificationSettingPopupShown', 'true')
      this.showNotificationPopup = false
      this.checkNewMedals()
    },
    async checkNewMedals() {
      if (!process.client) return
      if (!authState.loggedIn || !authState.userId) return
      try {
        const res = await fetch(`${API_BASE_URL}/api/medals?userId=${authState.userId}`)
        if (res.ok) {
          const medals = await res.json()
          const seen = JSON.parse(localStorage.getItem('seenMedals') || '[]')
          const m = medals.filter((i) => i.completed && !seen.includes(i.type))
          if (m.length > 0) {
            this.newMedals = m
            this.showMedalPopup = true
          }
        }
      } catch (e) {
        // ignore errors
      }
    },
    closeMedalPopup() {
      if (!process.client) return
      const seen = new Set(JSON.parse(localStorage.getItem('seenMedals') || '[]'))
      this.newMedals.forEach((m) => seen.add(m.type))
      localStorage.setItem('seenMedals', JSON.stringify([...seen]))
      this.showMedalPopup = false
    },
  },
}
</script>
