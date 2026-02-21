import request from './index'

// 获取克隆声音列表
export function getVoiceCloneList() {
  return request.get('/api/voice-clone/list')
}

// 获取克隆声音状态
export function getVoiceCloneStatus(id) {
  return request.get(`/api/voice-clone/status/${id}`)
}

// 创建声音复刻任务
export function createVoiceClone(name, sampleUrls) {
  return request.post('/api/voice-clone/create', {
    name,
    sampleUrls
  })
}

// 删除克隆声音
export function deleteVoiceClone(id) {
  return request.delete(`/api/voice-clone/${id}`)
}

// 获取克隆声音样本
export function getVoiceSamples(id) {
  return request.get(`/api/voice-clone/${id}/samples`)
}

export default {
  getVoiceCloneList,
  getVoiceCloneStatus,
  createVoiceClone,
  deleteVoiceClone,
  getVoiceSamples
}
