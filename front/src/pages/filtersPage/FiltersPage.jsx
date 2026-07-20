import React from 'react'

import Sources from '../../layouts/filtersPage/sources/Sources'
import Orders from '../../layouts/filtersPage/orders/Orders'
import CurrentFilter from '../../layouts/filtersPage/currentFilter/CurrentFilter';
import { Bindings } from '@/modules/auto-replies/components/Bindings';
import { Divider } from '@mui/material';

import './filtersPage.scss'

const FiltersPage = () => {
	return <div className='filtersPage'>
		<div className='container sections'>
			<Sources />
			<Divider  />
			<CurrentFilter />
			<Divider  />
			<Bindings />
			<Divider  />
			<Orders />
		</div>
	</div>
}

export default FiltersPage