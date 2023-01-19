import React, { useState, useEffect } from 'react'

import Button from '../button/Button'

import './dropDownList.scss'
import ListItem from './ListItem'

const DropDownList = ({ open = false, defaultValue, elems, cb }) => {
	const [isOpen, setIsOpen] = useState(open)
	const [value, setValue] = useState(defaultValue)
	const [items, setItems] = useState([])


	const handleValue = data => {
		console.log(data)
		setValue(data.name)
		cb(data)
		setIsOpen(false)
	}
	
	useEffect(() => {
		setItems(elems)
		setValue(defaultValue)
	}, [elems])

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
			{items && items.map((item, index) => <ListItem onClick={handleValue} item={item} key={index}  />)}
		</div>}
	</div>
}

export default DropDownList