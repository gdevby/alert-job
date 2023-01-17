import React, { useState, useEffect } from 'react'

import Title from '../../components/title/Title'
import Button from '../../components/button/Button'
import AddFilterForm from '../../components/addFilterForm/AddFilterForm'
import TechnologyWords from '../../components/filters/technologyWords/TechnologyWords'
import TitleWords from '../../components/filters/titleWords/TitleWords'
import DescriptionWords from '../../components/filters/descriptionWords/DescriptionWords'



import './addFilterPage.scss'
import { useNavigate } from 'react-router-dom'


const AddFilterPage = () => {
	const [isOpenPopup, setIsOpenPopup] = useState(false)
	const [wordsType, setWordstype] = useState('')
	const [words, setWords] = useState('')
	const [filterId, setFilterId] = useState('')

	const navigate = useNavigate()

	const handleCurrentFilterType = (data) => {
		console.log(data)
	}

	const addNewFilter = () => {
		
	}

	const handlePopup = (wordType) => {
		console.log(wordType)
		setWordstype(wordType)
		searchWords(wordType)
		setIsOpenPopup(true)
		console.log(isOpenPopup)
	}
	
	
	
	
	

	return <div className='filtersPage'>
		<div className='container'>
			<Title text={'Добавление фильтров'} />

			<AddFilterForm setFilterId={setFilterId}/>

			<div className='wordsContains_block'>
				<div>
					<TechnologyWords filter_id={filterId}/>
				</div>
				<div>
					<TitleWords filter_id={filterId}/>
				</div>
				<div>
					<DescriptionWords filter_id={filterId}/>
				</div>
			</div>
			<div className='addFilter'>
				<Button text={'Добавить'} onClick={addNewFilter} />
			</div>
		</div>
	</div>
}

export default AddFilterPage

