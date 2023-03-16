import api from '../api/coreApi'

const ordersService = {
	//getOrders: (module_id) => api.get(`user/module/${module_id}/orders`)
	getOrders: (module_id, type) => api.get(`user/module/${module_id}/${type}-filter/orders`)
}

export { ordersService }