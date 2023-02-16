import api from '../api/coreApi'

const sourceService = {
	addSource: ({
		siteSource, siteCategory, siteSubCategory, flRuForAll
	}) => api.post(`user/source`, {
		siteSource, siteCategory, siteSubCategory, flRuForAll
	}),
	deleteSource: (source_id) => api.delete(`user/source/${source_id}`),
	getSources: (module_id) => api.get(`user/module/${module_id}/source`)
}

export { sourceService }