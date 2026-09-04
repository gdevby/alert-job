import React, { useState, useEffect } from 'react'

import Title from '../../components/common/title/Title'
import InstructionForTg from '../../components/notification/instructionForTg/InstructionForTg'
import NotificationSource from '../../layouts/notificationPage/norificationSource/NotificationSource';
import WebhookSource from '../../layouts/notificationPage/webhookSource/WebhookSource';
import AlertStatus from '../../layouts/notificationPage/alertStatus/AlertStatus';
import AlertTime from '../../layouts/notificationPage/alertTime/AlertTime';
import Btn from '../../components/common/button/Button';

import { coreService } from '../../services/parser/endponits/coreService'
import { changeAuthStatus } from '../../hooks/changeAuthStatus';

import './notificationsPage.scss'


const NotificationsPage = () => {
	const [currentPlatform, setCurrentPlatform] = useState('')
	const [alertStatus, setAlertStatus] = useState(false)
	const [telegramId, setTelegramId] = useState('')
	const [shedule, setShedule] = useState([])
	const [isOpenTime, setIsOpenTime] = useState(false)
	const [alertType, setAlertType] = useState()
	const [email, setEmail] = useState('')
	const [webhookUrl, setWebhookUrl] = useState('')

	const { handleStatus } = changeAuthStatus()


	const handleCurrentPlatform = (data, send) => {
		setCurrentPlatform(data)
		console.log(telegramId)
		if (send || telegramId) {
			coreService.changeAlertsType(data.name == 'email')
		}
	}

	useEffect(() => {
		coreService.getAlertInfo()
			.then(response => {
				setAlertStatus(response.data.switchOffAlerts)
				setShedule(response.data.alertTimeDTO)
				setAlertType(response.data.defaultSendType)
				setTelegramId(response.data.telegram === null ? '' : response.data.telegram)
				setEmail(response.data.email)
				setWebhookUrl(response.data.webhookUrl === null ? '' : response.data.webhookUrl)
				if (response.data.defaultSendType) {
					return setCurrentPlatform({ name: 'email', id: 1 })
				}
				setCurrentPlatform({ name: 'telegram', id: 2 })
			})
			.catch(e => {
				if (e.code == 302) {
					handleStatus(false)
				}
			})
	}, [])

	const handleAlertsStatus = () => {
		coreService.changeAlertsStatus(!alertStatus).then(() => setAlertStatus(!alertStatus))
	}

	const handleShowsAlertTime = () => {
		setIsOpenTime(prev => !prev)
	}
	
	const updateTelegramId = (newId) => {
		setTelegramId(newId)
	}

	const updateWebhookUrl = (newUrl) => {
		setWebhookUrl(newUrl)
	}

	return <div className='notification_page'>
		<div className='container'>
			<Title text='Настройка уведомлений' />
			<NotificationSource
				handleCurrentPlatform={handleCurrentPlatform}
				currentPlatform={currentPlatform}
				tgId={telegramId}
				alertType={alertType}
				email={email}
				updateTelegramId={updateTelegramId}
			/>
			<WebhookSource webhookUrl={webhookUrl} updateWebhookUrl={updateWebhookUrl} />
			<AlertStatus alertStatus={alertStatus} handleAlertsStatus={handleAlertsStatus} />
			<Btn className='mt-1' text={isOpenTime ? 'Скрыть настройку время оповещения' : 'Показать настройки время оповещения'} onClick={handleShowsAlertTime} />
			{isOpenTime && <AlertTime shedule={shedule} />}
			{currentPlatform.name == 'telegram' && <InstructionForTg />}
		</div>
	</div>
}


export default NotificationsPage