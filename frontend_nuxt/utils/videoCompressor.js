/**
 * 基于 WebCodecs + MP4Box.js 的视频压缩工具
 * 专为现代浏览器 (Chrome/Safari) 优化
 */

import { UPLOAD_CONFIG } from '../config/uploadConfig.js'
import { compressVideoWithWebCodecs, isWebCodecSupported } from './webcodecVideoCompressor.js'

// 导出配置供外部使用
export const VIDEO_CONFIG = UPLOAD_CONFIG.VIDEO

/**
 * 检查文件大小是否超出限制
 */
export function checkFileSize(file) {
  return {
    isValid: file.size <= VIDEO_CONFIG.MAX_SIZE,
    actualSize: file.size,
    maxSize: VIDEO_CONFIG.MAX_SIZE,
    needsCompression: file.size > VIDEO_CONFIG.TARGET_SIZE,
  }
}

/**
 * 格式化文件大小显示
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

/**
 * 压缩视频文件 - 使用 WebCodecs
 */
export async function compressVideo(file, onProgress = () => {}) {
  // 检查是否需要压缩
  const sizeCheck = checkFileSize(file)
  if (!sizeCheck.needsCompression) {
    onProgress({ stage: 'completed', progress: 100 })
    return file
  }

  // 检查 WebCodecs 支持
  if (!isWebCodecSupported()) {
    throw new Error('当前浏览器不支持视频压缩功能，请使用支持 WebCodecs 的浏览器')
  }

  try {
    return await compressVideoWithWebCodecs(file, { onProgress })
  } catch (error) {
    console.error('WebCodecs 压缩失败:', error)
    throw new Error(`视频压缩失败: ${error.message}`)
  }
}

/**
 * 预加载 WebCodecs（可选的性能优化）
 */
export async function preloadVideoCompressor() {
  try {
    if (!isWebCodecSupported()) {
      throw new Error('当前浏览器不支持 WebCodecs')
    }
    return { success: true, message: 'WebCodecs 已就绪' }
  } catch (error) {
    console.warn('WebCodecs 检测失败:', error)
    return { success: false, error: error.message }
  }
}
