import React, { useState, useEffect } from 'react'

import Title from '../../components/title/Title'
import Field from '../../components/field/Field'
import DropDownList from '../../components/dropDownList/DropDowList'


const AddFilterPage = () => {
	const [filtersType, setFiltersType] = useState(['По совпадению', 'По совпадению с дополнение для команд'])
	const [currentFulterType, setCurrentFilterType] = useState('По совпадению')
	
	
	const handleCurrentFilterType = (data) =>{ 
		console.log(data)
	}
	
	return <div className='container'>
		<Title text={'Добавление фильтров'} />

		<Field
			type={'text'} defaultValue=''
			placeholder={'Введите название'} label={<label>Название фильтра</label>} />
			
		<DropDownList open={false} defaultValue={currentFulterType} elems={filtersType} cb={handleCurrentFilterType} />
		<div></div>
	</div>
}

export default AddFilterPage