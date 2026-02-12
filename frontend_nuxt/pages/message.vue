<template>
  <div class="message-page">
    <BaseTabs v-model="selectedTab" :tabs="tabs">
      <template #right>
        <div class="message-page-header-right">
          <div class="message-page-header-right-item" @click="markAllRead">
            <check-correct class="message-page-header-right-item-button-icon" />
            <span class="message-page-header-right-item-button-text"> 已读所有消息 </span>
          </div>
        </div>
      </template>

      <div v-if="selectedTab === 'control'">
        <div class="message-control-container">
          <div class="message-control-title">通知设置</div>
          <div class="message-control-item-container">
            <template v-for="pref in notificationPrefs">
              <div
                v-if="canShowNotification(pref.type)"
                :key="pref.type"
                class="message-control-item"
              >
                <div class="message-control-item-label">{{ formatType(pref.type) }}</div>
                <BaseSwitch
                  :model-value="pref.enabled"
                  @update:modelValue="(val) => togglePref(pref, val)"
                />
              </div>
            </template>
          </div>
        </div>
        <div class="message-control-container">
          <div class="message-control-title">邮件通知设置</div>
          <div class="message-control-item-container">
            <div v-for="pref in emailPrefs" :key="pref.type" class="message-control-item">
              <div class="message-control-item-label">{{ formatType(pref.type) }}</div>
              <BaseSwitch
                :model-value="pref.enabled"
                @update:modelValue="(val) => toggleEmailPref(pref, val)"
              />
            </div>
          </div>
        </div>
      </div>

      <template v-else>
        <div v-if="isLoadingMessage" class="loading-message">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        </div>

        <BasePlaceholder
          v-else-if="notifications.length === 0"
          text="暂时没有消息 :)"
          icon="inbox"
        />

        <div class="timeline-container" v-if="notifications.length > 0">
          <BaseTimeline :items="notifications">
            <template #item="{ item }">
              <div class="notif-content" :class="{ read: item.read }">
                <span v-if="!item.read" class="unread-dot"></span>
                <span class="notif-type">
                  <template v-if="item.type === 'COMMENT_REPLY' && item.parentComment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.comment.author.id}`"
                        >{{ item.comment.author.username }}
                      </NuxtLink>
                      对我的评论
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}#comment-${item.parentComment.id}`"
                        >
                          <span
                            v-html="stripMarkdownWithTiebaMoji(item.parentComment.content, 500)"
                          ></span>
                        </NuxtLink>
                      </span>
                      回复了
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                        >
                          <span
                            v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"
                          ></span>
                        </NuxtLink>
                      </span>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'COMMENT_REPLY' && !item.parentComment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.comment.author.id}`"
                        >{{ item.comment.author.username }}
                      </NuxtLink>
                      对我的文章
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}`"
                        >
                          {{ stripMarkdownLength(item.post.title, 100) }}
                        </NuxtLink>
                      </span>
                      回复了
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                        >
                          <span
                            v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"
                          ></span>
                        </NuxtLink>
                      </span>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'ACTIVITY_REDEEM' && !item.parentComment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <span class="notif-user">{{ item.fromUser.username }} </span>
                      申请进行奶茶兑换，联系方式是：{{ item.content }}
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POINT_REDEEM' && !item.parentComment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <span class="notif-user">{{ item.fromUser.username }} </span>
                      申请积分兑换，联系方式是：{{ item.content }}
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'REACTION' && item.post && !item.comment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <span class="notif-user">{{ item.fromUser.username }} </span> 对我的文章
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}`"
                        >
                          {{ stripMarkdownLength(item.post.title, 100) }}
                        </NuxtLink>
                      </span>
                      进行了表态
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'REACTION' && item.comment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                        >{{ item.fromUser.username }}
                      </NuxtLink>
                      对我的评论
                      <span>
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                        >
                          <span
                            v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"
                          ></span>
                        </NuxtLink>
                      </span>
                      进行了表态
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_VIEWED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      查看了您的帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'LOTTERY_WIN'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      恭喜你在抽奖贴
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      中获奖
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'LOTTERY_DRAW'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您的抽奖贴
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已开奖
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POLL_VOTE'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      有用户参与了你的投票贴
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POLL_RESULT_OWNER'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你的投票帖
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已出结果
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POLL_RESULT_PARTICIPANT'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你参与的投票帖
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已出结果
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'CATEGORY_PROPOSAL_RESULT_OWNER'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你的分类提案
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      <span v-if="item.approved">已通过</span>
                      <span v-else>
                        未通过<span v-if="item.content">，原因：{{ item.content }}</span>
                      </span>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'CATEGORY_PROPOSAL_RESULT_PARTICIPANT'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你参与的分类提案
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      <span v-if="item.approved">已通过</span>
                      <span v-else>
                        未通过<span v-if="item.content">，原因：{{ item.content }}</span>
                      </span>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_UPDATED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您关注的帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      下面有新评论
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        <span v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"></span>
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'USER_ACTIVITY' && item.parentComment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你关注的
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.comment.author.id}`"
                      >
                        {{ item.comment.author.username }}
                      </NuxtLink>
                      在 对评论
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.parentComment.id}`"
                      >
                        <span
                          v-html="stripMarkdownWithTiebaMoji(item.parentComment.content, 500)"
                        ></span>
                      </NuxtLink>
                      回复了
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        <span v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"></span>
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'USER_ACTIVITY'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你关注的
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.comment.author.id}`"
                      >
                        {{ item.comment.author.username }}
                      </NuxtLink>
                      在文章
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      下面评论了
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        <span v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"></span>
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'MENTION' && item.comment">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      在评论中提到了你：
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        <span v-html="stripMarkdownWithTiebaMoji(item.comment.content, 500)"></span>
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'MENTION'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      在帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      中提到了你
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'USER_FOLLOWED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      开始关注你了
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'USER_UNFOLLOWED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      取消关注你了
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'FOLLOWED_POST'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      你关注的
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      发布了文章
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_SUBSCRIBED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      订阅了你的文章
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_UNSUBSCRIBED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      取消订阅了你的文章
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_REVIEW_REQUEST' && item.fromUser">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      发布了帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      ，请审核
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_REVIEW_REQUEST'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您发布的帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已提交审核
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'REGISTER_REQUEST'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      {{ item.fromUser.username }} 希望注册为会员，理由是：{{ item.content }}
                      <template #actions v-if="authState.role === 'ADMIN'">
                        <div v-if="!item.read" class="optional-buttons">
                          <div
                            class="mark-approve-button-item"
                            @click="approve(item.fromUser.id, item.id)"
                          >
                            同意
                          </div>
                          <div
                            class="mark-reject-button-item"
                            @click="reject(item.fromUser.id, item.id)"
                          >
                            拒绝
                          </div>
                        </div>
                        <div v-else class="has_read_button" @click="markRead(item.id)">已读</div>
                      </template>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_REVIEWED' && item.approved">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您发布的帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已审核通过
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_REVIEWED' && item.approved === false">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您发布的帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      已被管理员拒绝
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_FEATURED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      您的文章
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      被收录为精选
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'DONATION'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/users/${item.fromUser.id}`"
                      >
                        {{ item.fromUser.username }}
                      </NuxtLink>
                      在帖子
                      <NuxtLink
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </NuxtLink>
                      打赏了你
                      <template v-if="item.content"> ，获得 {{ item.content }} 积分 </template>
                    </NotificationContainer>
                  </template>
                  <template v-else-if="item.type === 'POST_DELETED'">
                    <NotificationContainer :item="item" :markRead="markRead">
                      管理员
                      <template v-if="item.fromUser">
                        <NuxtLink
                          class="notif-content-text"
                          @click="markRead(item.id)"
                          :to="`/users/${item.fromUser.id}`"
                        >
                          {{ item.fromUser.username }}
                        </NuxtLink>
                      </template>
                      删除了您的帖子
                      <span class="notif-content-text">
                        <span v-html="stripMarkdownWithTiebaMoji(item.content, 500)"></span>
                      </span>
                    </NotificationContainer>
                  </template>
                  <template v-else>
                    <NotificationContainer :item="item" :markRead="markRead">
                      {{ formatType(item.type) }}
                    </NotificationContainer>
                  </template>
                </span>
                <span class="notif-time">{{ TimeManager.format(item.createdAt) }}</span>
              </div>
            </template>
          </BaseTimeline>
          <InfiniteLoadMore :key="selectedTab" :on-load="loadMore" :pause="isLoadingMessage" />
        </div>
      </template>
    </BaseTabs>
  </div>
</template>

<script setup>
import { ref, watch, onActivated } from 'vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import BaseTimeline from '~/components/BaseTimeline.vue'
import NotificationContainer from '~/components/NotificationContainer.vue'
import InfiniteLoadMore from '~/components/InfiniteLoadMore.vue'
import BaseTabs from '~/components/BaseTabs.vue'
import { toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import { stripMarkdownWithTiebaMoji } from '~/utils/markdown'
import {
  fetchNotifications,
  fetchUnreadCount,
  isLoadingMessage,
  markNotificationRead,
  notifications,
  markAllRead,
  hasMore,
  fetchNotificationPreferences,
  updateNotificationPreference,
  fetchEmailNotificationPreferences,
  updateEmailNotificationPreference,
} from '~/utils/notification'
import TimeManager from '~/utils/time'
import BaseSwitch from '~/components/BaseSwitch.vue'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const route = useRoute()
const selectedTab = ref(
  ['all', 'unread', 'control'].includes(route.query.tab) ? route.query.tab : 'unread',
)
const tabs = [
  { key: 'all', label: '消息' },
  { key: 'unread', label: '未读' },
  { key: 'control', label: '消息设置' },
]
const notificationPrefs = ref([])
const emailPrefs = ref([])
const page = ref(0)
const pageSize = 30

const loadMore = async () => {
  if (!hasMore.value) return true
  page.value++
  await fetchNotifications({
    page: page.value,
    size: pageSize,
    unread: selectedTab.value === 'unread',
    append: true,
  })
  return !hasMore.value
}

watch(selectedTab, async (tab) => {
  page.value = 0
  await fetchNotifications({ page: 0, size: pageSize, unread: tab === 'unread' })
})

const fetchPrefs = async () => {
  notificationPrefs.value = await fetchNotificationPreferences()
}

const fetchEmailPrefs = async () => {
  emailPrefs.value = await fetchEmailNotificationPreferences()
}

const togglePref = async (pref, value) => {
  const ok = await updateNotificationPreference(pref.type, value)
  if (ok) {
    pref.enabled = value
    await fetchNotifications({
      page: page.value,
      size: pageSize,
      unread: selectedTab.value === 'unread',
    })
    await fetchUnreadCount()
  } else {
    toast.error('操作失败')
  }
}

const toggleEmailPref = async (pref, value) => {
  const ok = await updateEmailNotificationPreference(pref.type, value)
  if (ok) {
    pref.enabled = value
  } else {
    toast.error('操作失败')
  }
}

const markRead = async (id) => {
  markNotificationRead(id)
  if (selectedTab.value === 'unread') {
    const index = notifications.value.findIndex((n) => n.id === id)
    if (index !== -1) notifications.value.splice(index, 1)
  }
}

const approve = async (id, nid) => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/users/${id}/approve`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    markRead(nid)
    toast.success('已同意')
  } else {
    toast.error('操作失败')
  }
}

const reject = async (id, nid) => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/users/${id}/reject`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    markRead(nid)
    toast.success('已拒绝')
  } else {
    toast.error('操作失败')
  }
}

const formatType = (t) => {
  switch (t) {
    case 'POST_VIEWED':
      return '帖子被查看'
    case 'COMMENT_REPLY':
      return '有人回复了你'
    case 'REACTION':
      return '有人点赞'
    case 'POST_REVIEW_REQUEST':
      return '帖子待审核'
    case 'POST_REVIEWED':
      return '帖子审核结果'
    case 'POST_UPDATED':
      return '关注的帖子有新评论'
    case 'FOLLOWED_POST':
      return '关注的用户发布了新文章'
    case 'POST_SUBSCRIBED':
      return '有人订阅了你的文章'
    case 'POST_UNSUBSCRIBED':
      return '有人取消订阅你的文章'
    case 'USER_FOLLOWED':
      return '有人关注了你'
    case 'USER_UNFOLLOWED':
      return '有人取消关注你'
    case 'USER_ACTIVITY':
      return '关注的用户有新动态'
    case 'MENTION':
      return '有人提到了你'
    case 'REGISTER_REQUEST':
      return '有人申请注册'
    case 'ACTIVITY_REDEEM':
      return '有人申请兑换奶茶'
    case 'POINT_REDEEM':
      return '有人申请积分兑换'
    case 'DONATION':
      return '有人打赏了你'
    case 'LOTTERY_WIN':
      return '抽奖中奖了'
    case 'LOTTERY_DRAW':
      return '抽奖已开奖'
    case 'POST_DELETED':
      return '帖子被删除'
    case 'POST_FEATURED':
      return '文章被精选'
    case 'POLL_VOTE':
      return '有人参与你的投票'
    case 'POLL_RESULT_OWNER':
      return '发布的投票结果已公布'
    case 'POLL_RESULT_PARTICIPANT':
      return '参与的投票结果已公布'
    case 'CATEGORY_PROPOSAL_RESULT_OWNER':
      return '分类提案结果已公布'
    case 'CATEGORY_PROPOSAL_RESULT_PARTICIPANT':
      return '参与的分类提案结果已公布'
    default:
      return t
  }
}

const isAdmin = computed(() => authState.role === 'ADMIN')

const needAdminSet = new Set([
  'POST_REVIEW_REQUEST',
  'REGISTER_REQUEST',
  'POINT_REDEEM',
  'ACTIVITY_REDEEM',
])

const canShowNotification = (type) => {
  return !needAdminSet.has(type) || isAdmin.value
}

onActivated(async () => {
  page.value = 0
  await fetchNotifications({ page: 0, size: pageSize, unread: selectedTab.value === 'unread' })
  fetchPrefs()
  fetchEmailPrefs()
})
</script>

<style scoped>
.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.message-page {
  background-color: var(--background-color);
  overflow-x: hidden;
}

.message-page-header-right {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.message-page-header-right-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
  color: var(--primary-color);
  padding-right: 10px;
  gap: 5px;
}

.message-page-header-right-item-button-icon {
  font-size: 12px;
}

.message-page-header-right-item-button-text {
  font-size: 12px;
}

.message-page-header-right-item-button-text:hover {
  text-decoration: underline;
}

.timeline-container {
  padding: 10px 20px;
  height: 100%;
}

.notif-content {
  display: flex;
  flex-direction: column;
  margin-bottom: 30px;
  position: relative;
}

.notif-content.read {
  opacity: 0.7;
}

.unread-dot {
  position: absolute;
  left: -10px;
  top: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.notif-type {
  font-weight: bold;
}

.notif-time {
  font-size: 12px;
  color: gray;
}

.notif-content-text {
  font-weight: bold;
  color: var(--primary-color) !important;
  text-decoration: none !important;
  word-break: break-all;
}

.optional-buttons {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.mark-approve-button-item {
  color: green;
  cursor: pointer;
}

.mark-reject-button-item {
  color: red;
  cursor: pointer;
}

.mark-approve-button-item:hover {
  text-decoration: underline;
}

.mark-reject-button-item:hover {
  text-decoration: underline;
}

.has_read_button {
  font-size: 12px;
}

.notif-content-text:hover {
  color: var(--primary-color) !important;
  text-decoration: underline !important;
}

.notif-user {
  font-weight: bold;
  color: var(--text-color);
}

.message-control-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
}

.message-control-container {
  padding: 20px;
}

.message-control-item-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-control-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 200px;
}

.message-control-item-label {
  font-size: 14px;
}

@media (max-width: 768px) {
  .has_read_button {
    display: none;
  }
}
</style>
