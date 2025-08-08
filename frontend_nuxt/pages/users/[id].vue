<template>
  <div class="profile-page">
    <div v-if="isLoading" class="loading-page">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)" />
    </div>

    <div v-else>
      <div class="profile-page-header">
        <div class="profile-page-header-avatar">
          <img :src="user.avatar" alt="avatar" class="profile-page-header-avatar-img" />
        </div>
        <div class="profile-page-header-user-info">
          <div class="profile-page-header-user-info-name">{{ user.username }}</div>
          <div class="profile-page-header-user-info-description">{{ user.introduction }}</div>
          <div v-if="!isMine && !subscribed" class="profile-page-header-subscribe-button" @click="subscribeUser">
            <i class="fas fa-user-plus"></i>
            关注
          </div>
          <div v-if="!isMine && subscribed" class="profile-page-header-unsubscribe-button" @click="unsubscribeUser">
            <i class="fas fa-user-minus"></i>
            取消关注
          </div>
          <LevelProgress
            :exp="levelInfo.exp"
            :current-level="levelInfo.currentLevel"
            :next-exp="levelInfo.nextExp"
          />
          <div class="profile-level-target">
            目标 Lv.{{ levelInfo.currentLevel + 1 }}
            <i
              class="fas fa-info-circle profile-exp-info"
              title="经验值可通过发帖、评论等操作获得，达到目标后即可提升等级，解锁更多功能。"
            ></i>
          </div>
        </div>
      </div>

      <div class="profile-info">
        <div class="profile-info-item">
          <div class="profile-info-item-label">加入时间:</div>
          <div class="profile-info-item-value">{{ formatDate(user.createdAt) }}</div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">最后发帖时间:</div>
          <div class="profile-info-item-value">{{ formatDate(user.lastPostTime) }}</div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">最后评论时间:</div>
          <div class="profile-info-item-value">{{ user.lastCommentTime!=null?formatDate(user.lastCommentTime):"暂无评论" }}</div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">浏览量:</div>
          <div class="profile-info-item-value">{{ user.totalViews }}</div>
        </div>
      </div>

      <div class="profile-tabs">
        <div :class="['profile-tabs-item', { selected: selectedTab === 'summary' }]" @click="selectedTab = 'summary'">
          <i class="fas fa-chart-line"></i>
          <div class="profile-tabs-item-label">总结</div>
        </div>
        <div :class="['profile-tabs-item', { selected: selectedTab === 'timeline' }]" @click="selectedTab = 'timeline'">
          <i class="fas fa-clock"></i>
          <div class="profile-tabs-item-label">时间线</div>
        </div>
        <div :class="['profile-tabs-item', { selected: selectedTab === 'following' }]"
          @click="selectedTab = 'following'">
          <i class="fas fa-user-plus"></i>
          <div class="profile-tabs-item-label">关注</div>
        </div>
      </div>

      <div v-if="tabLoading" class="tab-loading">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)" />
      </div>
      <template v-else>
        <div v-if="selectedTab === 'summary'" class="profile-summary">
          <div class="total-summary">
            <div class="summary-title">统计信息</div>
            <div class="total-summary-content">
              <div class="total-summary-item">
                <div class="total-summary-item-label">访问天数</div>
                <div class="total-summary-item-value">{{ user.visitedDays }}</div>
              </div>
              <div class="total-summary-item">
                <div class="total-summary-item-label">已读帖子</div>
                <div class="total-summary-item-value">{{ user.readPosts }}</div>
              </div>
              <div class="total-summary-item">
                <div class="total-summary-item-label">已送出的💗</div>
                <div class="total-summary-item-value">{{ user.likesSent }}</div>
              </div>
              <div class="total-summary-item">
                <div class="total-summary-item-label">已收到的💗</div>
                <div class="total-summary-item-value">{{ user.likesReceived }}</div>
              </div>
            </div>
          </div>
          <div class="summary-divider">
            <div class="hot-reply">
              <div class="summary-title">热门回复</div>
              <div class="summary-content" v-if="hotReplies.length > 0">
                <BaseTimeline :items="hotReplies">
                  <template #item="{ item }">
                    在
                    <router-link :to="`/posts/${item.comment.post.id}`" class="timeline-link">
                      {{ item.comment.post.title }}
                    </router-link>
                    <template v-if="item.comment.parentComment">
                      下对
                      <router-link :to="`/posts/${item.comment.post.id}#comment-${item.comment.parentComment.id}`"
                        class="timeline-link">
                        {{ stripMarkdownLength(item.comment.parentComment.content, 200) }}
                      </router-link>
                      回复了
                    </template>
                    <template v-else>
                      下评论了
                    </template>
                    <router-link :to="`/posts/${item.comment.post.id}#comment-${item.comment.id}`"
                      class="timeline-link">
                      {{ stripMarkdownLength(item.comment.content, 200) }}
                    </router-link>
                    <div class="timeline-date">
                      {{ formatDate(item.comment.createdAt) }}
                    </div>
                  </template>
                </BaseTimeline>
              </div>
              <div v-else>
                <div class="summary-empty">暂无热门回复</div>
              </div>
            </div>
            <div class="hot-topic">
              <div class="summary-title">热门话题</div>
              <div class="summary-content" v-if="hotPosts.length > 0">
                <BaseTimeline :items="hotPosts">
                  <template #item="{ item }">
                    <router-link :to="`/posts/${item.post.id}`" class="timeline-link">
                      {{ item.post.title }}
                    </router-link>
                    <div class="timeline-snippet">
                      {{ stripMarkdown(item.post.snippet) }}
                    </div>
                    <div class="timeline-date">
                      {{ formatDate(item.post.createdAt) }}
                    </div>
                  </template>
                </BaseTimeline>
              </div>
              <div v-else>
                <div class="summary-empty">暂无热门话题</div>
              </div>
            </div>
            <div class="hot-tag">
              <div class="summary-title">TA创建的tag</div>
              <div class="summary-content" v-if="hotTags.length > 0">
                <BaseTimeline :items="hotTags">
                  <template #item="{ item }">
                    <span class="timeline-link" @click="gotoTag(item.tag)">
                      {{ item.tag.name }}<span v-if="item.tag.count"> x{{ item.tag.count }}</span>
                    </span>
                    <div class="timeline-snippet" v-if="item.tag.description">
                      {{ item.tag.description }}
                    </div>
                    <div class="timeline-date">
                      {{ formatDate(item.tag.createdAt) }}
                    </div>
                  </template>
                </BaseTimeline>
              </div>
              <div v-else>
                <div class="summary-empty">暂无标签</div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="selectedTab === 'timeline'" class="profile-timeline">
          <BasePlaceholder v-if="timelineItems.length === 0" text="暂无时间线" icon="fas fa-inbox" />
          <BaseTimeline :items="timelineItems">
            <template #item="{ item }">
              <template v-if="item.type === 'post'">
                发布了文章
                <router-link :to="`/posts/${item.post.id}`" class="timeline-link">
                  {{ item.post.title }}
                </router-link>
                <div class="timeline-date">{{ formatDate(item.createdAt) }}</div>
              </template>
              <template v-else-if="item.type === 'comment'">
                在
                <router-link :to="`/posts/${item.comment.post.id}`" class="timeline-link">
                  {{ item.comment.post.title }}
                </router-link>
                下评论了
                <router-link :to="`/posts/${item.comment.post.id}#comment-${item.comment.id}`" class="timeline-link">
                  {{ stripMarkdownLength(item.comment.content, 200) }}
                </router-link>
                <div class="timeline-date">{{ formatDate(item.createdAt) }}</div>
              </template>
              <template v-else-if="item.type === 'reply'">
                在
                <router-link :to="`/posts/${item.comment.post.id}`" class="timeline-link">
                  {{ item.comment.post.title }}
                </router-link>
                下对
                <router-link :to="`/posts/${item.comment.post.id}#comment-${item.comment.parentComment.id}`"
                  class="timeline-link">
                  {{ stripMarkdownLength(item.comment.parentComment.content, 200) }}
                </router-link>
                回复了
                <router-link :to="`/posts/${item.comment.post.id}#comment-${item.comment.id}`" class="timeline-link">
                  {{ stripMarkdownLength(item.comment.content, 200) }}
                </router-link>
                <div class="timeline-date">{{ formatDate(item.createdAt) }}</div>
              </template>
              <template v-else-if="item.type === 'tag'">
                创建了标签
                <span class="timeline-link" @click="gotoTag(item.tag)">
                  {{ item.tag.name }}<span v-if="item.tag.count"> x{{ item.tag.count }}</span>
                </span>
                <div class="timeline-snippet" v-if="item.tag.description">
                  {{ item.tag.description }}
                </div>
                <div class="timeline-date">{{ formatDate(item.createdAt) }}</div>
              </template>
            </template>
          </BaseTimeline>
        </div>

        <div v-else class="follow-container">
          <div class="follow-tabs">
            <div :class="['follow-tab-item', { selected: followTab === 'followers' }]"
              @click="followTab = 'followers'">关注者
            </div>
            <div :class="['follow-tab-item', { selected: followTab === 'following' }]"
              @click="followTab = 'following'">正在关注
            </div>
          </div>
          <div class="follow-list">
            <UserList v-if="followTab === 'followers'" :users="followers" />
            <UserList v-else :users="followings" />
          </div>
        </div>

      </template>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { API_BASE_URL, toast } from '../main'
import { getToken, authState } from '../utils/auth'
import BaseTimeline from '../components/BaseTimeline.vue'
import UserList from '../components/UserList.vue'
import BasePlaceholder from '../components/BasePlaceholder.vue'
import LevelProgress from '../components/LevelProgress.vue'
import { stripMarkdown, stripMarkdownLength } from '../utils/markdown'
import TimeManager from '../utils/time'
import { prevLevelExp } from '../utils/level'

export default {
  name: 'ProfileView',
  components: { BaseTimeline, UserList, BasePlaceholder, LevelProgress },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const username = route.params.id

    const user = ref({})
    const hotPosts = ref([])
    const hotReplies = ref([])
    const hotTags = ref([])
    const timelineItems = ref([])
    const followers = ref([])
    const followings = ref([])
    const subscribed = ref(false)
    const isLoading = ref(true)
    const tabLoading = ref(false)
    const selectedTab = ref('summary')
    const followTab = ref('followers')

    const levelInfo = computed(() => {
      const exp = user.value.experience || 0
      const currentLevel = user.value.currentLevel || 0
      const nextExp = user.value.nextLevelExp || 0
      const prevExp = prevLevelExp(currentLevel)
      const total = nextExp - prevExp
      const ratio = total > 0 ? (exp - prevExp) / total : 1
      const percent = Math.max(0, Math.min(1, ratio)) * 100
      return { exp, currentLevel, nextExp, percent }
    })

    const isMine = computed(() => authState.username === username)

    const formatDate = (d) => {
      if (!d) return ''
      return TimeManager.format(d)
    }

    const fetchUser = async () => {
      const token = getToken()
      const headers = token ? { Authorization: `Bearer ${token}` } : {}
      const res = await fetch(`${API_BASE_URL}/api/users/${username}`, { headers })
      if (res.ok) {
        const data = await res.json()
        user.value = data
        subscribed.value = !!data.subscribed
      } else if (res.status === 404) {
        router.replace('/404')
      }
    }

    const fetchSummary = async () => {
      const postsRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-posts`)
      if (postsRes.ok) {
        const data = await postsRes.json()
        hotPosts.value = data.map(p => ({ icon: 'fas fa-book', post: p }))
      }

      const repliesRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-replies`)
      if (repliesRes.ok) {
        const data = await repliesRes.json()
        hotReplies.value = data.map(c => ({ icon: 'fas fa-comment', comment: c }))
      }

      const tagsRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-tags`)
      if (tagsRes.ok) {
        const data = await tagsRes.json()
        hotTags.value = data.map(t => ({ icon: 'fas fa-tag', tag: t }))
      }
    }

    const fetchTimeline = async () => {
      const [postsRes, repliesRes, tagsRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/users/${username}/posts?limit=50`),
        fetch(`${API_BASE_URL}/api/users/${username}/replies?limit=50`),
        fetch(`${API_BASE_URL}/api/users/${username}/tags?limit=50`)
      ])
      const posts = postsRes.ok ? await postsRes.json() : []
      const replies = repliesRes.ok ? await repliesRes.json() : []
      const tags = tagsRes.ok ? await tagsRes.json() : []
      const mapped = [
        ...posts.map(p => ({
          type: 'post',
          icon: 'fas fa-book',
          post: p,
          createdAt: p.createdAt
        })),
        ...replies.map(r => ({
          type: r.parentComment ? 'reply' : 'comment',
          icon: 'fas fa-comment',
          comment: r,
          createdAt: r.createdAt
        })),
        ...tags.map(t => ({
          type: 'tag',
          icon: 'fas fa-tag',
          tag: t,
          createdAt: t.createdAt
        }))
      ]
      mapped.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      timelineItems.value = mapped
    }

    const fetchFollowUsers = async () => {
      const [followerRes, followingRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/users/${username}/followers`),
        fetch(`${API_BASE_URL}/api/users/${username}/following`)
      ])
      followers.value = followerRes.ok ? await followerRes.json() : []
      followings.value = followingRes.ok ? await followingRes.json() : []
    }

    const loadSummary = async () => {
      tabLoading.value = true
      await fetchSummary()
      tabLoading.value = false
    }

    const loadTimeline = async () => {
      tabLoading.value = true
      await fetchTimeline()
      tabLoading.value = false
    }

    const loadFollow = async () => {
      tabLoading.value = true
      await fetchFollowUsers()
      tabLoading.value = false
    }

    const subscribeUser = async () => {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      const res = await fetch(`${API_BASE_URL}/api/subscriptions/users/${username}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        subscribed.value = true
        toast.success('已关注')
      } else {
        toast.error('操作失败')
      }
    }

    const unsubscribeUser = async () => {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      const res = await fetch(`${API_BASE_URL}/api/subscriptions/users/${username}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        subscribed.value = false
        toast.success('已取消关注')
      } else {
        toast.error('操作失败')
      }
    }

    const gotoTag = tag => {
      const value = encodeURIComponent(tag.id ?? tag.name)
      router.push({ path: '/', query: { tags: value } })
    }

    const init = async () => {
      try {
        await fetchUser()
        await loadSummary()
      } catch (e) {
        console.error(e)
      } finally {
        isLoading.value = false
      }
    }

    onMounted(init)

    watch(selectedTab, async val => {
      if (val === 'timeline' && timelineItems.value.length === 0) {
        await loadTimeline()
      } else if (val === 'following' && followers.value.length === 0 && followings.value.length === 0) {
        await loadFollow()
      }
    })
    return {
      user,
      hotPosts,
      hotReplies,
      timelineItems,
      followers,
      followings,
      subscribed,
      isMine,
      isLoading,
      tabLoading,
      selectedTab,
      followTab,
      formatDate,
      stripMarkdown,
      stripMarkdownLength,
      loadTimeline,
      loadFollow,
      loadSummary,
      subscribeUser,
      unsubscribeUser,
      gotoTag,
      hotTags,
      levelInfo
    }
  }
}
</script>

<style scoped>
.loading-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.profile-page {
  background-color: var(--background-color);
}

.profile-page-header {
  display: flex;
  align-items: center;
  padding: 20px;
}

.profile-page-header-avatar-img {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-page-header-user-info {
  margin-left: 20px;
}

.profile-page-header-user-info-name {
  font-size: 24px;
  font-weight: bold;
}

.profile-page-header-user-info-description {
  font-size: 20px;
  color: #666;
}

.profile-page-header-subscribe-button {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  border-radius: 8px;
  padding: 5px 10px;
  color: white;
  background-color: var(--primary-color);
  margin-top: 15px;
  width: fit-content;
  cursor: pointer;
}

.profile-page-header-subscribe-button:hover {
  background-color: var(--primary-color-hover);
}

.profile-page-header-unsubscribe-button {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  border-radius: 8px;
  padding: 5px 10px;
  color: var(--primary-color);
  background-color: var(--background-color);
  border: 1px solid var(--primary-color);
  margin-top: 15px;
  width: fit-content;
  cursor: pointer;
}

.profile-level-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
  font-size: 14px;
}

.profile-level-current {
  font-weight: bold;
}

.profile-level-bar {
  width: 200px;
  height: 8px;
  background-color: var(--normal-background-color);
  border-radius: 4px;
  overflow: hidden;
}

.profile-level-bar-inner {
  height: 100%;
  background-color: var(--primary-color);
}

.profile-level-info {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.profile-level-exp,
.profile-level-target {
  font-size: 12px;
  opacity: 0.8;
}

.profile-exp-info {
  margin-left: 4px;
  opacity: 0.5;
  cursor: pointer;
}

.profile-info {
  display: flex;
  flex-direction: row;
  padding: 0 20px;
  gap: 20px;
  border-top: 1px solid var(--normal-border-color);
  border-bottom: 1px solid var(--normal-border-color);
  scrollbar-width: none; 
  overflow-x: auto;
}

.profile-info-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  padding: 10px 0;
  white-space: nowrap; 
}

.profile-info-item-label {
  font-size: 14px;
  opacity: 0.7;
}

.profile-info-item-value {
  font-size: 14px;
}

.profile-tabs {
  position: sticky;
  top: calc(var(--header-height) + 1px);
  z-index: 200;
  background-color: var(--background-color-blur);
  display: flex;
  flex-direction: row;
  padding: 0 20px;
  border-bottom: 1px solid var(--normal-border-color);
  scrollbar-width: none; 
  overflow-x: auto;
 }

.profile-tabs-item {
  display: flex;
  flex: 0 0 auto;
  flex-direction: row;
  gap: 10px;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  width: 200px;
  cursor: pointer;
  white-space: nowrap; 
}

.profile-tabs-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.profile-summary {
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 20px;
}

.summary-title {
  font-size: 20px;
  margin-bottom: 10px;
  font-weight: bold;
}

.total-summary {
  width: 100%;
}

.total-summary-content {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  row-gap: 0px;
  column-gap: 20px;
}

.total-summary-item {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.total-summary-item-label {
  font-size: 18px;
  opacity: 0.7;
}

.total-summary-item-value {
  font-size: 24px;
  font-weight: bold;
}

.summary-divider {
  margin-top: 20px;
  display: flex;
  flex-direction: row;
  gap: 20px;
  width: 100%;
  flex-wrap: wrap;
}

.hot-reply,
.hot-topic,
.hot-tag {
  width: 40%;
}

.profile-timeline {
  padding: 20px;
}

.timeline-date {
  font-size: 12px;
  color: gray;
  margin-top: 5px;
}

.timeline-snippet {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.timeline-link {
  font-weight: bold;
  color: var(--primary-color);
  text-decoration: none;
}

.timeline-link:hover {
  text-decoration: underline;
}

.summary-empty {
  margin-top: 10px;
  font-size: 14px;
  opacity: 0.5;
}

.summary-content {
  margin-top: 10px;
}

.tab-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.follow-container {}

.follow-tabs {
  display: flex;
  flex-direction: row;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 10px;
}

.follow-tab-item {
  padding: 10px 20px;
  cursor: pointer;
}

.follow-tab-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.follow-list {
  padding-left: 20px;
}

@media (max-width: 768px) {
  .profile-page {
    width: 100vw;
  }

  .profile-page-header-avatar-img {
    width: 100px;
    height: 100px;
  }

  .profile-tabs-item {
    width: 100px;
  }

  .summary-divider {
    flex-direction: column;
  }

  .hot-reply,
  .hot-topic,
  .hot-tag {
    width: 100%;
  }

  .profile-timeline {
    width: calc(100vw - 40px);
  }
}
</style>
