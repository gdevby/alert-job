import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom'

import DropDownList from '../../../components/dropDownList/DropDowList';
import Btn from '../../../components/button/Button'

import { filterService } from '../../../services/parser/endponits/filterService'

import { setCurrentFilter, setIsNew, removeCurrentFilter } from '../../../store/slices/filterSlice'

const CurrentFilter = () => {
	const [currentFilters, setCurrentFilters] = useState([])
	const [filter, setFilter] = useState('')

	const { id } = useParams()

	const dispatch = useDispatch()
	const navigate = useNavigate()
	
	const { currentFilter, isChoose } = useSelector(state => state.filter)

	useEffect(() => {
		console.log(isChoose)
		if (isChoose) {
			setFilter(currentFilter)
		}
	}, [isChoose])


	const addNewFilter = () => {
		dispatch(
			setIsNew({
				isNew: true
			})
		)
		dispatch(removeCurrentFilter())
		navigate(`/page/adding-filter/${id}`)
	}

	const editFilter = () => {
		dispatch(
			setIsNew({
				isNew: false
			})
		)
		navigate(`/page/edit-filter/${id}/${filter.id}`)
	}

	const removeFilter = () => {
		filterService
			.deleteFilter(id, filter.id)
			.then(() => {
				setCurrentFilters(prev => prev.filter(item => item.id != filter.id))
				dispatch(removeCurrentFilter())
				setFilter('')
			})
	}

	const handleCurrentFilter = data => {
		filterService
			.updateCurrentFilter(id, data.id)
			.then(() => {
				setFilter(data)
				dispatch(
					setCurrentFilter({
						description: data.descriptionsDTO,
						title: data.titlesDTO,
						technologies: data.technologiesDTO,
						maxPrice: data.maxValue,
						minPrice: data.minValue,
						id: data.id,
						name: data.name
					})
				)
			})
	}

	useEffect(() => {
		filterService
			.getFilters(id)
			.then(response => {
				setCurrentFilters(response.data)
			})

		filterService
			.getCurrentFilter(id)
			.then((response) => {
				if (response.data !== '') {
					setFilter(response.data)
					dispatch(
						setCurrentFilter({
							description: response.data.descriptionsDTO,
							title: response.data.titlesDTO,
							technologies: response.data.technologiesDTO,
							maxPrice: response.data.maxValue,
							minPrice: response.data.minValue,
							id: response.data.id,
							name: response.data.name
						})
					)
				}

			})
	}, [])


	return <div className='current_filter'>
		<div className='current_filter__title'>Теперь создайте фильтр с помощью кнопки "Добавить новый фильтр", который будет заказам из источника заказов</div>
		<div className='current_filter__content'>
			<DropDownList className='current_filter__list' defaultValue={filter.id} label={'Выберите фильтр'} elems={currentFilters} onClick={handleCurrentFilter} defaultLabe={'Выберите фильтр'} />
			{filter && <div className='current_filter__content-actions'>
				<Btn onClick={editFilter} text={'Редактировать фильтр'} variant='contained' />
				<Btn onClick={removeFilter} text={'Удалить фильтр'} variant='contained' />
			</div>}
			<div>
				<Btn onClick={addNewFilter} text={'Добавить новый фильтр'} variant='contained' />
			</div>
		</div>
	</div>
}

export default CurrentFilter