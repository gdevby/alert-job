import api from '../api/coreApi'

const coreService = {
	checkAuth: () => api.get('user/test'),
	checkAuth1: () => api.post('user/authentication'),
	sendTestMessage: () => api.post('test-message'),
	getStatue: () => api.get('user/alerts'),
	changeAlertsStatus: (status) => api.patch(`user/alerts?status=${status}`),
	changeAlertsType: (default_send) => api.patch(`user/alerts/type?default_send=${default_send}`),
	getAlertType: () => api.get('user/alerts/type'),
	changeTgId: (id) => api.patch(`user/telegram?telegram_id=${id}`)
}

export { coreService }