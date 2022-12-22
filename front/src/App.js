import React from 'react'

import Header from './components/header/Header'
import Footer from './components/footer/Footer'
import Router from './components/router/Router'

import './index.scss'

const App = () => {
	
	
	
	return <>
		<Header />
		<div className='content'>
			<Router />
		</div>
		<Footer />
	</>
}

export default App