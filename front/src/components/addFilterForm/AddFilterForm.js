import React, { useEffect, useState } from 'react'

import Field from '../../components/field/Field'
import Button from '../button/Button'

import { useDispatch, useSelector } from 'react-redux';

import { filterService } from '../../services/parser/endponits/filterService'
import { setCurrentFilter } from '../../store/slices/filterSlice';

const AddFilterForm = ({ setFilterId }) => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState('')
	const [maxPrice, setMaxPrice] = useState('')
	const [isAdded, setIsAdded] = useState(true)
	const [filterId, setId] = useState('')

	const { isChoose, isNew, currentFilter } = useSelector(state => state.filter)
	const dispatch = useDispatch()

	const addFilter = event => {
		filterService
			.addFilter({ name: filterName, minValue: 0, maxValue: 0 })
			.then(response => {
				const id = response.data.id
				setFilterId(id)
				setId(id)
				dispatch(
					setCurrentFilter({
						description: [],
						title: [],
						technologies: [],
						maxPrice: '',
						minPrice: '',
						id: id,
						name: filterName
					})
				)
			})
			.finally(() => setIsAdded(false))
	}

	const updateFilter = event => {
		console.log(!isNew || isChoose)
		console.log(!isNew, isChoose)
		if (!isNew || isChoose || !isAdded) {
			console.log('dfdsfs')
			filterService
				.updateFilter(currentFilter.id, { name: filterName, minValue: minPrice, maxValue: maxPrice })
				.then(console.log)
		}
	}

	useEffect(() => {
		if (!isNew || isChoose) {
			console.log('current filter', currentFilter)
			setFilterName(currentFilter.name)
			setMinPrice(currentFilter.minPrice)
			setMaxPrice(currentFilter.maxPrice)
		}
	}, [isChoose, isNew])

	useEffect(() => {
		console.log('isNew', isNew)
		setIsAdded(isNew)
	}, [isNew])


	return <div>
		<Field
			type={'text'} defaultValue={filterName} cb={setFilterName} onBlur={updateFilter}
			placeholder={'Введите название'} label={<label>Сначала введите название фильтра</label>} />
		<div className='addFilter__button'>
			{isAdded ? <Button text={'Добавить фильтр'} onClick={addFilter} /> : ''}
		</div>
		{!isAdded && <div className='price_block'>
			<Field
				type={'number'} defaultValue={minPrice} cb={setMinPrice} onBlur={updateFilter}
				placeholder={'Минимальная цена'} label={<label>Минимальная цена</label>} />
			<Field
				type={'number'} defaultValue={maxPrice} cb={setMaxPrice} onBlur={updateFilter}
				placeholder={'Максимальная цена'} label={<label>Максимальная цена</label>} />
		</div>}

	</div>
}

export default AddFilterForm