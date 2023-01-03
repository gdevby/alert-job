import api from '../api/coreApi'

const coreService = {
	checkAuth: () => api.get('user/test'),
	checkAuth1: () => api.get('user/authentication'),
	sendTestMessage: () => api.post('test-message'),
	getStatue: () => api.get('user/alerts')
}

export { coreService }