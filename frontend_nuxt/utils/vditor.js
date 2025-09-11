import Vditor from 'vditor'
import { getToken, authState } from './auth'
import { searchUsers, fetchFollowings, fetchAdmins } from './user'
import { tiebaEmoji } from './tiebaEmoji'
import vditorPostCitation from './vditorPostCitation.js'
import { checkFileSize, formatFileSize, compressVideo, VIDEO_CONFIG } from './videoCompressor.js'
import { UPLOAD_CONFIG } from '../config/uploadConfig.js'

export function getEditorTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'classic'
}

export function getPreviewTheme() {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export function createVditor(editorId, options = {}) {
  const { placeholder = '', preview = {}, input, after } = options
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  const WEBSITE_BASE_URL = config.public.websiteBaseUrl

  const fetchMentions = async (value) => {
    if (!value) {
      const [followings, admins] = await Promise.all([
        fetchFollowings(authState.username),
        fetchAdmins(),
      ])
      const combined = [...followings, ...admins]
      const seen = new Set()
      return combined.filter((u) => {
        if (seen.has(u.id)) return false
        seen.add(u.id)
        return true
      })
    }
    return searchUsers(value)
  }

  const isMobile = window.innerWidth <= 768
  const toolbar = isMobile
    ? ['emoji', 'upload']
    : [
        'emoji',
        'bold',
        'italic',
        'strike',
        '|',
        'list',
        'line',
        'quote',
        'code',
        'inline-code',
        '|',
        'undo',
        'redo',
        '|',
        'link',
        'upload',
      ]

  let vditor
  vditor = new Vditor(editorId, {
    placeholder,
    height: 'auto',
    theme: getEditorTheme(),
    preview: Object.assign(
      {
        theme: { current: getPreviewTheme() },
      },
      preview,
    ),
    hint: {
      emoji: tiebaEmoji,
      extend: [
        {
          key: '@',
          hint: async (key) => {
            const list = await fetchMentions(key)
            return list.map((u) => ({
              value: `@[${u.username}]`,
              html: `<BaseImage src="${u.avatar}" /> @${u.username}`,
            }))
          },
        },
        vditorPostCitation(API_BASE_URL, WEBSITE_BASE_URL),
      ],
    },
    cdn: 'https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/vditor',
    toolbar,
    upload: {
      accept: 'image/*,video/*',
      multiple: false,
      handler: async (files) => {
        const file = files[0]
        const ext = file.name.split('.').pop().toLowerCase()
        const videoExts = ['mp4', 'webm', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'm4v', 'ogv']
        const isVideo = videoExts.includes(ext)

        // 检查文件大小
        const sizeCheck = checkFileSize(file)
        if (!sizeCheck.isValid) {
          console.log(
            '文件大小不能超过',
            formatFileSize(sizeCheck.maxSize),
            '，当前文件',
            formatFileSize(sizeCheck.actualSize),
          )
          vditor.tip(
            `文件大小不能超过 ${formatFileSize(sizeCheck.maxSize)}，当前文件 ${formatFileSize(sizeCheck.actualSize)}`,
            3000,
          )
          return '文件过大'
        }

        let processedFile = file

        // 如果是视频文件且需要压缩
        if (isVideo && sizeCheck.needsCompression) {
          try {
            vditor.tip('视频压缩中...', 0)
            vditor.disabled()

            // 使用 WebCodecs 压缩视频
            processedFile = await compressVideo(file, (progress) => {
              const messages = {
                initializing: '初始化编码器',
                preparing: '准备压缩',
                analyzing: '分析视频',
                compressing: '压缩中',
                finalizing: '完成压缩',
                completed: '压缩完成',
              }
              const message = messages[progress.stage] || progress.stage
              vditor.tip(`${message} ${progress.progress}%`, 0)
            })

            const originalSize = formatFileSize(file.size)
            const compressedSize = formatFileSize(processedFile.size)
            const savings = Math.round((1 - processedFile.size / file.size) * 100)

            vditor.tip(`压缩完成！${originalSize} → ${compressedSize} (节省 ${savings}%)`, 2000)
            // 压缩成功但仍然超过最大限制，则阻止上传
            if (processedFile.size > VIDEO_CONFIG.MAX_SIZE) {
              vditor.tip(
                `压缩后仍超过限制 (${formatFileSize(VIDEO_CONFIG.MAX_SIZE)}). 请降低分辨率或码率后再上传。`,
                4000,
              )
              vditor.enable()
              return '压缩后仍超过大小限制'
            }
          } catch (error) {
            // 压缩失败时，如果原文件超过最大限制，则阻止上传
            if (file.size > VIDEO_CONFIG.MAX_SIZE) {
              vditor.tip(
                `视频压缩失败，且文件超过限制 (${formatFileSize(VIDEO_CONFIG.MAX_SIZE)}). 请先压缩后再上传。`,
                4000,
              )
              vditor.enable()
              return '视频压缩失败且文件过大'
            }
            vditor.tip('视频压缩失败，将尝试上传原文件', 3000)
            processedFile = file
          }
        }

        vditor.tip('文件上传中', 0)
        vditor.disabled()
        const res = await fetch(
          `${API_BASE_URL}/api/upload/presign?filename=${encodeURIComponent(processedFile.name)}`,
          { headers: { Authorization: `Bearer ${getToken()}` } },
        )
        if (!res.ok) {
          vditor.enable()
          vditor.tip('获取上传地址失败')
          return '获取上传地址失败'
        }
        const info = await res.json()
        const put = await fetch(info.uploadUrl, { method: 'PUT', body: processedFile })
        if (!put.ok) {
          vditor.enable()
          vditor.tip('上传失败')
          return '上传失败'
        }

        const imageExts = [
          'apng',
          'bmp',
          'gif',
          'ico',
          'cur',
          'jpg',
          'jpeg',
          'jfif',
          'pjp',
          'pjpeg',
          'png',
          'svg',
          'webp',
        ]
        const audioExts = ['wav', 'mp3', 'ogg']
        let md
        if (imageExts.includes(ext)) {
          md = `![${file.name}](${info.fileUrl})`
        } else if (audioExts.includes(ext)) {
          md = `<audio controls="controls" src="${info.fileUrl}"></audio>`
        } else if (videoExts.includes(ext)) {
          md = `<video width="600" controls>\n  <source src="${info.fileUrl}" type="video/${ext}">\n  你的浏览器不支持 video 标签。\n</video>`
        } else {
          md = `[${file.name}](${info.fileUrl})`
        }
        vditor.insertValue(md + '\n')
        vditor.enable()
        vditor.tip('上传成功')
        return null
      },
    },
    // upload: {
    //   fieldName: 'file',
    //   url: `${API_BASE_URL}/api/upload`,
    //   accept: 'image/*,video/*',
    //   multiple: false,
    //   headers: { Authorization: `Bearer ${getToken()}` },
    //   format(files, responseText) {
    //     const res = JSON.parse(responseText)
    //     if (res.code === 0) {
    //       return JSON.stringify({
    //         code: 0,
    //         msg: '',
    //         data: {
    //           errFiles: [],
    //           succMap: { [files[0].name]: res.data.url }
    //         }
    //       })
    //     } else {
    //       return JSON.stringify({
    //         code: 1,
    //         msg: '上传失败',
    //         data: { errFiles: files.map(f => f.name), succMap: {} }
    //       })
    //     }
    //   }
    // },
    toolbarConfig: { pin: true },
    cache: { enable: false },
    input,
    after,
  })

  return vditor
}
