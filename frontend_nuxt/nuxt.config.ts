import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: true,
  // Ensure Vditor styles load before our overrides in global.css
  css: ['vditor/dist/index.css', '~/assets/global.css'],
  app: {
    head: {
      script: [
        {
          tagPriority: 'high',
          innerHTML: `
            (function () {
              try {
                const mode = localStorage.getItem('theme-mode');
                const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
                const theme = mode === 'dark' || mode === 'light' ? mode : (prefersDark ? 'dark' : 'light');
                document.documentElement.dataset.theme = theme;
              } catch (e) {}
            })();
          `,
        },
      ],
      link: [
        {
          rel: 'icon',
          type: 'image/x-icon',
          href: '/favicon.ico',
        },
        {
          rel: 'apple-touch-icon',
          href: '/apple-touch-icon.png',
        },
        {
          rel: 'manifest',
          href: '/manifest.webmanifest',
        },
        {
          rel: 'stylesheet',
          href: 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css',
          referrerpolicy: 'no-referrer',
        },
      ],
    },
  },
  vue: {
    compilerOptions: {
      isCustomElement: (tag) => ['l-hatch', 'l-hatch-spinner'].includes(tag),
    },
  },
})
