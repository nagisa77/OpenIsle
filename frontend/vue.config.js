const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
    // publicPath: process.env.NODE_ENV === 'production'
    // // ? '/ai-cv/' 
    // ? '/OpenIsle/' 
    // : '/',
  transpileDependencies: true,
  configureWebpack: {
    devtool: 'source-map'
  },
})
