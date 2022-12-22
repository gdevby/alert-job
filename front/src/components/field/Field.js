import React, {useState} from 'react'


const Field = ({ type, placeholder, onChange }) => {
	const [value, setValue] = useState('')
	
	const handleValue = event => {
		const input_text = event.target.value
		setValue(input_text)
		onChange(input_text)
	}
	
	return <input type={type} placeholder={placeholder} onChange={handleValue} value={value}/>
}

export default Field