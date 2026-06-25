import { useDispatch } from 'react-redux';
import { setAuth } from '../store/slices/userSlice';

export const changeAuthStatus = () => {
	
	const dispatch = useDispatch()
	const handleStatus = (status) => {
		dispatch(
			setAuth({
				isAuth: status
			})
		)
	}
	
	return {handleStatus}
}