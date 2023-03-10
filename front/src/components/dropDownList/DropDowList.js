import React, { useState, useEffect, useRef } from 'react'
import useOnClickOutside from '../../hooks/useOnClickOutside'

import './dropDownList.scss'
import ListItem from './ListItem'

const DropDownList = ({ open = false, defaultValue, elems, cb }) => {
	const [isOpen, setIsOpen] = useState(open)
	const [value, setValue] = useState(defaultValue)
	const [items, setItems] = useState([])

	const ref = useRef()

	const handleValue = data => {
		console.log(data)
		setValue(data.name)
		cb(data)
		setIsOpen(false)
	}

	const handleOpen = () => {
		setIsOpen(!isOpen)
	}

	useEffect(() => {
		setItems(elems)
		setValue(defaultValue)
	}, [elems])

	useEffect(() => {
		setValue(defaultValue)
	}, [defaultValue])


	useOnClickOutside(ref, setIsOpen)

	return <div className='list'>
		<div ref={ref}>
			<div className='list__button' onClick={handleOpen} >
				{value}
				<i className={isOpen ? 'arrow arrow-up' : 'arrow arrow-down'}></i>
			</div>
			{isOpen && <div className='list_items' >
				{items && items.map((item, index) => <ListItem onClick={handleValue} item={item} key={index} />)}
			</div>}
		</div>
	</div>
}

export default DropDownList