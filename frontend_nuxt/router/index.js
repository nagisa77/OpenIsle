export default {
  push(path) {
    if (process.client) {
      window.location.href = path
    }
  },
}
