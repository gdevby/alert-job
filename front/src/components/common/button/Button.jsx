import React from 'react'
import cn from 'classnames'
import Button from '@mui/material/Button';

import './button.scss'

const Btn = ({className = '', text, onClick, color = 'primary', styles = {}, variant = 'text', disabled = false }) => {
	//"contained" , 'button' color={color}
	const classes = cn(...className.split(' '))
	return <Button className={classes}  color={color} onClick={onClick} variant={variant} sx={styles} disabled={disabled}>{text}</Button>
}


export default Btn