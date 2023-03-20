import React, { useEffect, useState } from 'react'
import { useDispatch } from 'react-redux';

import Header from './layouts/header/Header'
import Footer from './layouts/footer/Footer'
import Router from './components/routes/router/Router'
import CircularProgress from '@mui/material/CircularProgress';

import { coreService } from './services/parser/endponits/coreService';

import { setAuth } from './store/slices/userSlice';

import './index.scss'


const App = () => {
	const [isFetching, setIsFetching] = useState(true)

	const dispatch = useDispatch()

	useEffect(() => {
		coreService.checkAuth1()
			.then(r => setAuthStatus(r.status == 200 || r.status == 201))
			.catch(error => setAuthStatus(false))
			.finally(() => setIsFetching(false))
	}, [])

	const setAuthStatus = (status) => {
		dispatch(
			setAuth({
				isAuth: status
			})
		)
	}

	return <div className='wrapper'>
		{isFetching ? <CircularProgress /> : <><div className='top'>
			<Header />
			<div className='content'>
				<Router />
			</div>
		</div>
			<Footer />
			</>}
	</div>
}

export default App