import api from '../api/coreApi'

const sourceService = {
	addSource: (module_id, {
		siteSource, siteCategory, siteSubCategory, flRuForAll
	}) => api.post(`user/module/${module_id}/source`, {
		siteSource, siteCategory, siteSubCategory, flRuForAll
	}),
	deleteSource: (module_id, source_id) => api.delete(`user/module/${module_id}/source/${source_id}`),
	getSources: (module_id) => api.get(`user/module/${module_id}/source`)
}

export { sourceService }