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
		coreService.checkAuth1()
			.then(r => setIsAuth(r.status == 200 || r.status == 201))
			.catch(error => setIsAuth(false))
	}, [])

	return <header className='header'>
		<div className='container'>
			<div className='header-content'>
				<div className='header-content__home'>
					<Link to='/'>Главная</Link>
				</div>
				<div className='header-content__navigation'>
					{isAuth && <Link to='/page/filters'>Фильтры</Link>}
					{isAuth && <Link to='/page/notifications'>Уведомления</Link>}
					<div>
						{!isAuth && <Button text={<span>Регистрация и <br /> Авторизация</span>} onClick={openLoginForm} />}
					</div>
				</div>
			</div>
		</div>
	</header>
}

export default Header