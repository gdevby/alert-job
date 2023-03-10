import React from 'react'
import cn from 'classnames'

import './button.scss'

const Button = ({className = '', text, onClick, id}) => {
	
	const classes = cn(...className.split(' '), 'button')
	return <button id={id} className={classes} onClick={onClick}>{text}</button>
}


export default Button