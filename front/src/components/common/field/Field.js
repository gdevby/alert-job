import React, { useState, useEffect } from 'react';

import TextField from '@mui/material/TextField';

import './field.scss';

const Field = (props) => {
	const { defaultValue, type, placeholder = '', cb, onBlur = () => { }, label = undefined } = props
	
	const [current_value, setValue] = useState('')

	useEffect(() => {
		setValue(defaultValue)
	}, [defaultValue])

	const handleValue = event => {
		const input_text = event.target.value
		setValue(input_text)
		cb(input_text)
	}

	return <div className='field-container'>
		<TextField
			id="standard-basic"
			label={label}
			variant="standard"
			placeholder={placeholder}
			onChange={handleValue}
			onBlur={onBlur}
			value={current_value}
			type={type}
		/>
	</div>
}

export default Field