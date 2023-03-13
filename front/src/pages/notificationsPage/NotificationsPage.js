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
	const [alertType, setAlertType] = useState('')
	const [telegramId, setTelegramId] = useState('')


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

	const saveTgId = () => {
		if (alertType != '') {
			coreService.changeTgId(telegramId).then(console.log)
		}

	}


	useEffect(() => {
		coreService.getStatue().then(response => setAlertStatus(response.data))
		coreService.getAlertType().then(response => {
			if (response.data.type) {
				setCurrentPlatform({ name: 'email', id: 1 })
			}else {
				setCurrentPlatform({ name: 'telegram', id: 2 })
				setTelegramId(response.data.value == 'null'? '' : response.data.value)
			}
		})
	}, [])
	
	const handleValue = (text) => {
		setAlertType(text)
		setTelegramId(text)
	}


	return <div className='notification_page'>
		<div className='container'>
			<Title text='Настройка уведомлений' />
			<div className='notification_source'>
				<DropDownList open={false} label={'Тип уведомлений'} defaultValue={currentPlatform.id} elems={platforms} onClick={handleCurrentPlatform} defaultLabe='Тип уведомлений'/>
				{currentPlatform.name == 'telegram' ?
					<div><Field 
						defaultValue={telegramId} type='text' 	
						placeholder='Введите адрес' cb={handleValue} 
						label={<label className="label">Введите айди </label>}/>
						
					</div>
					: <p>Используется почта при регистрации аккаунта</p>}
				<div className='notification_source__send-btn'>
					{currentPlatform.name == 'telegram' && <Button text={'Сохранить'} onClick={saveTgId} variant='contained'/>}
					<Button text={'Отправить тестовое уведомление'} onClick={sendTestNotification} variant='contained'/>
					
				</div>
			</div>
			
			<div className='alert_status'>
				<input type='checkbox' onChange={handleAlertsStatus} checked={alertStatus} />
				{alertStatus? 'Включено': 'Отключено'}
			</div>
			{currentPlatform.name == 'telegram' && <InstructionForTg />}
		</div>
	</div>
}


export default NotificationsPage