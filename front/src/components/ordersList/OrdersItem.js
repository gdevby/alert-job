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
			<Typography sx={{ width: '45%', flexShrink: 0 }}>
				{order.title}
			</Typography>
			<Typography>{order.technologies.join(',')}</Typography>
		</AccordionSummary>
		<AccordionDetails>
			<Typography className='order__message' dangerouslySetInnerHTML={{__html: order.message}}>
			</Typography>
			<Typography>
				Ссылка на заказ: <OrderLink link={order.link}/>
			</Typography>
			<Typography>
				Цена: {order.price?.price || 'не указана'}
			</Typography>
		</AccordionDetails>
	</Accordion>
}

const OrderLink = ({link}) => {
	return <Typography component='a' href={link} sx={{ 'paddingLeft': 0, 'color': '#3d53f9;' }}>
				{link}
			</Typography>
}

export default OrdersItem