import api from '../api/coreApi'

const filterService = {
	getFilters: (module_id) => api.get(`user/module/${module_id}/filter`),
	getWords: (word_type, name, page) => api.get(`user/${word_type}?name=${name}&page=${page}`),
	addWord: (word, word_type) => api.post(`user/${word_type}`, {name: word}),
	updateWord: (word_type, word_id, filter_id) => api.patch(`user/filter/${filter_id}/${word_type}/${word_id}`),
	addFilter: (filter) => api.post('user/filter', filter),
	deleteFilter: (id) => api.delete(`user/filter/${id}`),
	updateFilter: (id, filter) => api.patch(`user/filter/${id}`, filter),
	updateCurrentFilter: (id) => api.patch(`user/filter/${id}/current`),
	addWordToFilter: (word_type, filter_id, word_id) => api.patch(`user/filter/${filter_id}/${word_type}/${word_id}`),
	deleteWord: (word_type, filter_id, word_id) => api.delete(`user/filter/${filter_id}/${word_type}/${word_id}`),
	getCurrentFilter: (module_id) => api.get(`user/module/${module_id}/current-filter`)
}

export { filterService }