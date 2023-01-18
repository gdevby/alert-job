import { createSlice } from '@reduxjs/toolkit';

const initialState = {
	filters: [],
	currentFilter: {
		id: null,
		name: '',
		descriptionWords: [],
		titleWords: [],
		technologyWords: [],
		maxPrice: 0,
		minPrice: 0
	},
	isChoose: false,
	isNew: true
};

const filterSlice = createSlice({
	name: 'filter',
	initialState,
	reducers: {
		setCurrentFilter(state, action) {
			state.currentFilter.descriptionWords = action.payload.description
			state.currentFilter.titleWords = action.payload.title
			state.currentFilter.technologyWords = action.payload.technology
			state.currentFilter.name = action.payload.name
			state.currentFilter.id = action.payload.id
			state.currentFilter.maxPrice = action.payload.maxPrice
			state.currentFilter.minPrice = action.payload.minPrice
			state.isChoose = true
		},
		setFilters(state, action) {
			state.filters = action.payload.filters
		},
		removeFilter(state, action) {
			state.filters = state.filters.filter(item => item.id !== action.payload.id)
		},
		setIsNew(state, action) {
			state.isNew = action.payload.isNew
		}
	},
});

export const { setCurrentFilter, setFilters, removeFilter, setIsNew } = filterSlice.actions;

export default filterSlice.reducer;