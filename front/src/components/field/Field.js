import React, {useState} from 'react'


import './field.scss'

const Field = ({ type, placeholder, cb, onBlur = () => {} }) => {
	const [value, setValue] = useState('')
	
	const handleValue = event => {
		const input_text = event.target.value
		setValue(input_text)
		cb(input_text)
	}
	
	return <input className='input' type={type} placeholder={placeholder} onChange={handleValue} value={value} onBlur={onBlur}/>
}

export default Field