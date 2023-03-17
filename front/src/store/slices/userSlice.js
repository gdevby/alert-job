import { createSlice } from '@reduxjs/toolkit';

const initialState = {
	isAuth: false,
};

const userSlice = createSlice({
	name: 'user',
	initialState,
	reducers: {
		setAuth(state, action) {
			state.isAuth = action.payload.isAuth
		}
	},
});

export const { setAuth } = userSlice.actions;

export default userSlice.reducer;