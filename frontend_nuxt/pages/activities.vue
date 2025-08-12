<template>
  <div class="activity-list-page">
    <div v-if="isLoadingActivities" class="loading-activities">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>

    <div class="activity-list-page-card" v-for="a in activities" :key="a.id">
      <div class="activity-list-page-card-normal">
        <div v-if="a.icon" class="activity-card-normal-left">
          <img :src="a.icon" alt="avatar" class="activity-card-left-avatar-img" />
        </div>
        <div class="activity-card-normal-right">
          <div class="activity-card-normal-right-header">
            <div class="activity-list-page-card-title">{{ a.title }}</div>
            <div v-if="a.ended" class="activity-list-page-card-state-end">已结束</div>
            <div v-else class="activity-list-page-card-state-ongoing">进行中</div>
          </div>
          <div class="activity-list-page-card-content">{{ a.content }}</div>
          <div class="activity-list-page-card-footer">
            <div class="activity-list-page-card-footer-start-time">
              <i class="fas fa-clock"></i>
              <span>开始于 {{ TimeManager.format(a.startTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <MilkTeaActivityComponent v-if="a.type === 'MILK_TEA'" />
    </div>
  </div>
</template>

<script>
import { API_BASE_URL } from '~/main'
import TimeManager from '~/utils/time'
import MilkTeaActivityComponent from '~/components/MilkTeaActivityComponent.vue'

export default {
  name: 'ActivityListPageView',
  components: { MilkTeaActivityComponent },
  data() {
    return {
      activities: [],
      TimeManager,
      isLoadingActivities: false,
    }
  },
  async mounted() {
    this.isLoadingActivities = true
    try {
      const res = await fetch(`${API_BASE_URL}/api/activities`)
      if (res.ok) {
        this.activities = await res.json()
      }
    } catch (e) {
      console.error(e)
    } finally {
      this.isLoadingActivities = false
    }
  },
}
</script>

<style scoped>
.loading-activities {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.activity-list-page {
  background-color: var(--background-color);
  padding: 20px;
  height: calc(100% - 40px);
  overflow-y: auto;
}

.activity-list-page-card {
  padding: 10px;
  width: calc(100% - 20px);
  gap: 10px;
  background-color: var(--activity-card-background-color);
  border-radius: 20px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
}

.activity-card-left-avatar-img {
  width: 150px;
  height: 150px;
  border-radius: 10%;
  object-fit: cover;
  background-color: var(--background-color);
}

.activity-card-normal-right-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.activity-list-page-card-normal {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.activity-list-page-card-title {
  font-size: 1.2rem;
  font-weight: bold;
}

.activity-list-page-card-content {
  font-size: 1rem;
  margin-top: 10px;
  opacity: 0.8;
}

.activity-list-page-card-footer {
  margin-top: 10px;
}

.activity-list-page-card-state-end,
.activity-list-page-card-state-ongoing {
  font-size: 0.8rem;
}

.activity-list-page-card-state-end {
  color: var(--text-color);
  opacity: 0.5;
}

.activity-list-page-card-state-ongoing {
  color: var(--primary-color);
}

.activity-list-page-card-footer-start-time {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 0.8rem;
  opacity: 0.7;
}

.activity-link {
  text-decoration: none;
  color: inherit;
}

@media screen and (max-width: 768px) {
  .activity-card-left-avatar-img {
    width: 80px;
    height: 80px;
  }

  .activity-list-page-card-title {
    font-size: 16px;
  }

  .activity-list-page-card-content {
    font-size: 12px;
    opacity: 0.7;
  }
}
</style>
