import MP4Box from 'mp4box'

// 检查 WebCodecs 支持
export function isWebCodecSupported() {
  return typeof window !== 'undefined' && typeof window.VideoEncoder !== 'undefined'
}

// 使用 WebCodecs + MP4Box.js 压缩视频
export async function compressVideoWithWebCodecs(file, opts = {}) {
  const { onProgress = () => {}, width = 720, bitrate = 1_000_000 } = opts

  if (!isWebCodecSupported()) {
    throw new Error('当前浏览器不支持 WebCodecs')
  }

  onProgress({ stage: 'initializing', progress: 0 })

  // 加载原始视频
  const url = URL.createObjectURL(file)
  const video = document.createElement('video')
  video.src = url
  video.muted = true
  await video.play().catch(() => {})
  video.pause()
  await new Promise((resolve) => {
    if (video.readyState >= 2) resolve()
    else video.onloadedmetadata = () => resolve()
  })

  const targetWidth = width
  const targetHeight = Math.round((video.videoHeight / video.videoWidth) * width)
  const canvas = document.createElement('canvas')
  canvas.width = targetWidth
  canvas.height = targetHeight
  const ctx = canvas.getContext('2d')

  const chunks = []
  const encoder = new VideoEncoder({
    output: (chunk) => {
      chunks.push(chunk)
    },
    error: (e) => {
      throw e
    },
  })
  encoder.configure({
    codec: 'avc1.42001E',
    width: targetWidth,
    height: targetHeight,
    bitrate,
    framerate: 30,
  })

  const duration = video.duration
  const frameCount = Math.floor(duration * 30)
  for (let i = 0; i < frameCount; i++) {
    video.currentTime = i / 30
    await new Promise((res) => (video.onseeked = res))
    ctx.drawImage(video, 0, 0, targetWidth, targetHeight)
    const bitmap = await createImageBitmap(canvas)
    const frame = new VideoFrame(bitmap, { timestamp: (i / 30) * 1000000 })
    encoder.encode(frame)
    frame.close()
    bitmap.close()
    onProgress({ stage: 'compressing', progress: Math.round(((i + 1) / frameCount) * 80) })
  }

  await encoder.flush()
  onProgress({ stage: 'finalizing', progress: 90 })

  const mp4box = MP4Box.createFile()
  const track = mp4box.addTrack({
    timescale: 1000,
    width: targetWidth,
    height: targetHeight,
  })

  let dts = 0
  chunks.forEach((chunk) => {
    const data = new Uint8Array(chunk.byteLength)
    chunk.copyTo(data)
    mp4box.addSample(track, data.buffer, {
      duration: chunk.duration ? chunk.duration / 1000 : 33,
      dts,
      cts: dts,
      is_sync: chunk.type === 'key',
    })
    dts += chunk.duration ? chunk.duration / 1000 : 33
  })

  const arrayBuffer = mp4box.flush()
  const outputFile = new File([arrayBuffer], file.name.replace(/\.[^.]+$/, '.mp4'), {
    type: 'video/mp4',
  })
  onProgress({ stage: 'completed', progress: 100 })
  URL.revokeObjectURL(url)
  return outputFile
}
