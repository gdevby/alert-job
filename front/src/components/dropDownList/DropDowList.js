import React, {useState} from 'react'

import Button from '../button/Button'

import './dropDownList.scss'

const DropDownList = ({open = false, defaultValue, elems, cb}) => {
	const [isOpen, setIsOpen] = useState(open)
	const [value, setValue] = useState(defaultValue)
	
	
	const handleValue = event => {
		setValue(event.target.textContent)
		cb({name: event.target.textContent, id: event.target.id})
		setIsOpen(false)
	}
	
	const handleOpen = () => {
		setIsOpen(true)
	}
	
	return <div className='list'>
		<Button text={value} onClick={handleOpen} />
		{isOpen && <div className='list_items'>
			{elems.map((item, index) => <div key={index} id={item.id} onClick={handleValue} className='list_item'>{item.name}</div>)}
		</div>}
	</div>
}

export default DropDownList