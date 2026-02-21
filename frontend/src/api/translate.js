import { request } from './index'

// 文本翻译
export function translateText(text, sourceLang, targetLang) {
  return request.post('/api/translate/text', {
    text,
    sourceLang,
    targetLang,
    type: 'TEXT'
  })
}

// 语音翻译
export function translateSpeech(audioUrl, sourceLang, targetLang, voiceName) {
  return request.post('/api/translate/speech', {
    audioUrl,
    sourceLang,
    targetLang,
    voiceName,
    type: 'SPEECH'
  })
}

// 获取翻译历史
export function getTranslationList() {
  return request.get('/api/translate/list')
}

// 获取翻译详情
export function getTranslationDetail(id) {
  return request.get(`/api/translate/${id}`)
}

export default {
  translateText,
  translateSpeech,
  getTranslationList,
  getTranslationDetail
}
