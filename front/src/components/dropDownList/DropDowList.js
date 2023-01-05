import React, { useState, useEffect } from 'react'

import Button from '../button/Button'

import './dropDownList.scss'

const DropDownList = ({ open = false, defaultValue, elems, cb }) => {
	const [isOpen, setIsOpen] = useState(open)
	const [value, setValue] = useState(defaultValue)


	const handleValue = event => {
		setValue(event.target.textContent)
		cb({ name: event.target.textContent, id: event.target.id })
		setIsOpen(false)
	}

	useEffect(() => {
		setValue(defaultValue)
	}, [defaultValue])

	const handleOpen = () => {
		setIsOpen(!isOpen)
	}

	return <div className='list'>
		<div className='list__button' onClick={handleOpen} >
			{value} 
			<i className={isOpen ? 'arrow arrow-up' : 'arrow arrow-down'}></i>
		</div>
		{isOpen && <div className='list_items'>
			{elems.map((item, index) => <div key={index} id={item.id} onClick={handleValue} className='list_item'>{item.name || item}</div>)}
		</div>}
	</div>
}

export default DropDownList