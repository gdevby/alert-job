import { combineReducers, configureStore } from '@reduxjs/toolkit';
import filterSlice from './slices/filterSlice';
import userSlice from './slices/userSlice';

const rootReducer = combineReducers({
	filter: filterSlice,
	user: userSlice
});

export const store = configureStore({
	reducer: rootReducer,
});