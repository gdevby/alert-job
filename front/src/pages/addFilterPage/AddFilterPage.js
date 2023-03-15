import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom'

import Title from '../../components/title/Title'
import AddFilterForm from '../../components/addFilterForm/AddFilterForm'
import TechnologyWords from '../../components/filters/technologyWords/TechnologyWords'
import TitleWords from '../../components/filters/titleWords/TitleWords'
import DescriptionWords from '../../components/filters/descriptionWords/DescriptionWords'
import Btn from '../../components/button/Button'
import Filters from '../../layouts/addFilterPage/filters/Filter';

import { filterService } from '../../services/parser/endponits/filterService';

import { removeCurrentFilter, setIsNew, setCurrentFilter } from '../../store/slices/filterSlice';

import './addFilterPage.scss'


const AddFilterPage = () => {
	const [isOpenPopup, setIsOpenPopup] = useState(false)
	const [wordsType, setWordstype] = useState('')
	const [words, setWords] = useState('')
	const [filterId, setFilterId] = useState()
	const [isShowNegativeFilters, setIsShowNegativeFilters] = useState(false)

	const { module_id, filter_id } = useParams()

	const navigate = useNavigate()
	const { currentFilter, isNew } = useSelector(state => state.filter)
	const dispatch = useDispatch()


	const handleCurrentFilterType = (data) => {
		console.log(data)
	}


	const remove = () => {
		filterService
			.deleteFilter(filterId)
			.then(() => {
				setFilterId('')
			})
			.then(() => {
				dispatch(removeCurrentFilter())
			})
			.finally(() => {
				navigate(`/page/filters/${module_id}`)
			})

	}

	const handlePopup = (wordType) => {
		console.log(wordType)
		setWordstype(wordType)
		searchWords(wordType)
		setIsOpenPopup(true)
		console.log(isOpenPopup)
	}

	useEffect(() => {
		if (!isNew) {
			setFilterId(currentFilter.id)
		}
	}, [isNew])

	useEffect(() => {
		if (filter_id) {
			filterService
				.getCurrentFilter(module_id)
				.then(response => {
					setFilterId(filter_id)
					dispatch(
						setCurrentFilter({
							description: response.data.descriptionsDTO,
							title: response.data.titlesDTO,
							technologies: response.data.technologiesDTO,
							negativeDescription: response.data.negativeDescriptionsDTO,
							negativeTitle: response.data.negativeTechnologiesDTO,
							negativeTechnologies: response.data.negativeTitlesDTO,
							maxPrice: response.data.maxValue,
							minPrice: response.data.minValue,
							id: response.data.id,
							name: response.data.name
						})
					)
					dispatch(
						setIsNew({
							isNew: false
						})
					)

				})
		}
	}, [filter_id])



	const showNegativeFilters = () => {
		setIsShowNegativeFilters(prev => !prev)
	}

	const addNewFilter = () => {
		navigate(`/page/filters/${module_id}`)
	}

	return <div className='filtersPage'>
		<div className='container'>
			<Title text={'Добавление фильтров'} />

			<AddFilterForm setFilterId={setFilterId} module_id={module_id} />

			{filterId && <>
				<Filters />
				<Btn text={isShowNegativeFilters ? 'Скрыть негативные фильтра' : 'Показать негативные фильтры'}
					variant='contained' onClick={showNegativeFilters} className='filtersPage__show-negative' />
				{isShowNegativeFilters && <Filters type={'negative-'} />}
				<div className='filter__actions'>
					<Btn text={'Сохранить'} onClick={addNewFilter} variant='contained' />
				</div>
			</>

			}



		</div>
	</div>
}

export default AddFilterPage

