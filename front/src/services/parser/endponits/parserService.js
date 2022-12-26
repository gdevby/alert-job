import api from '../api/parserApi'

const parserService = {
	getSites: () => api.get('sites'),
	getCategories: (id) => api.get(`categories?site_id=${id}`),
	getSubcategories: (id) => api.get(`subcategories?category_id=${id}`)
}

export { parserService }