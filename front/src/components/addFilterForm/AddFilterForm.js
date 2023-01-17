import React, { useState } from 'react'

import Field from '../../components/field/Field'
import Button from '../button/Button'

import { filterService } from '../../services/parser/endponits/filterService'

const AddFilterForm = ({ setFilterId }) => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState(0)
	const [maxPrice, setMaxPrice] = useState(0)
	
	
	const addFilter = event => {
		event.preventDefault()
		filterService
		.addFilter({name: filterName, minValue: minPrice, maxValue: maxPrice})
		.then(response => setFilterId(response.data.id))
	}
	
	
	return <form>
		<Field
			type={'text'} defaultValue='' cb={setFilterName}
			placeholder={'Введите название'} label={<label>Название фильтра</label>} />

		<div className='price_block'>
			<Field
				type={'number'} defaultValue='' cb={setMinPrice}
				placeholder={'Минимальная цена'} label={<label>Минимальная цена</label>} />
			<Field
				type={'number'} defaultValue='' cb={setMaxPrice}
				placeholder={'Максимальная цена'} label={<label>Максимальная цена</label>} />
		</div>
		<div>
			<Button text={'Добавить фильтр'} onClick={addFilter}/>
		</div>
	</form>
}

export default AddFilterForm