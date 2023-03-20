import React, { useState, useEffect } from 'react'

import './field.scss'

const Field = ({ defaultValue, type, placeholder, cb, onBlur = () => { }, label = undefined }) => {
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
	{label && <label htmlFor='input'>{label}</label>}
	<input className='input' id='input' 
			type={type} placeholder={placeholder} 
			onChange={handleValue} value={current_value} 
			onBlur={onBlur} />
	</div>
}

export default Field