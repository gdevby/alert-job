import React, { useState, useEffect } from 'react'

import Title from '../../components/title/Title'
import Field from '../../components/field/Field'
import Button from '../../components/button/Button'
import DropDownList from '../../components/dropDownList/DropDowList'
import SearchPopup from '../../components/seachPopup/SearchPopup'

import { filterService } from '../../services/parser/endponits/filterService'


const AddFilterPage = () => {
	const [isOpenPopup, setIsOpenPopup] = useState(false)
	const [wordsType, setWordstype] = useState('')
	const [words, setWords] = useState('')


	const handleCurrentFilterType = (data) => {
		console.log(data)
	}

	const addNewFilter = () => {
		console.log(addFilter)
	}

	const handlePopup = (wordType) => {
		console.log(wordType)
		setWordstype(wordType)
		searchWords(wordType)
		setIsOpenPopup(true)
		console.log(isOpenPopup)
	}
	
	const searchWords = (wordType) => {
		filterService
		.getWords(wordType)
		.then(response => setWords(response.data))
	}
	
	const addWord = (word) => {
		filterService
		.addWord(word, wordsType)
		.then(console.log)
	}
	
	
	

	return <div className='filtersPage'>
		<SearchPopup isOpen={isOpenPopup} close={() => setIsOpenPopup(false)} elements={words} adding={addWord}/>
		<div className='container'>
			<Title text={'Добавление фильтров'} />

			<Field
				type={'text'} defaultValue=''
				placeholder={'Введите название'} label={<label>Название фильтра</label>} />

			<div className='price_block'>
				<Field
					type={'number'} defaultValue=''
					placeholder={'Минимальная цена'} label={<label>Минимальная цена</label>} />
				<Field
					type={'number'} defaultValue=''
					placeholder={'Максимальная цена'} label={<label>Максимальная цена</label>} />
			</div>

			<div className='wordsContains_block'>
				<div>
					Уведомлять, если технология содержит
					<Button text={'Добавить'} data-Wordtype='technology-word'  onClick={() => handlePopup('technology-word')} />
				</div>
				<div>
					Уведомлять, если названии содержит
					<Button text={'Добавить'} data-Wordtype='title-word' onClick={() => handlePopup('title-word')} />
				</div>
				<div>
					Уведомлять, в описании содержится
					<Button text={'Добавить'} data-Wordtype='description-word' onClick={() => handlePopup('description-word')} />
				</div>
			</div>
			<div className='filtersPage'>
				<Button text={'Добавить'} onClick={addNewFilter} />
			</div>
		</div>
	</div>
}

export default AddFilterPage