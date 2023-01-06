import React from 'react'

import './button.scss'

const Button = ({text, onClick, id}) => {
	return <button id={id} className='button' onClick={onClick}>{text}</button>
}


export default Button