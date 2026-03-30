import { createI18n } from 'vue-i18n'
import zh from './zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: 'zh',
  messages: {
    zh,
  },
})

export default i18n
