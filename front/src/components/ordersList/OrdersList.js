import React from 'react'
import OrdersItem from './OrdersItem'

const OrdersList = ({ orders }) => {
	
	
	return <div className='orders__list'>
		{orders.map(item => <OrdersItem key={item.link} order={item}/>)}
	</div>
}

export default OrdersList