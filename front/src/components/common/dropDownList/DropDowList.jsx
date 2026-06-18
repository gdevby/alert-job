import React, { useState, useEffect } from 'react'
import cn from 'classnames'

import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import InputLabel from '@mui/material/InputLabel';

import './dropDownList.scss'

const DropDownList = ({ elems, onClick, label, defaultLabe, className = '', defaultValue = '' }) => {
	const [items, setItems] = useState([])
	const [current, setCurrent] = useState('')

	const classes = cn(...className.split(' '))

	useEffect(() => {
		setItems(elems)
	}, [elems])
	
	useEffect(() => {
		setCurrent(defaultValue)
	}, [defaultValue])

	const handleChange = e => {
		if (e.target.value) {
			setCurrent(e.target.value)
		} else {
			setCurrent(0)
		}
	}

	return <FormControl fullWidth size='small' className={classes}>
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
}

export default DropDownList