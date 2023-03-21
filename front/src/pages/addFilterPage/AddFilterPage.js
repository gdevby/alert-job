import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom'

import Title from '../../components/common/title/Title'
import AddFilterForm from '../../layouts/addFilterPage/addFilterForm/AddFilterForm'
import Btn from '../../components/common/button/Button'
import Filters from '../../layouts/addFilterPage/filters/Filter';

import { filterService } from '../../services/parser/endponits/filterService';

import { removeCurrentFilter, setIsNew, setCurrentFilter, setActivatedNegativeFilters } from '../../store/slices/filterSlice';

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
					setIsShowNegativeFilters(response.data.activatedNegativeFilters)
					setFilterId(filter_id)
					dispatch(
						setCurrentFilter({
							description: response.data.descriptionsDTO,
							title: response.data.titlesDTO,
							technologies: response.data.technologiesDTO,
							negativeDescription: response.data.negativeDescriptionsDTO,
							negativeTitle: response.data.negativeTitlesDTO,
							negativeTechnologies: response.data.negativeTechnologiesDTO,
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
		filterService
			.updateFilter(module_id, filter_id, { activatedNegativeFilters: !isShowNegativeFilters })
			.then(console.log)
			.finally(() => {
				dispatch(setActivatedNegativeFilters({ activatedNegativeFilters: !isShowNegativeFilters }))
				setIsShowNegativeFilters(prev => !prev)
			})
	}

	const updateFilter = (data) => {
		filterService
			.updateFilter(module_id, filter_id, {
				...data,
				activatedNegativeFilters: isShowNegativeFilters
			})
	}

	const addNewFilter = () => {
		navigate(`/page/filters/${module_id}`)
	}



	return <div className='filtersPage'>
		<div className='container'>
			<Title text={'Добавление фильтров'} />

			<AddFilterForm setFilterId={setFilterId} module_id={module_id} updateFilter={updateFilter} />

			{filterId && <>
				<Filters />
				<p>Негативные фильтры фильтруют заказы, которые проходят по фильтрам выше, то есть дополнительное фильтрование.</p>
				<Btn text={isShowNegativeFilters ? 'Негативные фильтры активны' : 'Негативные фильтры неактивны'}
					color={isShowNegativeFilters ? 'success' : 'error'} variant='contained' onClick={showNegativeFilters} className='filtersPage__show' />
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

