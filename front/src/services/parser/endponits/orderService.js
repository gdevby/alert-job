import api from '../api/coreApi'

const ordersService = {
	getOrders: (module_id) => api.get(`user/module/${module_id}/orders`)
}

export { ordersService }