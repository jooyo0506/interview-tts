import { request } from './index'

// 获取会话列表
export function getChatSessions() {
  return request.get('/api/chat/sessions')
}

// 创建会话
export function createChatSession(title, contextText) {
  return request.post('/api/chat/session', {
    title,
    contextText
  })
}

// 获取会话消息
export function getChatMessages(sessionId) {
  return request.get(`/api/chat/session/${sessionId}/messages`)
}

// 发送消息
export function sendChatMessage(sessionId, content) {
  return request.post(`/api/chat/session/${sessionId}/message`, {
    content
  })
}

// 删除会话
export function deleteChatSession(sessionId) {
  return request.delete(`/api/chat/session/${sessionId}`)
}

export default {
  getChatSessions,
  createChatSession,
  getChatMessages,
  sendChatMessage,
  deleteChatSession
}
