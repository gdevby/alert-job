import React from 'react'

import Sources from '../../layouts/filtersPage/sources/Sources'
import Orders from '../../layouts/filtersPage/orders/Orders'
import CurrentFilter from '../../layouts/filtersPage/currentFilter/CurrentFilter';

import './filtersPage.scss'

const FiltersPage = () => {
	return <div className='filtersPage'>
		<div className='container'>
			<Sources />
			<CurrentFilter />
			<Orders />
		</div>
	</div>
}

export default FiltersPage