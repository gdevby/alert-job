import React, { useEffect } from 'react'
import { useDispatch } from 'react-redux';

import Header from './layouts/header/Header'
import Footer from './layouts/footer/Footer'
import Router from './components/routes/router/Router'

import { coreService } from './services/parser/endponits/coreService';

import { setAuth } from './store/slices/userSlice';

import './index.scss'


const App = () => {
	const dispatch = useDispatch()
	
	useEffect(() => {
		coreService.checkAuth1()
			.then(r =>setAuthStatus(r.status == 200 || r.status == 201))
			.catch(error => setAuthStatus(false))
	}, [])
	
	const setAuthStatus = (status) => {
		dispatch(
			setAuth({
				isAuth: status
			})
		)
	}
	
	
	return <div className='wrapper'>
		<div className='top'>
			<Header />
			<div className='content'>
				<Router />
			</div>
		</div>
		<Footer />
	</div>
}

export default App