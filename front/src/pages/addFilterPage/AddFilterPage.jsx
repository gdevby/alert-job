import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom'

import Title from '../../components/common/title/Title'
import AddFilterForm from '../../layouts/addFilterPage/addFilterForm/AddFilterForm'
import Btn from '../../components/common/button/Button'
import Filters from '../../layouts/addFilterPage/filters/Filter';

import { filterService } from '../../services/parser/endponits/filterService';

import { setIsNew, setCurrentFilter, setActivatedNegativeFilters } from '../../store/slices/filterSlice';

import './addFilterPage.scss'

const AddFilterPage = () => {
	const [filterId, setFilterId] = useState()
	const [isShowNegativeFilters, setIsShowNegativeFilters] = useState(false)

	const { module_id, filter_id } = useParams()

	const navigate = useNavigate()
	const { currentFilter, isNew } = useSelector(state => state.filter)
	const dispatch = useDispatch()

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
							negativeDescription: response.data.negativeDescriptionsDTO,
							negativeTitle: response.data.negativeTitlesDTO,
							descriptionWordPrice: response.data.descriptionWordPrice,
							maxPrice: response.data.maxValue,
							minPrice: response.data.minValue,
							id: response.data.id,
							name: response.data.name,
							openForAll: response.data.openForAll
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
			<Title text={filterId? 'Редактирование фильтра' : 'Добавление фильтров'} />
			<Btn text={'Вернуться к модулю'} onClick={addNewFilter} variant='contained' />
			<AddFilterForm updateFilter={updateFilter} />
			{filterId && <>
				<Filters />
				<p className='mt-1'>Негативные фильтры(слова минусы) фильтруют заказы, которые проходят по фильтрам выше, то есть дополнительное фильтрование.</p>
				<Btn text={isShowNegativeFilters ? 'Негативные фильтры активны' : 'Негативные фильтры неактивны'}
					color={isShowNegativeFilters ? 'success' : 'error'} variant='contained' onClick={showNegativeFilters} className='filtersPage__show' />
				{isShowNegativeFilters && <Filters type={'negative-'} />}
				<div className='filter__actions'>

				</div>
			</>
			}
		</div>
	</div>
}

export default AddFilterPage

