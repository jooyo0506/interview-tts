import { request } from './index'

// 获取播放列表
export function getPlaylistList() {
  return request.get('/api/playlist/list')
}

// 创建播放列表
export function createPlaylist(name, description) {
  return request.post('/api/playlist/create', { name, description })
}

// 删除播放列表
export function deletePlaylist(id) {
  return request.delete(`/api/playlist/${id}`)
}

// 重命名播放列表
export function renamePlaylist(id, name) {
  return request.put(`/api/playlist/${id}/rename`, { name })
}

// 获取播放列表音频
export function getPlaylistAudios(id) {
  return request.get(`/api/playlist/${id}/audios`)
}

// 添加音频到播放列表
export function addAudioToPlaylist(playlistId, audioFileId) {
  return request.post(`/api/playlist/${playlistId}/audio`, { audioFileId })
}

// 从播放列表移除音频
export function removeAudioFromPlaylist(playlistId, audioFileId) {
  return request.delete(`/api/playlist/${playlistId}/audio/${audioFileId}`)
}

export default {
  getPlaylistList,
  createPlaylist,
  deletePlaylist,
  renamePlaylist,
  getPlaylistAudios,
  addAudioToPlaylist,
  removeAudioFromPlaylist
}
