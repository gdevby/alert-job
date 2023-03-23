import React from 'react'

import Moment from 'react-moment';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const OrdersItem = ({ order }) => {

	const getDate = (date) => {
		return <Moment format="HH:MM DD.MM.YYYY">
			{date}
		</Moment>
	}

	return <Accordion>
		<AccordionSummary
			expandIcon={<ExpandMoreIcon />}
			className='order__content'
		>
			<Typography sx={{ width: '33%', wordBreak: 'break-word', display: 'flex', flexDirection: 'column', userSelect: 'text' }} component='div'>
				<Typography sx={{ wordBreak: 'break-word' }} component='div' dangerouslySetInnerHTML={{ __html: order.title }}>
				</Typography>
				<Typography sx={{ wordBreak: 'break-word', userSelect: 'text' }} component='div'>
					Дата: {getDate(order.dateTime)}
				</Typography>
			</Typography>
			<Typography sx={{ width: '33%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }}>
				{order.technologies.join(',')}
			</Typography>
			<Typography sx={{ width: '12%', flexShrink: 0, wordBreak: 'break-word', userSelect: 'text' }}>
				{order.price?.price || 'не указана'}
			</Typography>
		</AccordionSummary>
		<AccordionDetails>
			<Typography className='order__message' dangerouslySetInnerHTML={{ __html: order.message }}>
			</Typography>
			<Typography>
				Ссылка на заказ: <OrderLink link={order.link} />
			</Typography>
		</AccordionDetails>
	</Accordion>
}

const OrderLink = ({ link }) => {
	return <Typography component='a' target='_blank' href={link} sx={{ 'paddingLeft': 0, 'color': '#3d53f9;' }}>
		{link}
	</Typography>
}

export default OrdersItem