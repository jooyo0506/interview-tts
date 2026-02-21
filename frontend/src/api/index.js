import axios from 'axios'
import { ref } from 'vue'

// 配置
const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080'
const USER_KEY_STORAGE_KEY = 'userKey'

// 获取或生成 userKey
export function getUserKey() {
  let userKey = uni.getStorageSync(USER_KEY_STORAGE_KEY)
  if (!userKey) {
    userKey = generateUUID()
    uni.setStorageSync(USER_KEY_STORAGE_KEY, userKey)
  }
  return userKey
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 创建 axios 实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userKey = getUserKey()
    config.headers['X-User-Key'] = userKey
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.success) {
      return res
    } else {
      uni.showToast({
        title: res.message || '请求失败',
        icon: 'none'
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('响应错误:', error)
    uni.showToast({
      title: error.message || '网络错误',
      icon: 'none'
    })
    return Promise.reject(error)
  }
)

// API 方法

// 初始化用户
export function initUser(userKey) {
  return request.post('/api/user/init', { userKey })
}

// 获取音色列表
export function getVoices() {
  return request.get('/api/tts/voices')
}

// 生成音频（短文本 - 300字以内）
export function generateAudio(rawText, voiceName) {
  return request.post('/api/tts/generate', {
    rawText,
    voiceName
  })
}

// 生成音频（长文本 - 10万+字）
// @param useEmotion 是否使用情感预测版
export function generateLongAudio(rawText, voiceName, useEmotion = false) {
  return request.post('/api/audio/generate-long', {
    rawText,
    voiceName,
    useEmotion
  })
}

// 查询长文本任务状态
export function queryTaskStatus(audioFileId, taskId, useEmotion = false) {
  return request.get('/api/audio/task-status', {
    params: { audioFileId, taskId, useEmotion }
  })
}

// 获取我的列表
export function getMyList() {
  return request.get('/api/audio/my-list')
}

// 获取音频详情（包含完整文本）
export function getAudioDetail(id) {
  return request.get('/api/audio/detail', { params: { id } })
}

// 收藏/取消收藏
export function collectAudio(audioId) {
  return request.post('/api/audio/collect', { audioId })
}

// 获取收藏列表
export function getCollectList() {
  return request.get('/api/audio/collect-list')
}

// 删除音频
export function deleteAudio(id) {
  return request.delete(`/api/audio/${id}`)
}

// 批量删除音频
export function deleteAudioBatch(ids) {
  return request.delete('/api/audio/batch', { data: { ids } })
}

// 重命名音频
export function renameAudio(id, name) {
  return request.put(`/api/audio/${id}/rename`, { name })
}

// 删除收藏
export function deleteCollect(id) {
  return request.delete(`/api/audio/collect/${id}`)
}

// 上传音频文件
export function uploadAudio(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}/api/upload/audio`,
      filePath: filePath,
      name: 'file',
      header: {
        'X-User-Key': getUserKey()
      },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.success) {
            resolve(data)
          } else {
            reject(new Error(data.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('解析响应失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

// 获取用户信息
export function getUserProfile() {
  return request.get('/api/user/profile')
}

// 更新用户信息
export function updateUserProfile(nickname, avatar) {
  return request.put('/api/user/profile', { nickname, avatar })
}

// ===== TTS v2.0 API =====

// 获取TTSv2支持的音色列表
export function getTtsV2Voices() {
  return request.get('/api/tts/v2/voices')
}

// TTSv2语音合成
export function synthesizeTtsV2(data) {
  return request.post('/api/tts/v2/synthesize', data)
}

export default {
  getUserKey,
  initUser,
  getVoices,
  generateAudio,
  generateLongAudio,
  queryTaskStatus,
  getMyList,
  collectAudio,
  getCollectList,
  deleteAudio,
  deleteAudioBatch,
  renameAudio,
  deleteCollect,
  uploadAudio,
  getUserProfile,
  updateUserProfile,
  getTtsV2Voices,
  synthesizeTtsV2
}
