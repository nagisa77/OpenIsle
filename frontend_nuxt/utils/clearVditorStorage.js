export function clearVditorStorage() {
  Object.keys(localStorage).forEach(key => {
    if (key.startsWith('vditoreditor-') || key === 'vditor') {
      localStorage.removeItem(key)
    }
  })
}
