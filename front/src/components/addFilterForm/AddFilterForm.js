import React, { useState } from 'react'

import Field from '../../components/field/Field'

import { filterService } from '../../services/parser/endponits/filterService'

const AddFilterForm = () => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState(0)
	const [maxPrice, setMaxPrice] = useState(0)
	
	
	const addFilter = event => {
		filterService.addFilter({name: filterName, minValue: minPrice, maxValue: maxPrice})
	}
	
	
	return <form onBlur={addFilter}>
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
	</form>
}

export default AddFilterForm