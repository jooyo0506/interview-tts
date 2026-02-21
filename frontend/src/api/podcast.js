import request from './index'

// 生成播客
export function generatePodcast(title, sourceText, voiceA, voiceB) {
  return request.post('/api/podcast/generate', {
    title,
    sourceText,
    voiceA,
    voiceB
  })
}

// 获取播客列表
export function getPodcastList() {
  return request.get('/api/podcast/list')
}

// 获取播客详情
export function getPodcastDetail(id) {
  return request.get(`/api/podcast/${id}`)
}

export default {
  generatePodcast,
  getPodcastList,
  getPodcastDetail
}
