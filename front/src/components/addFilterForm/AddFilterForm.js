import React, { useEffect, useState } from 'react'

import Field from '../../components/field/Field'
import Button from '../button/Button'

import { useDispatch, useSelector } from 'react-redux';

import { filterService } from '../../services/parser/endponits/filterService'

const AddFilterForm = ({ setFilterId }) => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState(0)
	const [maxPrice, setMaxPrice] = useState(0)
	const [isAdded, setIsAdded] = useState(true)
	const [filterId, setId] = useState('')

	const { isChoose, isNew, currentFilter } = useSelector(state => state.filter)

	const addFilter = event => {
		filterService
			.addFilter({ name: filterName, minValue: minPrice, maxValue: maxPrice })
			.then(response => {
				const id = response.data.id
				setFilterId(id)
				setId(id)
			})
			.finally(() => setIsAdded(false))
	}

	const updateFilter = event => {
		if (!isNew && isChoose) {
			filterService
				.updateFilter(currentFilter.id, { name: filterName, minValue: minPrice, maxValue: maxPrice })
				.then(console.log)
		}
	}

	useEffect(() => {
		console.log(isNew, isChoose)
		if (!isNew && isChoose) {
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

		<div className='price_block'>
			<Field
				type={'number'} defaultValue={minPrice} cb={setMinPrice}
				placeholder={'Минимальная цена'} label={<label>Минимальная цена</label>} />
			<Field
				type={'number'} defaultValue={maxPrice} cb={setMaxPrice}
				placeholder={'Максимальная цена'} label={<label>Максимальная цена</label>} />
		</div>
		<div>
			{isAdded? <Button text={'Добавить фильтр'} onClick={addFilter} /> : ''}
		</div>
	</div>
}

export default AddFilterForm