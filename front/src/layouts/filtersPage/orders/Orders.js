import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'

import OrdersList from '../../../components/ordersList/OrdersList'
import Btn from '../../../components/button/Button'
import CircularProgress from '@mui/material/CircularProgress';
import ReplayIcon from '@mui/icons-material/Replay';

import { ordersService } from '../../../services/parser/endponits/orderService'

import './orders.scss'



const Orders = () => {
	const [orders, setOrders] = useState([])
	const [isShowingOrders, setIsShowingOrders] = useState(false)
	const [isFetching, setIsFetching] = useState()

	const { id } = useParams()

	const showOrders = () => {
		if (!isShowingOrders) {
			setIsFetching(true)
		} else {
			setIsShowingOrders(false)
		}
	}

	const getOrders = () => {
		ordersService
			.getOrders(id)
			.then((response) => {
				setOrders(response.data)
			})
			.finally(() => {
				setIsShowingOrders(true)
				setIsFetching(false)
			})
	}

	useEffect(() => {
		if (isFetching) {
			getOrders()
		}
	}, [isFetching])
	
	const Empty = () => {
		return <div className='orders__list_empty'>
			Заказов нет <ReplayIcon className='orders__list_empty_icon' onClick={() => setIsFetching(true)} />
		</div>
	}

	return <div className='orders'>
		<div className='orders__actions'>
			<Btn onClick={showOrders} text={!isShowingOrders ? 'Показать заказы' : 'Скрыть заказы'} variant='contained' />
			{isShowingOrders && <ReplayIcon className='orders__list_empty_icon' onClick={() => setIsFetching(true)} />}
		</div>
		{
			isShowingOrders && (isFetching ? <CircularProgress /> : (orders.length == 0 ? <Empty /> : <OrdersList orders={orders} />))
		}

	</div>
}




export default Orders