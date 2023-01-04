import React, {useState} from 'react'


const Field = ({ type, placeholder, cb }) => {
	const [value, setValue] = useState('')
	
	const handleValue = event => {
		const input_text = event.target.value
		setValue(input_text)
		cb(input_text)
	}
	
	return <input type={type} placeholder={placeholder} onChange={handleValue} value={value}/>
}

export default Field