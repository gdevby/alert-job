import api from '../api/coreApi'

const filterService = {
	getFilters: () => api.get('user/filter'),
	getWords: (word_type) => api.get(`user/${word_type}`),
	addWord: (word, word_type) => api.post(`user/${word_type}`, {name: word})
}

export { filterService }