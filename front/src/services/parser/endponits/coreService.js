import api from '../api/coreApi'

const coreService = {
	checkAuth: () => api.get('user/test')
}

export { coreService }