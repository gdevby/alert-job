import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useSelector } from 'react-redux';

import OrdersList from '../../../components/orders/ordersList/OrdersList'
import Btn from '../../../components/common/button/Button'
import CircularProgress from '@mui/material/CircularProgress';
import ReplayIcon from '@mui/icons-material/Replay';
import Period from '../../../components/orders/period/Period';
import Popup from '../../../components/common/popup/Popup';
import DropDownList from '../../../components/common/dropDownList/DropDowList';

import { ordersService } from '../../../services/parser/endponits/orderService'
import { parserService } from '../../../services/parser/endponits/parserService';

import './orders.scss'



const Orders = () => {
	const [sites, setSites] = useState([])
	const [currentSite, setCurrentSite] = useState('')
	const [orders, setOrders] = useState([])
	const [isShowingOrders, setIsShowingOrders] = useState(false)
	const [isFetching, setIsFetching] = useState()
	const [ordersType, setOrdersType] = useState(true)
	const [period, setPeriod] = useState(7)
	const [popup, setPopup] = useState({})
	const [isOpenPopup, setIsOpenPopup] = useState(false)

	const { id } = useParams()
	const { isChoose } = useSelector(state => state.filter)

	useEffect(() => {
		parserService
			.getSites()
			.then(response => {
				const additionalItem = { id: 0, name: 'ВСЕ ИСТОЧНИКИ' };
				setSites([additionalItem, ...response.data])
				setCurrentSite(additionalItem)
			})
	}, [])

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

	const filteredOrders = currentSite && currentSite.id !== 0 ? orders.filter(({ sourceSite }) => sourceSite.sourceName == currentSite.name) : orders

	return <div className='orders'>
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup}
			title={popup.title}
			content={popup.content}
			actions={popup.actions}
		/>
		<Period updatePeriod={updatePeriod} />
		<div style={{marginBottom: 16, maxWidth: 256}}>
			<DropDownList defaultValue={currentSite?.id} label={'Источник'} elems={sites} onClick={setCurrentSite} defaultLabe={'Источник'} />
		</div>
		<div className='orders__actions'>
			<Btn onClick={() => showOrders(true)} text={'Показать заказы, о которых вы были бы уведомлены'} variant='contained' />
			{(isShowingOrders && filteredOrders.length != 0) && <ReplayIcon className='orders__list_empty_icon' onClick={() => setIsFetching(true)} />}
			<Btn onClick={() => showOrders(false)} text={'Показать заказы, которые вы не получили'} color={'error'} variant='contained' />
		</div>
		{(isShowingOrders && filteredOrders.length > 0) && <div className='orders__list-head'><div>Название</div><div>Категории</div><div>Цена</div></div>}
		{
			isShowingOrders && (isFetching ? <div style={{ textAlign: 'center', marginTop: '.5rem' }}><CircularProgress /></div> : (filteredOrders.length == 0 ? <Empty /> : <OrdersList orders={filteredOrders} />))
		}

	</div>
}




export default Orders