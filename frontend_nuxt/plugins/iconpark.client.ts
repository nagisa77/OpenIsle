import { defineNuxtPlugin } from 'nuxt/app'
import {
  Pin,
  Fireworks,
  Gift,
  RankingList,
  Star,
  Edit,
  HashtagKey,
  Remind,
  Info,
  ChartLine,
  Finance,
  Up,
  Down,
  TagOne,
  MedalOne,
  Next,
  DropDownList,
  MoreOne,
  SunOne,
  Moon,
  ComputerOne,
  Comment,
  Link,
  SlyFaceWhitSmile,
  Like,
  ApplicationMenu,
  Search,
  Copy,
  Loading,
  Rss,
  MessageEmoji,
  AddUser,
  ReduceUser,
  MessageOne,
  AlarmClock,
  Bookmark,
  ArrowLeft,
} from '@icon-park/vue-next'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.component('Pin', Pin)
  nuxtApp.vueApp.component('Fireworks', Fireworks)
  nuxtApp.vueApp.component('Gift', Gift)
  nuxtApp.vueApp.component('RankingList', RankingList)
  nuxtApp.vueApp.component('Star', Star)
  nuxtApp.vueApp.component('Edit', Edit)
  nuxtApp.vueApp.component('HashtagKey', HashtagKey)
  nuxtApp.vueApp.component('Remind', Remind)
  nuxtApp.vueApp.component('Info', Info)
  nuxtApp.vueApp.component('ChartLine', ChartLine)
  nuxtApp.vueApp.component('Finance', Finance)
  nuxtApp.vueApp.component('Up', Up)
  nuxtApp.vueApp.component('Down', Down)
  nuxtApp.vueApp.component('TagOne', TagOne)
  nuxtApp.vueApp.component('MedalOne', MedalOne)
  nuxtApp.vueApp.component('Next', Next)
  nuxtApp.vueApp.component('DropDownList', DropDownList)
  nuxtApp.vueApp.component('MoreOne', MoreOne)
  nuxtApp.vueApp.component('SunOne', SunOne)
  nuxtApp.vueApp.component('Moon', Moon)
  nuxtApp.vueApp.component('ComputerOne', ComputerOne)
  nuxtApp.vueApp.component('CommentIcon', Comment)
  nuxtApp.vueApp.component('LinkIcon', Link)
  nuxtApp.vueApp.component('SlyFaceWhitSmile', SlyFaceWhitSmile)
  nuxtApp.vueApp.component('Like', Like)
  nuxtApp.vueApp.component('ApplicationMenu', ApplicationMenu)
  nuxtApp.vueApp.component('SearchIcon', Search)
  nuxtApp.vueApp.component('Copy', Copy)
  nuxtApp.vueApp.component('Loading', Loading)
  nuxtApp.vueApp.component('Rss', Rss)
  nuxtApp.vueApp.component('MessageEmoji', MessageEmoji)
  nuxtApp.vueApp.component('AddUser', AddUser)
  nuxtApp.vueApp.component('ReduceUser', ReduceUser)
  nuxtApp.vueApp.component('MessageOne', MessageOne)
  nuxtApp.vueApp.component('AlarmClock', AlarmClock)
  nuxtApp.vueApp.component('Bookmark', Bookmark)
  nuxtApp.vueApp.component('ArrowLeft', ArrowLeft)
})
