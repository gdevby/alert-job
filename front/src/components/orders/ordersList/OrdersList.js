import React from 'react'
import OrdersItem from './OrdersItem'

const OrdersList = ({ orders }) => {

	return <div className='orders__list'>
		{orders.map(item => <OrdersItem key={item.dateTime} order={item} />)}
	</div>
}

export default OrdersList