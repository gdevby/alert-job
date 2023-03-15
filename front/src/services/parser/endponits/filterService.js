import api from '../api/coreApi'

const filterService = {
	getFilters: (module_id) => api.get(`user/module/${module_id}/filter`),
	getCurrentFilter: (module_id) => api.get(`user/module/${module_id}/current-filter`),
	addFilter: (module_id, filter) => api.post(`user/module/${module_id}/filter`, filter),
	deleteFilter: (module_id, filter_id) => api.delete(`user/module/${module_id}/filter/${filter_id}`),
	updateFilter: (module_id, filter_id, filter) => api.patch(`user/module/${module_id}/filter/${filter_id}`, filter),
	updateCurrentFilter: (module_id, filter_id) => api.post(`user/module/${module_id}/current-filter/${filter_id}`),
	getWords: (word_type, name, page) => api.get(`user/${word_type}?name=${name}&page=${page}`),
	addWord: (word, word_type) => api.post(`user/${word_type}`, {name: word}),
	updateWord: (word_type, word_id, filter_id, type = '') => api.patch(`user/${type}filter/${filter_id}/${word_type}/${word_id}`),
	addWordToFilter: (word_type, filter_id, word_id, type = '') => api.patch(`user/${type}filter/${filter_id}/${word_type}/${word_id}`),
	deleteWord: (word_type, filter_id, word_id, type) => api.delete(`user/${type}filter/${filter_id}/${word_type}/${word_id}`),
}

export { filterService }