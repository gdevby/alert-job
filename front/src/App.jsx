import React, { useEffect, useState } from 'react'

import Header from './layouts/header/Header'
import Footer from './layouts/footer/Footer'
import Router from './components/routes/router/Router'
import CircularProgress from '@mui/material/CircularProgress';

import { coreService } from './services/parser/endponits/coreService';
import { changeAuthStatus } from './hooks/changeAuthStatus';

import './index.scss'


const App = () => {
	const [isFetching, setIsFetching] = useState(true)

	const { handleStatus } = changeAuthStatus()

	useEffect(() => {
		coreService.checkAuth1()
			.then(r => {
				setAuthStatus(r.data === 'ok' || r.data === 'user created')
			})
			.catch(error => {
				setAuthStatus(false)
			})
			.finally(() => setIsFetching(false))
	}, [])

	const setAuthStatus = (status) => {
		handleStatus(status)
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