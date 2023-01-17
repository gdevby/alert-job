import React, { useState, useEffect } from 'react'

import Button from '../../button/Button'
import Words from '../word/Words'



import { filterService } from '../../../services/parser/endponits/filterService'

const TitleWords = ({ filter_id }) => {
	const [isOpen, setIsOpen] = useState(false)
	const [words, setWords] = useState([])
	const [selectValue, setSelectValue] = useState('')
	const [result, setResult] = useState([])

	const openSearch = () => {
		setIsOpen(true)
	}

	const addWord = (word) => {
		console.log(word)
		filterService
			.addWordToFilter('title-word', filter_id, word.id)
			.then(() => {
				setWords((prev) => [...prev, word])
				setIsOpen(false)
			})

	}

	const deleteWord = (word) => {

	}

	const getWords = (text, page = 0) => {
		filterService
			.getWords('title-word', text, page)
			.then(response => setResult(response.data.content))
	}

	const changeWord = (event) => {
		setSelectValue(event.target.value)
		getWords(event.target.value)
	}


	const add = () => {
		if (result.length == 0) {
			filterService
				.addWord(selectValue, 'title-word')
				.then(response => {
					addWord(response.data)
				})
		}
	}

	const closePopup = () => {
		setIsOpen(false)
	}

	const remove = (id) => {
		filterService
		.deleteWord('title-word', filter_id, id)
		.then(console.log)
		
	}

	const handleSelect = () => {
		const word = { id: event.target.id, name: event.target.textContent.trim() }
		addWord(word)
	}

	return <>
		<div className={isOpen ? 'searchPopup searchPopup__open' : 'searchPopup searchPopup__close'}>
			<div className='searchPopup__content'>
				<div className='searchPopup__header'>
					<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
					<input type='text' onChange={changeWord} value={selectValue} />
				</div>
				<div className='searchPopup__body'>
					{result && result.map(item => <div id={item.id} onClick={handleSelect}>{item.name}</div>)}
				</div>
				<div className='searchPopup__footer'>
					<Button onClick={add} text={'Добавить'}/>
				</div>
			</div>
		</div>
		Уведомлять, в названии содержится
		<Button text={'Добавить'} onClick={openSearch} />
		<div>
			<Words items={words} remove={remove} />
		</div>
	</>
}

export default TitleWords