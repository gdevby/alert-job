import React, { useState, useEffect } from 'react'


import Field from '../../components/field/Field'


import './searchPopup.scss'

const SearchPopup = ({isOpen = false, onChange, close, elements = [], adding}) => {	
	const [open, setOpen] = useState(isOpen)
	const [result, setResult] = useState(elements)
	const [selectValue, setSelectValue] = useState()
	
	
	useEffect(() => {
		setResult(elements)
	},[elements])
	
	useEffect(() => {
		setOpen(isOpen)
	}, [isOpen])
	
	const closePopup = () => {
		setOpen(false)
		close()
	}
	
	const changeWord = (text) => {
		setSelectValue(text)	
	}
	
	
	const handleSelect = (event) => {
		console.log(event.target)
		// setSelectValue({name: event.target.textContent.trim(), id: event.target.id})
	}
	
	const add = () => {
		adding(selectValue)
	}
	
	return <div className={open? 'searchPopup searchPopup__open': 'searchPopup searchPopup__close'}>
		<div className='searchPopup__content'>
			<div className='searchPopup__header'>
				<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
				<Field defaultValue={''} cb={changeWord}/>
			</div>
			<div className='searchPopup__body'>
				{result && result.map(item => <div id={item.id} onClick={handleSelect}>{item.name}</div>)}
			</div>
			<div className='searchPopup__footer'>
				<div onClick={add}>Добавить</div>
			</div>
		</div>
	</div>
}

export default SearchPopup