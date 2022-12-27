import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

import Button from '../button/Button'

import { coreService } from '../../services/parser/endponits/coreService' 

import './header.scss'

const Header = () => {
	const [isAuth, setIsAuth] = useState(false)
	
	
	const openLoginForm = () => {
		window.open(`${window.location.origin}/oauth2/authorization/keycloak-spring-gateway-client`, '_parent')
	}
	
	useEffect(() => {
		coreService
		.checkAuth()
		.then(response => setIsAuth(response.status == '200'))
	}, [])

	return <header className='header'>
		<div className='container'>
			<div className='header-content'>
				{!isAuth && <Button text='Логин' onClick={openLoginForm}/>}
				<Link to='/'>Главная</Link>
				{isAuth && <Link to='/page/filters'>Фильтры</Link>}
				{isAuth && <Link to='/page/notifications'>Уведомления</Link>}
			</div>
		</div>
	</header>
}

export default Header