// markdown.js
import hljs from 'highlight.js/lib/common'
import MarkdownIt from 'markdown-it'
import sanitizeHtml from 'sanitize-html'
import { toast } from '../main'
import { tiebaEmoji } from './tiebaEmoji'

// 动态切换 hljs 主题（保持你原有逻辑）
if (typeof window !== 'undefined') {
  const theme =
    document.documentElement.dataset.theme ||
    (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  if (theme === 'dark') {
    import('highlight.js/styles/atom-one-dark.css')
  } else {
    import('highlight.js/styles/atom-one-light.css')
  }
}

/** @section 自定义插件：@mention */
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
        ['rel', 'noopener noreferrer'],
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

/** @section 自定义插件：贴吧表情 :tieba123: */
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

/** @section 链接外开 */
function linkPlugin(md) {
  const defaultRender =
    md.renderer.rules.link_open ||
    function (tokens, idx, options, env, self) {
      return self.renderToken(tokens, idx, options)
    }

  md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
    const token = tokens[idx]
    const hrefIndex = token.attrIndex('href')

    if (hrefIndex >= 0) {
      const href = token.attrs[hrefIndex][1]
      if (href.startsWith('http://') || href.startsWith('https://')) {
        token.attrPush(['target', '_blank'])
        token.attrPush(['rel', 'noopener noreferrer'])
      }
    }

    return defaultRender(tokens, idx, options, env, self)
  }
}

/** @section MarkdownIt 实例：开启 HTML，但配合强净化 */
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight: (str, lang) => {
    if (lang === 'mermaid') {
      return `<pre class="mermaid">${str}</pre>`
    }
    let code = ''
    if (lang && hljs.getLanguage(lang)) {
      code = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
    } else {
      code = hljs.highlightAuto(str).value
    }
    const lineNumbers = code
      .trim()
      .split('\n')
      .map(() => `<div class="line-number"></div>`)
    return `<pre class="code-block"><button class="copy-code-btn">Copy</button><div class="line-numbers">${lineNumbers.join('')}</div><code class="hljs language-${lang || ''}">${code.trim()}</code></pre>`
  },
})

const md2TextRender = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})

md.use(mentionPlugin)
md.use(tiebaEmojiPlugin)
md.use(linkPlugin)

/** @section sanitize-html 配置：只白名单需要的标签/属性/类名 */
const SANITIZE_CFG = {
  // 允许的标签（包含你代码块里用到的 button/div）
  allowedTags: [
    'a',
    'p',
    'div',
    'span',
    'pre',
    'code',
    'button',
    'img',
    'br',
    'hr',
    'blockquote',
    'strong',
    'em',
    'ul',
    'ol',
    'li',
    'h1',
    'h2',
    'h3',
    'h4',
    'h5',
    'h6',
    'table',
    'thead',
    'tbody',
    'tr',
    'td',
    'th',
    'video',
    'source',
    'iframe',
  ],
  // 允许的属性
  allowedAttributes: {
    a: ['href', 'name', 'target', 'rel', 'class'],
    img: ['src', 'alt', 'title', 'width', 'height', 'class'],
    div: ['class'],
    span: ['class'],
    pre: ['class'],
    code: ['class'],
    button: ['class'],
    video: [
      'controls',
      'autoplay',
      'muted',
      'loop',
      'playsinline',
      'poster',
      'preload',
      'width',
      'height',
      'crossorigin',
    ],
    source: ['src', 'type'],
    iframe: [
      'src',
      'title',
      'width',
      'height',
      'allow',
      'allowfullscreen',
      'frameborder',
      'referrerpolicy',
    ],
  },
  // 允许的类名（保留你的样式钩子）
  allowedClasses: {
    a: ['mention-link'],
    img: ['emoji'],
    pre: ['code-block', 'mermaid'],
    div: ['line-numbers', 'line-number'],
    code: ['hljs', /^language-/],
    button: ['copy-code-btn'],
  },
  // 允许的协议（视频可能是 blob: / data:）
  allowedSchemes: ['http', 'https', 'data', 'blob'],
  allowProtocolRelative: false,
  // 统一移除所有 on* 事件、style 等（默认就会清理）
  transformTags: {
    // 没写 controls 的 video，强制加上（避免静默自动播放）
    video: (tagName, attribs) => {
      const attrs = { ...attribs }
      if (!('controls' in attrs)) attrs.controls = 'controls'
      // 安全建议：若允许 autoplay，默认要求 muted
      if ('autoplay' in attrs && !('muted' in attrs)) {
        attrs.muted = 'muted'
      }
      return { tagName, attribs: attrs }
    },
  },
}

/** @section 渲染 & 事件 */
export function renderMarkdown(text) {
  const raw = md.render(text || '')
  const html = sanitizeHtml(raw, SANITIZE_CFG)
  if (typeof window !== 'undefined') {
    setTimeout(async () => {
      const mermaid = await import('mermaid')
      mermaid.default.initialize({ startOnLoad: false })
      mermaid.default.run({ nodes: document.querySelectorAll('.mermaid') })
    })
  }
  return html
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

/** @section 纯文本提取（保持你原有“统一正则法”） */
export function stripMarkdown(text) {
  const html = md2TextRender.render(text || '')
  let plainText = html.replace(/<[^>]+>/g, '')
  plainText = plainText
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[ \t]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
  return plainText
}

export function stripMarkdownLength(text, length) {
  const plain = stripMarkdown(text)
  if (!length || plain.length <= length) {
    return plain
  }
  return plain.slice(0, length) + '...'
}

// 朴素文本带贴吧表情
export function stripMarkdownWithTiebaMoji(text, length){
    if (!text) return ''

    // Markdown 转成纯文本
    const plain = stripMarkdown(text)
    // 替换 :tieba123: 为 <img>
    const withEmoji = plain.replace(/:tieba(\d+):/g, (match, num) => {
      const key = `tieba${num}`
      const file = tiebaEmoji[key]
      return file
        ? `<img loading="lazy" class="emoji" src="${file}" alt="${key}">`
        : match // 没有匹配到图片则保留原样
    })

    // 截断纯文本长度（防止撑太长）
    const truncated = withEmoji.length > length ? withEmoji.slice(0, length) + '...' : withEmoji
    return truncated

}
