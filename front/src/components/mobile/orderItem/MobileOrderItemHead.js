import React from 'react'

import Typography from '@mui/material/Typography';
import OrderDate from '../../orders/ordersItem/OrderDate';

const MobileOrderItemHead = (props) => {
	
	const { title, dateTime,  price, sourceSite } = props.order;
	
	return <>
		<Typography sx={{ width: '100%', wordBreak: 'break-word', display: 'flex', flexDirection: 'column', userSelect: 'text' }} component='div'>
			<Typography sx={{ wordBreak: 'break-word' }} component='div'>
				Название: {title}
			</Typography>
			<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Дата: <OrderDate date={dateTime} />
			</Typography>
			<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Источник: {sourceSite.sourceName}
			</Typography>
			<Typography sx={{ width: '100%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Категории: { sourceSite.categoryName } { sourceSite.subCategoryName?  ', ' + sourceSite.subCategoryName :  '' }
			</Typography>
			<Typography sx={{ width: '100%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Цена: {price?.price || price?.value || 'не указана'}
			</Typography>
		</Typography>
	</>
}

export default MobileOrderItemHead;