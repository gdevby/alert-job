import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom';

import Field from '../../../components/common/field/Field'
import Btn from '../../../components/common/button/Button'

import { useDispatch, useSelector } from 'react-redux';

import { filterService } from '../../../services/parser/endponits/filterService'
import { setCurrentFilter } from '../../../store/slices/filterSlice';


const AddFilterForm = ({ setFilterId, module_id, updateFilter }) => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState('')
	const [maxPrice, setMaxPrice] = useState('')
	const [isAdded, setIsAdded] = useState(true)
	const [filterId, setId] = useState('')

	const { isChoose, isNew, currentFilter } = useSelector(state => state.filter)
	const dispatch = useDispatch()
	const navigate = useNavigate()

	const addFilter = event => {
		filterService
			.addFilter(module_id, { name: filterName, minValue: null, maxValue: null })
			.then(response => {
				const id = response.data.id
				setFilterId(id)
				setId(id)
				dispatch(
					setCurrentFilter({
						description: [],
						title: [],
						technologies: [],
						negativeDescription: [],
						negativeTitle: [],
						negativeTechnologies: [],
						maxPrice: '',
						minPrice: '',
						id: id,
						name: filterName
					})
				)
				return id
			})
			.then((id) => {
				filterService
					.updateCurrentFilter(module_id, id)
					.finally(() => {
						navigate(`/page/edit-filter/${module_id}/${id}`)
					})
			})
			.finally((response) => {
				setIsAdded(false)
			})
	}

	const updateCurrentFilter = (type) => {
		if (!isNew || isChoose || !isAdded) {
			const data = {
				name: type === 'name' ? filterName : null,
				minValue: minPrice == '' ? 0 : minPrice,
				maxValue: maxPrice == '' ? 0 : maxPrice,
			}
			updateFilter(data)
		}
	}

	useEffect(() => {
		if (!isNew || isChoose) {
			setFilterName(currentFilter.name)
			setMinPrice(currentFilter.minPrice || '')
			setMaxPrice(currentFilter.maxPrice || '')
		}
	}, [isChoose, isNew])

	useEffect(() => {
		setIsAdded(isNew)
	}, [isNew])

	return <div>
		<Field
			type={'text'} defaultValue={filterName} cb={setFilterName} onBlur={() => updateCurrentFilter('name')}
			placeholder={'Введите название'} label={<label>Сначала введите название фильтра</label>} />
		<div className='addFilter__button'>
			{isAdded ? <Btn text={'Добавить фильтр'} onClick={addFilter} /> : ''}
		</div>
		{!isAdded && <div className='price_block'>
			<Field
				type={'number'} defaultValue={minPrice} cb={setMinPrice} onBlur={() => updateCurrentFilter('minPrice')}
				placeholder={'Минимальная цена'} label={<label>Минимальная цена, лучше выставить низкую цену, т.к. заказчики не знают цену и могут установить 1000руб за дорогой проект</label>} />
			<Field
				type={'number'} defaultValue={maxPrice} cb={setMaxPrice} onBlur={() => updateCurrentFilter('maxPrice')}
				placeholder={'Максимальная цена'} label={<label>Максимальная цена</label>} />
		</div>}
	</div>
}

export default AddFilterForm