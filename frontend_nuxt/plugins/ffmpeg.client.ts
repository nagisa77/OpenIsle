import { FFmpeg } from '@ffmpeg/ffmpeg'
import { toBlobURL } from '@ffmpeg/util'
import { defineNuxtPlugin, useRuntimeConfig } from 'nuxt/app'

let ffmpeg: FFmpeg | null = null

export default defineNuxtPlugin(() => {
  return {
    provide: {
      ffmpeg: async () => {
        if (ffmpeg) return ffmpeg
        ffmpeg = new FFmpeg()
        const base = '~/assets/js/ffmpeg/core-mt/dist/umd'
        await ffmpeg.load({
          coreURL: await toBlobURL(`${base}/ffmpeg-core.js`, 'text/javascript'),
          wasmURL: await toBlobURL(`${base}/ffmpeg-core.wasm`, 'application/wasm'),
          workerURL: await toBlobURL(`${base}/ffmpeg-core.worker.js`, 'text/javascript'),
        })

        return ffmpeg
      },
    },
  }
})
