import React, { useState, useEffect, useReducer } from 'react'

import Title from '../../components/title/Title'
import Field from '../../components/field/Field'
import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { coreService } from '../../services/parser/endponits/coreService'

import './notificationsPage.scss'

const NotificationsPage = () => {
	const [platforms, setPlatforms] = useState([{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }])
	const [currentPlatform, setCurrentPlatform] = useState({})
	const [alertStatus, setAlertStatus] = useState(false)
	const [alertType, setAlertType] = useState('')


	const sendTestNotification = () => {
		coreService.sendTestMessage().then(console.log)
	}
	
	const handleCurrentPlatform = (data) => {
		console.log(data)
		setCurrentPlatform(data)
		coreService.changeAlertsType(data.name == 'email')
	}
	
	const handleAlertsStatus = e => {
		coreService.changeAlertsStatus(!alertStatus).then(() => setAlertStatus(!alertStatus))
		
	}

	useEffect(() => {
		coreService.getStatue().then(response => setAlertStatus(response.data))
	}, [])
	

	return <div className='notification_page'>
		<div className='container'>
			<Title text='Настройка уведомлений' />
			<div className='notification_source'>
				<DropDownList open={false} defaultValue={'email'} elems={platforms} cb={handleCurrentPlatform} />
				{currentPlatform == 'telegram'? <Field type='text' placeholder='Введите адрес' cb={setAlertType}/> : 'Используется почта при регистрации аккаунта'}
				<div className='notification_source__send-btn'>
					<Button text={'Отправить тестовое уведомление'} onClick={sendTestNotification} />
				</div>
			</div>
			<div>
				<input type='checkbox' onChange={handleAlertsStatus} checked={alertStatus}/>
				Отключить
			</div>




		</div>
	</div>
}


export default NotificationsPage