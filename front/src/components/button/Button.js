import React from 'react'
import cn from 'classnames'
import Button from '@mui/material/Button';

import './button.scss'

const Btn = ({className = '', text, onClick, id, color = 'primary'}) => {
	
	const classes = cn(...className.split(' '), 'button')
	return <Button id={id} className={classes} color={color} onClick={onClick} variant="contained">{text}</Button>
}


export default Btn