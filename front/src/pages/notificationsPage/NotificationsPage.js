import React, { useState, useEffect } from 'react'

import Title from '../../components/common/title/Title'
import TextField from '@mui/material/TextField';
import DropDownList from '../../components/common/dropDownList/DropDowList'
import Button from '../../components/common/button/Button'
import InstructionForTg from '../../components/notification/instructionForTg/InstructionForTg'
import Switch from '@mui/material/Switch';

import { coreService } from '../../services/parser/endponits/coreService'
import { changeAuthStatus } from '../../hooks/changeAuthStatus';

import './notificationsPage.scss'

const NotificationsPage = () => {
	const [platforms, setPlatforms] = useState([{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }])
	const [currentPlatform, setCurrentPlatform] = useState('')
	const [alertStatus, setAlertStatus] = useState(false)
	const [alertType, setAlertType] = useState('')
	const [telegramId, setTelegramId] = useState('')

	const { handleStatus } = changeAuthStatus()

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
		coreService.getAlertInfo()
			.then(response => {
				console.log(response)
				setAlertStatus(response.data.switchOffAlerts)
				if (response.data.defaultSendType) {
					setCurrentPlatform({ name: 'email', id: 1 })
				} else {
					setCurrentPlatform({ name: 'telegram', id: 2 })
					setTelegramId(response.data.telegram == 'null' ? '' : response.data.telegram)
				}
			})
			.catch(e => {
				if (e.code == 302) {
					handleStatus(false)
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
				<DropDownList open={false} label={'Тип уведомлений'} defaultValue={currentPlatform.id} elems={platforms} onClick={handleCurrentPlatform} defaultLabe='Тип уведомлений' />
				{currentPlatform.name == 'telegram' ?
					<div>
						<TextField
							id="standard-basic"
							label="Введите адрес"
							value={telegramId}
							variant="standard"
							placeholder='Введите айди' onChange={(e) => handleValue(e.target.value)} />
					</div>
					: <p>Используется почта при регистрации аккаунта</p>}
				<div className='notification_source__send-btn'>
					{currentPlatform.name == 'telegram' && <Button text={'Сохранить'} onClick={saveTgId} variant='contained' />}
					<Button text={'Отправить тестовое уведомление'} onClick={sendTestNotification} variant='contained' />

				</div>
			</div>

			<div className='alert_status'>
				<Switch
					checked={alertStatus}
					onChange={handleAlertsStatus}
					inputProps={{ 'aria-label': 'controlled' }}
					size="small"
				/>
				{alertStatus ? 'Включено' : 'Отключено'}
			</div>
			{currentPlatform.name == 'telegram' && <InstructionForTg />}
		</div>
	</div>
}


export default NotificationsPage