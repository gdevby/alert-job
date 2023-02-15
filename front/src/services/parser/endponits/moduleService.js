import api from '../api/coreApi'

const moduleService = {
	getModules: () => api.get('user/order-module'),
	addModule: (name) => api.post('user/order-module', {name, available: true}),
	updateModule: (name, available, moduleId) => api.patch(`user/order-module/${moduleId}`, {name, available}),
	deleteModule: (moduleId) => api.delete(`user/order-module/${moduleId}`)
}

export { moduleService }