import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth';

import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Btn from '../../components/common/button/Button'
import MobileMenu from '../../components/mobile/menu/MobileMenu';

import './header.scss'



const Header = () => {
	const { isAuth } = useAuth()

	const openLoginForm = () => {
		window.open(`${window.location.origin}/oauth2/authorization/keycloak-spring-gateway-client`, '_parent')
	}

	const navigate = useNavigate()

	return <AppBar component="nav" position='static' className='header_menu'>
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
				<Btn text={'История заказов'} onClick={() => navigate('/page/order-history')} styles={{ color: '#fff', marginRight: 5 }} />
					<Btn text={'Модули'} onClick={() => navigate('/page/modules')} styles={{ color: '#fff' }} />
					<Btn text={'Уведомления'} onClick={() => navigate('/page/notifications')} styles={{ color: '#fff' }} />
					<Btn text={'Выйти'}
						onClick={() => window.open(`${window.location.origin}/logout`, '_parent')}
						styles={{ color: '#fff' }} />
				</> :
					<Btn text={<span>Регистрация и <br /> Авторизация</span>} onClick={openLoginForm} styles={{ color: '#fff' }} />
				}
			</Box>
		</Toolbar>
	</AppBar>
}

export default Header