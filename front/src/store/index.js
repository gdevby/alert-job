import { combineReducers, configureStore } from '@reduxjs/toolkit';
import filterSlice from './slices/filterSlice';

const rootReducer = combineReducers({
	filter: filterSlice
});

export const store = configureStore({
	reducer: rootReducer,
});