import React from 'react'
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const OrdersItem = ({ order }) => {

	return <Accordion>
		<AccordionSummary
			expandIcon={<ExpandMoreIcon />}
			className='order__content'
		>
			<Typography sx={{ width: '33%', flexShrink: 0, wordBreak: 'break-word' }}>
				{order.title}
			</Typography>
			<Typography sx={{ width: '33%', flexShrink: 0, wordBreak: 'break-word' }}>
				{order.technologies.join(',')}
			</Typography>
			<Typography sx={{ width: '12%', flexShrink: 0, wordBreak: 'break-word' }}>
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