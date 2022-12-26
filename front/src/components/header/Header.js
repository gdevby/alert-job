import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import Button from '../button/Button'

import './header.scss'

const Header = () => {
	
	const openLoginForm = () => {
		window.open('http://aj.by/oauth2/authorization/keycloak-spring-gateway-client', '_parent')
	}
	

	return <header className='header'>
		<div className='container'>
			<div className='header-content'>
				<Button text='Логин' onClick={openLoginForm}/>
				<Link to='/page/filters'>Фильтры</Link>
				<Link to='/page/notifications'>Уведомления</Link>
			</div>
		</div>
	</header>
}

export default Header