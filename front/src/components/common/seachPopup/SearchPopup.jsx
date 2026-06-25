import React, { useState, useEffect } from 'react'
import useDebounce from '../../../hooks/use-debounce'

import './searchPopup.scss'

const SearchPopup = ({isOpen = false, onChange, close, elements = [], adding}) => {	
	const [open, setOpen] = useState(isOpen)
	const [result, setResult] = useState(elements)
	const [selectValue, setSelectValue] = useState('')
	
	const debouncedSearchTerm = useDebounce(selectValue, 1000)
	
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
	
	const changeWord = (event) => {
		onChange()
		setSelectValue(event.target.value)	
	}
	
	
	const handleSelect = (event) => {
		console.log(event.target)
		// setSelectValue({name: event.target.textContent.trim(), id: event.target.id})
	}
	
	const add = () => {
		adding(selectValue)
	}
	
	useEffect(() => {
		if (debouncedSearchTerm) {

      } else {
        setResult([]);
      }
	}, [debouncedSearchTerm])
	
	return <div className={open? 'searchPopup searchPopup__open': 'searchPopup searchPopup__close'}>
		<div className='searchPopup__content'>
			<div className='searchPopup__header'>
				<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
				<input type='text' onChange={changeWord} value={selectValue}/>
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