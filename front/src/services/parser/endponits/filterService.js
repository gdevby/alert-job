import api from '../api/coreApi'

const filterService = {
	getFilters: () => api.get('user/filter'),
	getWords: (word_type) => api.get(`user/${word_type}`),
	addWord: (word, word_type) => api.post(`user/${word_type}`, {name: word}),
	updateWord: (word_type, word_id, filter_id) => api.patch(`user/filter/${filter_id}/${word_type}/${word_id}`),
	addFilter: (filter) => api.post('user/filter', filter),
	deleteFilter: (id) => api.delete(`user/filter/${id}`),
	updateFilter: (id, filter) => api.patch(`user/filter/${id}`, filter),
	updateCurrentFilter: (id) => api.patch(`user/filter/${id}/current`)
}

export { filterService }