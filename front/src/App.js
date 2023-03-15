import React from 'react'

import Header from './layouts/header/Header'
import Footer from './layouts/footer/Footer'
import Router from './components/router/Router'
import { CssBaseline } from '@mui/material'

import './index.scss'

const App = () => {


	//<CssBaseline/>
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