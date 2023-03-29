import api from '../api/coreApi'

const coreService = {
	checkAuth: () => api.get('user/test'),
	checkAuth1: () => api.post('user/authentication'),
	sendTestMessage: () => api.post('test-message'),
	getAlertInfo: () => api.get('user/alert-info'),
	addAlertTime: (time) => api.patch('user/alert-time', time),
	removeAlertTime: (id) => api.delete(`user/alert-time/${id}`),
	changeAlertsStatus: (status) => api.patch(`user/alerts?status=${status}`),
	changeAlertsType: (default_send) => api.patch(`user/alerts/type?default_send=${default_send}`),
	changeTgId: (id) => api.patch(`user/telegram?telegram_id=${id}`)
}

export { coreService }