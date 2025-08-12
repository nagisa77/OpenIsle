export const medalTitles = {
  COMMENT: '评论达人',
  POST: '发帖达人',
  SEED: '种子用户',
  CONTRIBUTOR: '贡献者',
  PIONEER: '开山鼻祖',
}

export function getMedalTitle(type) {
  return medalTitles[type] || ''
}
