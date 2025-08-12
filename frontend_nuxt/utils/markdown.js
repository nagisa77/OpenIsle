import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import MarkdownIt from 'markdown-it'
import { toast } from '../main'
import { tiebaEmoji } from './tiebaEmoji'

function mentionPlugin(md) {
  const mentionReg = /^@\[([^\]]+)\]/
  function mention(state, silent) {
    const pos = state.pos
    if (state.src.charCodeAt(pos) !== 0x40) return false
    const match = mentionReg.exec(state.src.slice(pos))
    if (!match) return false
    if (!silent) {
      const tokenOpen = state.push('link_open', 'a', 1)
      tokenOpen.attrs = [
        ['href', `/users/${match[1]}`],
        ['target', '_blank'],
        ['class', 'mention-link'],
      ]
      const text = state.push('text', '', 0)
      text.content = `@${match[1]}`
      state.push('link_close', 'a', -1)
    }
    state.pos += match[0].length
    return true
  }
  md.inline.ruler.before('emphasis', 'mention', mention)
}

function tiebaEmojiPlugin(md) {
  md.renderer.rules['tieba-emoji'] = (tokens, idx) => {
    const name = tokens[idx].content
    const file = tiebaEmoji[name]
    return `<img class="emoji" src="${file}" alt="${name}">`
  }
  md.inline.ruler.before('emphasis', 'tieba-emoji', (state, silent) => {
    const pos = state.pos
    if (state.src.charCodeAt(pos) !== 0x3a) return false
    const match = state.src.slice(pos).match(/^:tieba(\d+):/)
    if (!match) return false
    const key = `tieba${match[1]}`
    if (!tiebaEmoji[key]) return false
    if (!silent) {
      const token = state.push('tieba-emoji', '', 0)
      token.content = key
    }
    state.pos += match[0].length
    return true
  })
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: (str, lang) => {
    let code = ''
    if (lang && hljs.getLanguage(lang)) {
      code = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
    } else {
      code = hljs.highlightAuto(str).value
    }
    return `<pre class="code-block"><button class="copy-code-btn">Copy</button><code class="hljs language-${lang || ''}">${code}</code></pre>`
  },
})

md.use(mentionPlugin)
md.use(tiebaEmojiPlugin)

export function renderMarkdown(text) {
  return md.render(text || '')
}

export function handleMarkdownClick(e) {
  if (e.target.classList.contains('copy-code-btn')) {
    const pre = e.target.closest('pre')
    const codeEl = pre && pre.querySelector('code')
    if (codeEl) {
      navigator.clipboard.writeText(codeEl.innerText).then(() => {
        toast.success('已复制')
      })
    }
  }
}

export function stripMarkdown(text) {
  const html = md.render(text || '')

  // 统一使用正则表达式方法，确保服务端和客户端行为一致
  let plainText = html.replace(/<[^>]+>/g, '')

  // 标准化空白字符处理
  plainText = plainText
    .replace(/\r\n/g, '\n') // Windows换行符转为Unix格式
    .replace(/\r/g, '\n') // 旧Mac换行符转为Unix格式
    .replace(/[ \t]+/g, ' ') // 合并空格和制表符为单个空格
    .replace(/\n{3,}/g, '\n\n') // 最多保留两个连续换行（一个空行）
    .trim()

  return plainText
}

export function stripMarkdownLength(text, length) {
  const plain = stripMarkdown(text)
  if (!length || plain.length <= length) {
    return plain
  }
  // 截断并加省略号
  return plain.slice(0, length) + '...'
}
