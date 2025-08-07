import { useToast } from 'vue-toastification'
import { initTheme } from './utils/theme'
import { clearVditorStorage } from './utils/clearVditorStorage'

export const API_DOMAIN = 'https://www.open-isle.com'
export const API_PORT = ''
export const API_BASE_URL = ''
export const GOOGLE_CLIENT_ID = '777830451304-nt8afkkap18gui4f9entcha99unal744.apps.googleusercontent.com'
export const GITHUB_CLIENT_ID = 'Ov23liVkO1NPAX5JyWxJ'
export const DISCORD_CLIENT_ID = '1394985417044000779'
export const TWITTER_CLIENT_ID = 'ZTRTU05KSk9KTTJrTTdrVC1tc1E6MTpjaQ'

export const toast = useToast()

initTheme()
clearVditorStorage()
