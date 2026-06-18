import React from 'react'

import Typography from '@mui/material/Typography';
import OrderDate from './OrderDate';

const OrdersItemHead = (props) => {

	const { title, dateTime,  price, sourceSite } = props.order

	return <>
		<Typography sx={{ width: '33%', wordBreak: 'break-word', display: 'flex', flexDirection: 'column', userSelect: 'text' }} component='div'>
			<Typography sx={{ wordBreak: 'break-word' }} component='div'>
				{title}
			</Typography>
			<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Дата: <OrderDate date={dateTime} />
			</Typography>
			<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Источник: {sourceSite.sourceName}
			</Typography>
		</Typography>
		<Typography sx={{ width: '23%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
			{ sourceSite.categoryName } <br/> {sourceSite.subCategoryName || ''}
		</Typography>
		<Typography sx={{ width: '12%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
			{price?.price || price?.value || 'не указана'}
		</Typography>
	</>
}

export default OrdersItemHead