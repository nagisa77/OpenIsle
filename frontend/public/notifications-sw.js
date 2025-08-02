self.addEventListener('push', function(event) {
  let payload = { body: 'New notification', url: '/' }
  try {
    if (event.data) payload = JSON.parse(event.data.text())
  } catch (e) {
    if (event.data) payload.body = event.data.text()
  }
  event.waitUntil(
    self.registration.showNotification('OpenIsle', {
      body: payload.body,
      icon: '/favicon.ico',
      data: { url: payload.url }
    })
  )
})

self.addEventListener('notificationclick', function(event) {
  const url = event.notification.data && event.notification.data.url
  event.notification.close()
  if (url) {
    event.waitUntil(clients.openWindow(url))
  }
})
