import { defineNuxtConfig } from 'nuxt/config'
export default defineNuxtConfig({
  devServer: {
    host: '0.0.0.0',
    port: 3000,
  },
  ssr: true,
  modules: ['@nuxt/image'],
  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || '',
      websocketUrl: process.env.NUXT_PUBLIC_WEBSOCKET_URL || '',
      websiteBaseUrl: process.env.NUXT_PUBLIC_WEBSITE_BASE_URL || '',
      googleClientId: process.env.NUXT_PUBLIC_GOOGLE_CLIENT_ID || '',
      githubClientId: process.env.NUXT_PUBLIC_GITHUB_CLIENT_ID || '',
      discordClientId: process.env.NUXT_PUBLIC_DISCORD_CLIENT_ID || '',
      twitterClientId: process.env.NUXT_PUBLIC_TWITTER_CLIENT_ID || '',
      telegramBotId: process.env.NUXT_PUBLIC_TELEGRAM_BOT_ID || '',
    },
  },
  css: [
    'vditor/dist/index.css',
    '~/assets/fonts.css',
    '~/assets/global.css',
    '@icon-park/vue-next/styles/index.css',
  ],
  app: {
    pageTransition: { name: 'page', mode: 'out-in' },
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

                
                let themeColor = '#fff';
                let themeStatus = 'default';
                if (theme === 'dark') {
                  themeColor = '#333';
                  themeStatus = 'black-translucent';
                } else {
                  themeColor = '#ffffff';
                  themeStatus = 'default';
                }
                
                const androidMeta = document.createElement('meta');
                androidMeta.name = 'theme-color';
                androidMeta.content = themeColor;
                
                const iosMeta = document.createElement('meta');
                iosMeta.name = 'apple-mobile-web-app-status-bar-style';
                iosMeta.content = themeStatus;

                document.head.appendChild(androidMeta);
                document.head.appendChild(iosMeta);
              } catch (e) {
                console.warn('Theme initialization failed:', e);
              }
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
        // {
        //   rel: 'stylesheet',
        //   href: 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css',
        //   referrerpolicy: 'no-referrer',
        // },
      ],
    },
    baseURL: '/',
    buildAssetsDir: '/_nuxt/',
  },
  vue: {
    compilerOptions: {
      isCustomElement: (tag) => ['l-hatch', 'l-hatch-spinner'].includes(tag),
    },
  },
  vite: {
    build: {
      // increase warning limit and split large libraries into separate chunks
      // chunkSizeWarningLimit: 1024,
      // rollupOptions: {
      //   output: {
      //     manualChunks(id) {
      //       if (id.includes('node_modules')) {
      //         if (id.includes('vditor')) {
      //           return 'vditor'
      //         }
      //         if (id.includes('echarts')) {
      //           return 'echarts'
      //         }
      //         if (id.includes('highlight.js')) {
      //           return 'highlight'
      //         }
      //       }
      //     },
      //   },
      // },
    },
  },
})
