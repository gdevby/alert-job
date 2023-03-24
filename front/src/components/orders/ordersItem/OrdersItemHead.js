import React from 'react'

import Typography from '@mui/material/Typography';
import OrderDate from './OrderDate';

const OrdersItemHead = ({title, date, technologies, price}) => {
	
	return <>
		<Typography sx={{ width: '33%', wordBreak: 'break-word', display: 'flex', flexDirection: 'column', userSelect: 'text' }} component='div'>
			<Typography sx={{ wordBreak: 'break-word' }} component='div'>
				{title}
			</Typography>
			<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
				Дата: <OrderDate date={date}/>
			</Typography>
		</Typography>
		<Typography sx={{ width: '33%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
			{technologies.join(',')}
		</Typography>
		<Typography sx={{ width: '12%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }} component='div'>
			{price?.price || price?.value || 'не указана'}
		</Typography>
	</>
}

export default OrdersItemHead