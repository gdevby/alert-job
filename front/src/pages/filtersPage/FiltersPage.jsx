import React from 'react'

import { useAutoReplyStatus } from '@/hooks/useAutoReplyStatus';
import Sources from '../../layouts/filtersPage/sources/Sources'
import Orders from '../../layouts/filtersPage/orders/Orders'
import CurrentFilter from '../../layouts/filtersPage/currentFilter/CurrentFilter';
import { Bindings } from '@/modules/auto-replies/components/Bindings';
import { Divider } from '@mui/material';

import './filtersPage.scss'

const FiltersPage = () => {
	const autoReplyStatus = useAutoReplyStatus();

	return <div className='filtersPage'>
		<div className='container sections'>
			<Sources />
			<Divider  />
			<CurrentFilter />
			<Divider  />
			{autoReplyStatus && (
        <>
          <Bindings />
          <Divider />
        </>
      )}
			<Orders />
		</div>
	</div>
}

export default FiltersPage