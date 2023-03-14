import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Btn from '../button/Button'

import { coreService } from '../../services/parser/endponits/coreService'

import './header.scss'
import MobileMenu from '../mobileMenu/MobileMenu';

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

	return <AppBar component="nav" position='static'>
		<Toolbar>
			<MobileMenu isAuth={isAuth} openLoginForm={openLoginForm} />
			<Typography
				variant="h6"
				component="div"
				sx={{ flexGrow: 1, display: { xs: 'none', sm: 'block' } }}
			>
				<Link to='/'>Главная</Link>
			</Typography>
			<Box sx={{ display: { xs: 'none', sm: 'block' } }}>
				{isAuth ? <>
					<Btn text={<Link to='/page/modules'>Модули</Link>} styles={{ color: '#fff' }} />
					<Btn text={<Link to='/page/notifications'>Уведомления</Link>} styles={{ color: '#fff' }} />
				</> :
					<Btn text={<span>Регистрация и <br /> Авторизация</span>} onClick={openLoginForm} styles={{ color: '#fff' }} />
				}
			</Box>
		</Toolbar>
	</AppBar>
	/*return <header className='header'>
		<div className='container'>
			<div className='header-content'>
				<div className='header-content__home'>
					<Link to='/'>Главная</Link>
				</div>
				<div className='header-content__navigation'>
					{isAuth && <Link to='/page/modules'>Модули</Link>}
					{isAuth && <Link to='/page/notifications'>Уведомления</Link>}
					<div>
						{!isAuth && <Button text={<span>Регистрация и <br /> Авторизация</span>} onClick={openLoginForm} />}
					</div>
				</div>
			</div>
		</div>
		<Button sx={{ color: '#fff' }}>
					<Link to='/page/modules'>Модули</Link>
				</Button>
				<Button sx={{ color: '#fff' }}>
						<Link to='/page/notifications'>Уведомления</Link>
					</Button>
					<Button onClick={openLoginForm}>
					<span>Регистрация и <br /> Авторизация</span>
				</Button>
	</header>*/
}

export default Header