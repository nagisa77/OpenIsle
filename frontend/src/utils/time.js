export default class TimeManager {
  static format(input) {
    const date = new Date(input)
    if (Number.isNaN(date.getTime())) return ''

    const now = new Date()
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    const diffDays = Math.floor((startOfToday - startOfDate) / 86400000)

    const hh = date.getHours().toString().padStart(2, '0')
    const mm = date.getMinutes().toString().padStart(2, '0')
    const timePart = `${hh}:${mm}`

    if (diffDays === 0) return `今天 ${timePart}`
    if (diffDays === 1) return `昨天 ${timePart}`
    if (diffDays === 2) return `前天 ${timePart}`

    const month = date.getMonth() + 1
    const day = date.getDate()

    if (date.getFullYear() === now.getFullYear()) {
      return `${month}.${day} ${timePart}`
    }

    if (date.getFullYear() === now.getFullYear() - 1) {
      return `去年 ${month}.${day} ${timePart}`
    }

    return `${date.getFullYear()}.${month}.${day} ${timePart}`
  }
}
