import React, { useState } from 'react'
import {  useParams } from 'react-router-dom'

import OrdersList from '../../../components/ordersList/OrdersList'
import Btn from '../../../components/button/Button'

import { ordersService } from '../../../services/parser/endponits/orderService'

import './orders.scss'



const Orders = () => {
	const [orders, setOrders] = useState([])
	const [isShowingOrders, setIsShowingOrders] = useState(false)

	const { id } = useParams()

	const showOrders = () => {
		if (!isShowingOrders) {
			ordersService
				.getOrders(id)
				.then((response) => {
					setOrders(response.data)
				})
				.finally(() => setIsShowingOrders(true))
		}else {
			setIsShowingOrders(false)
		}

	}

	return <div className='orders'>
		<div className='orders__actions'>
			<Btn onClick={showOrders} text={!isShowingOrders? 'Показать заказы': 'Скрыть заказы'} variant='contained' />
		</div>
		{isShowingOrders && <OrdersList orders={orders} />}
	</div>
}


export default Orders