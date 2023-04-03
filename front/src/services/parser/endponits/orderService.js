import api from '../api/coreApi'

const ordersService = {
	getOrders: (module_id, type, period) => api.get(`user/module/${module_id}/${type}-filter/orders?period=${period}`)
}

export { ordersService }