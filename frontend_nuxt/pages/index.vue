<template>
  <div class="home-page">
    <div v-if="!isMobile" class="search-container">
      <div class="search-title">一切可能，从此刻启航</div>
      <div class="search-subtitle">愿你在此遇见灵感与共鸣。若有疑惑，欢迎发问，亦可在知识的海洋中搜寻答案。</div>
      <SearchDropdown />
    </div>


    <div class="topic-container">
      <div class="topic-item-container">
        <div
          v-for="topic in topics"
          :key="topic"
          class="topic-item"
          :class="{ selected: topic === selectedTopic }"
          @click="selectedTopic = topic"
        >
          {{ topic }}
        </div>
        <div class="topic-select-container">
          <CategorySelect v-model="selectedCategory" :options="categoryOptions" />
          <TagSelect v-model="selectedTags" :options="tagOptions" />
        </div>
      </div>
    </div>

    <div class="article-container">
      <template v-if="selectedTopic === '最新' || selectedTopic === '排行榜' || selectedTopic === '最新回复'">
        <div class="article-header-container">
          <div class="header-item main-item">
            <div class="header-item-text">话题</div>
          </div>
          <div class="header-item avatars">
            <div class="header-item-text">参与人员</div>
          </div>
          <div class="header-item comments">
            <div class="header-item-text">回复</div>
          </div>
          <div class="header-item views">
            <div class="header-item-text">浏览</div>
          </div>
          <div class="header-item activity">
            <div class="header-item-text">活动</div>
          </div>
        </div>

        <div v-if="isLoadingPosts && articles.length === 0" class="loading-container">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        </div>

        <div v-else-if="articles.length === 0">
          <div class="no-posts-container">
            <div class="no-posts-text">暂时没有帖子 :( 点击发帖发送第一篇相关帖子吧!</div>
          </div>
        </div>

        <div class="article-item" v-for="article in articles" :key="article.id">
          <div class="article-main-container">
            <NuxtLink class="article-item-title main-item" :to="`/posts/${article.id}`">
              <i v-if="article.pinned" class="fas fa-thumbtack pinned-icon"></i>
              {{ article.title }}
            </NuxtLink>
            <div class="article-item-description main-item">{{ sanitizeDescription(article.description) }}</div>
            <div class="article-info-container main-item">
              <ArticleCategory :category="article.category" />
              <ArticleTags :tags="article.tags" />
            </div>
          </div>

          <div class="article-member-avatars-container">
            <NuxtLink
              v-for="member in article.members"
              :key="member.id"
              class="article-member-avatar-item"
              :to="`/users/${member.id}`"
            >
              <img class="article-member-avatar-item-img" :src="member.avatar" alt="avatar" />
            </NuxtLink>
          </div>

          <div class="article-comments main-info-text">
            {{ article.comments }}
          </div>

          <div class="article-views main-info-text">
            {{ article.views }}
          </div>

          <div class="article-time main-info-text">
            {{ article.time }}
          </div>
        </div>
      </template>
      <div v-else-if="selectedTopic === '热门'" class="placeholder-container">
        热门帖子功能开发中，敬请期待。
      </div>
      <div v-else class="placeholder-container">
        分类浏览功能开发中，敬请期待。
      </div>
      <div v-if="isLoadingPosts && articles.length > 0" class="loading-container bottom-loading">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useScrollLoadMore } from '~/utils/loadMore'
import { stripMarkdown } from '~/utils/markdown'
import { API_BASE_URL } from '~/main'
import { getToken } from '~/utils/auth'
import TimeManager from '~/utils/time'
import CategorySelect from '~/components/CategorySelect.vue'
import TagSelect from '~/components/TagSelect.vue'
import ArticleTags from '~/components/ArticleTags.vue'
import ArticleCategory from '~/components/ArticleCategory.vue'
import SearchDropdown from '~/components/SearchDropdown.vue'
import { isMobile } from '~/utils/screen'

export default {
  name: 'HomePageView',
  components: {
    CategorySelect,
    TagSelect,
    ArticleTags,
    ArticleCategory,
    SearchDropdown,
    ClientOnly: () => import('vue').then(m => m.defineAsyncComponent(() => import('vue').then(() => ({ template: '<slot />' }))))
  },
  async setup() {
    const route = useRoute()
    const selectedCategory = ref('')
    if (route.query.category) {
      const c = decodeURIComponent(route.query.category)
      selectedCategory.value = isNaN(c) ? c : Number(c)
    }
    const selectedTags = ref([])
    if (route.query.tags) {
      const t = Array.isArray(route.query.tags) ? route.query.tags.join(',') : route.query.tags
      selectedTags.value = t
        .split(',')
        .filter(v => v)
        .map(v => decodeURIComponent(v))
        .map(v => (isNaN(v) ? v : Number(v)))
    }

    const tagOptions = ref([])
    const categoryOptions = ref([])
    const isLoadingPosts = ref(false)
    const topics = ref(['最新回复', '最新', '排行榜' /*, '热门', '类别'*/])
    const selectedTopic = ref(
      route.query.view === 'ranking'
        ? '排行榜'
        : route.query.view === 'latest'
          ? '最新'
          : '最新回复'
    )

    const articles = ref([])
    const page = ref(0)
    const pageSize = 10
    const allLoaded = ref(false)

    const loadOptions = async () => {
      if (selectedCategory.value && !isNaN(selectedCategory.value)) {
        try {
          const res = await fetch(`${API_BASE_URL}/api/categories/${selectedCategory.value}`)
          if (res.ok) {
            categoryOptions.value = [await res.json()]
          }
        } catch (e) { /* ignore */ }
      }

      if (selectedTags.value.length) {
        const arr = []
        for (const t of selectedTags.value) {
          if (!isNaN(t)) {
            try {
              const r = await fetch(`${API_BASE_URL}/api/tags/${t}`)
              if (r.ok) arr.push(await r.json())
            } catch (e) { /* ignore */ }
          }
        }
        tagOptions.value = arr
      }
    }

    const buildUrl = () => {
      let url = `${API_BASE_URL}/api/posts?page=${page.value}&pageSize=${pageSize}`
      if (selectedCategory.value) {
        url += `&categoryId=${selectedCategory.value}`
      }
      if (selectedTags.value.length) {
        selectedTags.value.forEach(t => {
          url += `&tagIds=${t}`
        })
      }
      return url
    }

    const buildRankUrl = () => {
      let url = `${API_BASE_URL}/api/posts/ranking?page=${page.value}&pageSize=${pageSize}`
      if (selectedCategory.value) {
        url += `&categoryId=${selectedCategory.value}`
      }
      if (selectedTags.value.length) {
        selectedTags.value.forEach(t => {
          url += `&tagIds=${t}`
        })
      }
      return url
    }

    const buildReplyUrl = () => {
      let url = `${API_BASE_URL}/api/posts/latest-reply?page=${page.value}&pageSize=${pageSize}`
      if (selectedCategory.value) {
        url += `&categoryId=${selectedCategory.value}`
      }
      if (selectedTags.value.length) {
        selectedTags.value.forEach(t => {
          url += `&tagIds=${t}`
        })
      }
      return url
    }

    const fetchPosts = async (reset = false) => {
      if (reset) {
        page.value = 0
        allLoaded.value = false
        articles.value = []
      }
      if (isLoadingPosts.value || allLoaded.value) return
      try {
        isLoadingPosts.value = true
        const token = getToken()
        const res = await fetch(buildUrl(), {
          headers: {
            Authorization: token ? `Bearer ${token}` : ''
          }
        })
        isLoadingPosts.value = false
        if (!res.ok) return
        const data = await res.json()
        articles.value.push(
          ...data.map(p => ({
            id: p.id,
            title: p.title,
            description: p.content,
            category: p.category,
            tags: p.tags || [],
            members: (p.participants || []).map(m => ({ id: m.id, avatar: m.avatar })),
            comments: p.commentCount,
            views: p.views,
            time: TimeManager.format(p.createdAt),
            pinned: !!p.pinnedAt
          }))
        )
        if (data.length < pageSize) {
          allLoaded.value = true
        } else {
          page.value += 1
        }
      } catch (e) {
        console.error(e)
      }
    }

    const fetchRanking = async (reset = false) => {
      if (reset) {
        page.value = 0
        allLoaded.value = false
        articles.value = []
      }
      if (isLoadingPosts.value || allLoaded.value) return
      try {
        isLoadingPosts.value = true
        const token = getToken()
        const res = await fetch(buildRankUrl(), {
          headers: {
            Authorization: token ? `Bearer ${token}` : ''
          }
        })
        isLoadingPosts.value = false
        if (!res.ok) return
        const data = await res.json()
        articles.value.push(
          ...data.map(p => ({
            id: p.id,
            title: p.title,
            description: p.content,
            category: p.category,
            tags: p.tags || [],
            members: (p.participants || []).map(m => ({ id: m.id, avatar: m.avatar })),
            comments: p.commentCount,
            views: p.views,
            time: TimeManager.format(p.createdAt),
            pinned: !!p.pinnedAt
          }))
        )
        if (data.length < pageSize) {
          allLoaded.value = true
        } else {
          page.value += 1
        }
      } catch (e) {
        console.error(e)
      }
    }

    const fetchLatestReply = async (reset = false) => {
      if (reset) {
        page.value = 0
        allLoaded.value = false
        articles.value = []
      }
      if (isLoadingPosts.value || allLoaded.value) return
      try {
        isLoadingPosts.value = true
        const token = getToken()
        const res = await fetch(buildReplyUrl(), {
          headers: {
            Authorization: token ? `Bearer ${token}` : ''
          }
        })
        isLoadingPosts.value = false
        if (!res.ok) return
        const data = await res.json()
        articles.value.push(
          ...data.map(p => ({
            id: p.id,
            title: p.title,
            description: p.content,
            category: p.category,
            tags: p.tags || [],
            members: (p.participants || []).map(m => ({ id: m.id, avatar: m.avatar })),
            comments: p.commentCount,
            views: p.views,
            time: TimeManager.format(p.lastReplyAt || p.createdAt),
            pinned: !!p.pinnedAt
          }))
        )
        if (data.length < pageSize) {
          allLoaded.value = true
        } else {
          page.value += 1
        }
      } catch (e) {
        console.error(e)
      }
    }

    const fetchContent = async (reset = false) => {
      if (selectedTopic.value === '排行榜') {
        await fetchRanking(reset)
      } else if (selectedTopic.value === '最新回复') {
        await fetchLatestReply(reset)
      } else {
        await fetchPosts(reset)
      }
    }

    useScrollLoadMore(fetchContent)

    watch([selectedCategory, selectedTags], () => {
      fetchContent(true)
    })

    watch(selectedTopic, () => {
      fetchContent(true)
    })

    const sanitizeDescription = text => stripMarkdown(text)

    await Promise.all([loadOptions(), fetchContent()])

    return {
      topics,
      selectedTopic,
      articles,
      sanitizeDescription,
      isLoadingPosts,
      selectedCategory,
      selectedTags,
      tagOptions,
      categoryOptions,
      isMobile
    }
  }
}
</script>

<style scoped>
.home-page {
  background-color: var(--background-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  container-type: inline-size;
  container-name: home-page;
}

.search-container {
  margin-top: 100px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.search-title {
  font-size: 32px;
  font-weight: bold;
}

.search-subtitle {
  font-size: 16px;
}


.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.bottom-loading {
  height: 100px;
}

.no-posts-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.no-posts-text {
  font-size: 14px;
  opacity: 0.7;
}

.topic-container {
  position: sticky;
  top: calc(var(--header-height) + 1px);
  z-index: 10;
  background-color: var(--background-color-blur);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 0;
}

.topic-item-container {
  margin-left: 20px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  flex-wrap: wrap;
}

.topic-select-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.topic-item {
  padding: 2px 10px;
  cursor: pointer;
}

.topic-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.article-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding-bottom: 100px;
}

.article-header-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  color: gray;
  border-bottom: 1px solid var(--normal-border-color);
  padding-bottom: 10px;
}

.article-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  border-bottom: 1px solid var(--normal-border-color);
}


.article-main-container,
.header-item.main-item {
  width: calc(60% - 20px);
  padding-left: 20px;
}

/* .article-member-avatars-container,
.header-item.avatars, */
.article-comments,
.header-item.comments,
.article-views,
.header-item.views,
.article-time,
.header-item.activity {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}

.article-member-avatars-container,
.header-item.avatars {
  width: 20%;
}

.article-comments,
.header-item.comments {
  width: 5%;
}

.article-views,
.header-item.views {
  width: 5%;
}

.article-time,
.header-item.activity {
  width: 10%;
}

.article-item-title {
  margin-top: 10px;
  font-size: 20px;
  text-decoration: none;
  color: var(--text-color);
  max-width: 100%;
}

.article-item-title:hover {
  color: var(--primary-color);
  text-decoration: underline;
}

.pinned-icon {
  margin-right: 4px;
  color: var(--primary-color);
}

.article-item-description {
  margin-top: 10px;
  font-size: 14px;
  color: gray;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-info-container {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.article-tags-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.article-tag-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.article-main-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.article-member-avatars-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 3px;
  margin-left: 20px;
}

.article-member-avatar-item {
  width: 25px;
  height: 25px;
  border-radius: 50%;
  overflow: hidden;
}

.article-member-avatar-item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  font-size: 16px;
  opacity: 0.7;
}

.main-info-text {
  font-size: 14px;
  opacity: 0.7;
}

@container home-page (max-width: 900px) {
  .article-main-container,
  .header-item.main-item {
    width: calc(70% - 20px);
    padding-left: 20px;
  }
  
  .article-member-avatars-container,
  .header-item.avatars {
    width: 10%;
  }
  
  .article-comments,
  .header-item.comments {
    width: 5%;
  }
  
  .article-views,
  .header-item.views {
    width: 5%;
  }
  
  .article-time,
  .header-item.activity {
    width: 10%;
  }
  .article-member-avatar-item:nth-child(n+4) {
    display: none;
  }
}

@container home-page (max-width: 768px) {
  .article-main-container,
  .header-item.main-item {
    width: calc(70% - 20px);
    padding-left: 20px;
  }
  
  .article-member-avatars-container,
  .header-item.avatars {
    width: 10%;
  }
  
  .article-comments,
  .header-item.comments {
    display: none;
  }
  
  .article-views,
  .header-item.views {
    display: none;
  }
  
  .article-time,
  .header-item.activity {
    width: 10%;
    margin-right: 3%;
  }

  .article-header-container {
    display: none;
  }

  .article-member-avatar-item:nth-child(n+2) {
    display: none;
  }
  
  .header-item-text {
    font-size: 12px;
  }

  .article-item-title {
    font-size: 16px;
    font-weight: bold;
  }

  .article-item-description {
    margin-top: 2px;
    font-size: 10px;
    max-width: 100%;
  }

  .main-info-text {
    font-size: 10px;
    opacity: 0.5;
  }

  .topic-container {
    position: initial;
  }
}

</style>
