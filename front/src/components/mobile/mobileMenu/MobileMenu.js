import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import Menu from '@mui/material/Menu';
import MenuIcon from '@mui/icons-material/Menu';
import MenuItem from '@mui/material/MenuItem';
import IconButton from '@mui/material/IconButton';
import Box from '@mui/material/Box';

const MobileMenu = ({isAuth, openLoginForm}) => {
	const [anchorElNav, setAnchorElNav] = useState(null);
	const [anchorElUser, setAnchorElUser] = useState(null);

	const handleOpenNavMenu = (event) => {
		setAnchorElNav(event.currentTarget);
	};
	
	const handleOpenUserMenu = (event) => {
		setAnchorElUser(event.currentTarget);
	};

	const handleCloseNavMenu = () => {
		setAnchorElNav(null);
	};

	const handleCloseUserMenu = () => {
		setAnchorElUser(null);
	};

	return <Box sx={{ flexGrow: 1, display: { xs: 'flex', md: 'none' } }}>
		<IconButton
			size="large"
			aria-label="account of current user"
			aria-controls="menu-appbar"
			aria-haspopup="true"
			onClick={handleOpenNavMenu}
			color="inherit"
		>
			<MenuIcon />
		</IconButton>
		<Menu
			id="menu-appbar"
			anchorEl={anchorElNav}
			anchorOrigin={{
				vertical: 'bottom',
				horizontal: 'left',
			}}
			keepMounted
			transformOrigin={{
				vertical: 'top',
				horizontal: 'left',
			}}
			open={Boolean(anchorElNav)}
			onClose={handleCloseNavMenu}
			sx={{
				display: { xs: 'block', md: 'none' },
			}}
		>
			<MenuItem onClick={handleCloseNavMenu}>
				<Link to='/'>Главная</Link>
			</MenuItem>
			{isAuth && <MenuItem onClick={handleCloseNavMenu}><Link to='/page/modules'>Модули</Link></MenuItem>}
			{isAuth && <MenuItem onClick={handleCloseNavMenu}><Link to='/page/notifications'>Уведомления</Link></MenuItem>}
			{!isAuth && <MenuItem onClick={openLoginForm}><span>Регистрация и <br /> Авторизация</span></MenuItem>}  
		</Menu>
	</Box>

}

export default MobileMenu