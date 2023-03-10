import React, { useState, useEffect, useRef } from 'react'
import useOnClickOutside from '../../hooks/useOnClickOutside'

import './dropDownList.scss'
import ListItem from './ListItem'
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import InputLabel from '@mui/material/InputLabel';

const DropDownList = ({ elems, onClick, label, defaultLabe }) => {
	//const [isOpen, setIsOpen] = useState(open)
	//const [value, setValue] = useState(defaultValue)
	const [items, setItems] = useState([])
	const [current, setCurrent] = useState('')

	/*const ref = useRef()

	const handleValue = data => {
		console.log(data)
		setValue(data.name)
		cb(data)
		setIsOpen(false)
	}*/

	/*	const handleOpen = () => {
			setIsOpen(!isOpen)
		}
	*/
	useEffect(() => {
		console.log(elems)
		setItems(elems)
		//setValue(defaultValue)
	}, [elems])

	/*useEffect(() => {
		setValue(defaultValue)
	}, [defaultValue])*/


	//useOnClickOutside(ref, setIsOpen)


	const handleChange = e => {
		if (e.target.value) {
			setCurrent(e.target.value)
		}else {
			setCurrent(0)
		}
		
	}

	return <FormControl fullWidth>
		<InputLabel id="demo-simple-select-label">{defaultLabe}</InputLabel>
		<Select
			labelId="demo-simple-select-label"
			id="demo-simple-select"
			value={current}
			label={label}
			onChange={handleChange}
		>
			{items.map(item => <MenuItem key={item.id} value={item.id || 0} id={item.id || 0} onClick={() => onClick(item)} >{item.name}</MenuItem>)}
		</Select>
	</FormControl>

	/*return <div className='list'>
		<div ref={ref}>
			<div className='list__button' onClick={handleOpen} >
				{value}
				<i className={isOpen ? 'arrow arrow-up' : 'arrow arrow-down'}></i>
			</div>
			{isOpen && <div className='list_items' >
				{items && items.map((item, index) => <ListItem onClick={handleValue} item={item} key={index} />)}
			</div>}
		</div>
	</div>*/
}

export default DropDownList