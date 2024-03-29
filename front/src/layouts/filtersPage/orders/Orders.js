import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useSelector } from 'react-redux';

import OrdersList from '../../../components/orders/ordersList/OrdersList'
import Btn from '../../../components/common/button/Button'
import CircularProgress from '@mui/material/CircularProgress';
import ReplayIcon from '@mui/icons-material/Replay';
import Period from '../../../components/orders/period/Period';
import Popup from '../../../components/common/popup/Popup';

import { ordersService } from '../../../services/parser/endponits/orderService'

import './orders.scss'



const Orders = () => {
	const [orders, setOrders] = useState([])
	const [isShowingOrders, setIsShowingOrders] = useState(false)
	const [isFetching, setIsFetching] = useState()
	const [ordersType, setOrdersType] = useState(true)
	const [period, setPeriod] = useState(7)
	const [popup, setPopup] = useState({})
	const [isOpenPopup, setIsOpenPopup] = useState(false)

	const { id } = useParams()
	const { isChoose } = useSelector(state => state.filter)

	const showOrders = (type = ordersType) => {
		if (!isChoose) {
			setPopup({
				title: 'У вас не установлен фильтр',
				content: `Чтобы получать заказы, вам надо добавить текущий фильтр`,
				actions: <>
					<Btn onClick={handleClosePopup} text={'Закрыть'} />
				</>
			})
			setIsOpenPopup(true)
			return
		}
		setOrdersType(type)
		setIsShowingOrders(true)
		setIsFetching(true)
	}

	const handleClosePopup = () => {
		setIsOpenPopup(false)
	}

	const getOrders = () => {
		ordersService
			.getOrders(id, ordersType, period)
			.then((response) => {
				setOrders(response.data)
			})
			.finally(() => {
				setIsFetching(false)
			})
	}

	useEffect(() => {
		if (isFetching) {
			getOrders()
		}
	}, [isFetching])

	const updatePeriod = (period) => {
		setPeriod(period)
	}

	const Empty = () => {
		return <div className='orders__list_empty'>
			<span>Заказы возможно еще не обновились, если вы только что добавили новый источник, подождите 10 минут.</span>
			<ReplayIcon className='orders__list_empty_icon' onClick={() => setIsFetching(true)} />
		</div>
	}

	return <div className='orders'>
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup}
			title={popup.title}
			content={popup.content}
			actions={popup.actions}
		/>
		<Period updatePeriod={updatePeriod} />
		<div className='orders__actions'>
			<Btn onClick={() => showOrders(true)} text={'Показать заказы, о которых вы были бы уведомлены'} variant='contained' />
			{(isShowingOrders && orders.length != 0) && <ReplayIcon className='orders__list_empty_icon' onClick={() => setIsFetching(true)} />}
			<Btn onClick={() => showOrders(false)} text={'Показать заказы, которые вы не получили'} color={'error'} variant='contained' />
		</div>
		{(isShowingOrders && orders.length > 0) && <div className='orders__list-head'><div>Название</div><div>Технологии</div><div>Категории</div><div>Цена</div></div>}
		{
			isShowingOrders && (isFetching ? <div style={{ textAlign: 'center', marginTop: '.5rem' }}><CircularProgress /></div> : (orders.length == 0 ? <Empty /> : <OrdersList orders={orders} />))
		}

	</div>
}




export default Orders