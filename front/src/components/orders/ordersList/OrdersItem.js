import React, { useEffect } from 'react'
import { useResize } from '../../../hooks/useResize';

import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import MobileOrderItemHead from '../../mobile/mobileOrderItem/MobileOrderItemHead';
import OrdersItemHead from '../ordersItem/OrdersItemHead';

const OrdersItem = ({ order }) => {
	const { width } = useResize()

	useEffect(() => {
		console.log(width)
	}, [width])

	const OrderLink = ({ link }) => {
		return <Typography component='a' target='_blank' href={link} sx={{ 'paddingLeft': 0, 'color': '#3d53f9;' }}>
			{link}
		</Typography>
	}

	return <Accordion>
		<AccordionSummary
			expandIcon={<ExpandMoreIcon />}
			className='order__content'
		>
			{width <= 625? <MobileOrderItemHead {...order}/> : <OrdersItemHead {...order}/>}
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



export default OrdersItem