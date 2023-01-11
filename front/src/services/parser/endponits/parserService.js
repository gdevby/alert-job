import api from '../api/parserApi'

const parserService = {
	getSites: () => api.get('sites'),
	getCategories: (id) => api.get(`categories?site_id=${id}`),
	getSubcategories: (id) => api.get(`subcategories?category_id=${id}`),
	getSiteNameById: (id) => api.get(`site/${id}`),
	getCatNameById: (site_id, category_id) => api.get(`site/${site_id}/category/${category_id}`),
	getSubCatNameById: (cat_id, sub_id) => api.get(`category/${cat_id}/subcategory/${sub_id}`)
}

export { parserService }