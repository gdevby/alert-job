import { createSlice } from '@reduxjs/toolkit';

const initialState = {
	filters: [],
	currentFilter: {
		id: null,
		name: '',
		activatedNegativeFilters: false,
		descriptionWords: [],
		titleWords: [],
		technologyWords: [],
		maxPrice: 0,
		minPrice: 0,
		negativeDescriptionWords: [],
		negativeTitleWords: [],
		negativeTechnologyWords: [],
		descriptionWordPrice: [],
		openForAll: false
	},
	isChoose: false,
	isNew: true
};

const filterSlice = createSlice({
	name: 'filter',
	initialState,
	reducers: {
		setCurrentFilter(state, action) {
			state.currentFilter.descriptionWordPrice = action.payload.descriptionWordPrice
			state.currentFilter.descriptionWords = action.payload.description
			state.currentFilter.titleWords = action.payload.title
			state.currentFilter.technologyWords = action.payload.technologies
			state.currentFilter.negativeDescriptionWords = action.payload.negativeDescription
			state.currentFilter.negativeTitleWords = action.payload.negativeTitle
			state.currentFilter.negativeTechnologyWords = action.payload.negativeTechnologies
			state.currentFilter.name = action.payload.name
			state.currentFilter.id = action.payload.id
			state.currentFilter.maxPrice = action.payload.maxPrice
			state.currentFilter.minPrice = action.payload.minPrice
			state.currentFilter.activatedNegativeFilters = action.payload.activatedNegativeFilters
			state.currentFilter.openForAll = action.payload.openForAll
			state.isChoose = true
		},
		setActivatedNegativeFilters(state, action) {
			state.currentFilter.activatedNegativeFilters = action.payload.activatedNegativeFilters
		},
		removeCurrentFilter(state, action) {
			state.currentFilter.descriptionWordPrice = []
			state.currentFilter.descriptionWords = []
			state.currentFilter.titleWords = []
			state.currentFilter.technologyWords = []
			state.currentFilter.negativeDescriptionWords = []
			state.currentFilter.negativeTitleWords = []
			state.currentFilter.negativeTechnologyWords = []
			state.currentFilter.activatedNegativeFilters = false
			state.currentFilter.name = ''
			state.currentFilter.openForAll = false
			state.currentFilter.id = null
			state.currentFilter.maxPrice = 0
			state.currentFilter.minPrice = 0
			state.isChoose = false
			state.isNew = true
		},
		setFilters(state, action) {
			state.filters = action.payload.filters
		},
		removeFilter(state, action) {
			state.filters = state.filters.filter(item => item.id !== action.payload.id)
		},
		setIsNew(state, action) {
			state.isNew = action.payload.isNew
		},
		changeTechologies(state, action) {
			state.currentFilter.technologyWords = [...state.currentFilter.technologyWords, action.payload.technologies]
		},
		changeTitles(state, action) {
			state.currentFilter.titleWords = [...state.currentFilter.titleWords, action.payload.titles]
		},
		changeDescriptions(state, action) {
			state.currentFilter.descriptionWords = [...state.currentFilter.descriptionWords, action.payload.decscriptions]
		},
		changeFilterName(state, action) {
			state.currentFilter.name = action.payload.name
		},
		changeMaxPrice(state, action) {
			state.currentFilter.maxPrice = action.payload.maxPrice
		},
		changeMinPrice(state, action) {
			state.currentFilter.minPrice = action.payload.minPrice
		}
	},
});

export const { setCurrentFilter, setFilters, removeFilter, setIsNew, removeCurrentFilter, setActivatedNegativeFilters } = filterSlice.actions;

export default filterSlice.reducer;