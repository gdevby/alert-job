import React, { useState, useEffect } from 'react'

import Title from '../../components/title/Title'
import Field from '../../components/field/Field'
import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'
import InstructionForTg from '../../components/instructionForTg/InstructionForTg'

import { coreService } from '../../services/parser/endponits/coreService'

import './notificationsPage.scss'

const NotificationsPage = () => {
	const [platforms, setPlatforms] = useState([{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }])
	const [currentPlatform, setCurrentPlatform] = useState('')
	const [alertStatus, setAlertStatus] = useState(false)
	const [alertType, setAlertType] = useState(0)
	const [telegramId, setTelegramId] = useState('')


	const sendTestNotification = () => {
		coreService.sendTestMessage().then(console.log)
	}

	const handleCurrentPlatform = (data) => {
		setCurrentPlatform(data.name)
		coreService.changeAlertsType(data.name == 'email')
	}

	const handleAlertsStatus = e => {
		coreService.changeAlertsStatus(!alertStatus).then(() => setAlertStatus(!alertStatus))
	}

	const saveTgId = () => {
		if (alertType != '') {
			coreService.changeTgId(alertType).then(console.log)
		}

	}


	useEffect(() => {
		coreService.getStatue().then(response => setAlertStatus(response.data))
		coreService.getAlertType().then(response => {
			if (response.data.type) {
				setCurrentPlatform('email')
			}else {
				setCurrentPlatform('telegram')
				setTelegramId(response.data.value)
			}
		})
	}, [])


	return <div className='notification_page'>
		<div className='container'>
			<Title text='Настройка уведомлений' />
			<div className='notification_source'>
				<DropDownList open={false} defaultValue={currentPlatform} elems={platforms} cb={handleCurrentPlatform} />
				{currentPlatform == 'telegram' ?
					<div><Field 
						defaultValue={telegramId} type='text' 	
						placeholder='Введите адрес' cb={setAlertType} 
						onBlur={saveTgId} label={<label className="label">Введите айди </label>}/>
					
					</div>
					: 'Используется почта при регистрации аккаунта'}
				<div className='notification_source__send-btn'>
					<Button text={'Отправить тестовое уведомление'} onClick={sendTestNotification} />
				</div>
			</div>
			<div className='alert_status'>
				<input type='checkbox' onChange={handleAlertsStatus} checked={alertStatus} />
				{alertStatus? 'Включено': 'Отключено'}
			</div>
			{currentPlatform == 'telegram' && <InstructionForTg />}
		</div>
	</div>
}


export default NotificationsPage