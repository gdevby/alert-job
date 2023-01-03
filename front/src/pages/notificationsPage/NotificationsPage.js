import React, { useState, useEffect } from 'react'

import Title from '../../components/title/Title'
import Field from '../../components/field/Field'
import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { coreService } from '../../services/parser/endponits/coreService'

import './notificationsPage.scss'

const NotificationsPage = () => {
	const [platforms, setPlatforms] = useState([{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }])
	const [currentPlatform, setCurrentPlatform] = useState({})


	const sendTestNotification = () => {
		coreService.sendTestMessage().then(console.log)

	}

	useEffect(() => {
		coreService.getStatue().then(console.log)
	}, [])

	return <div className='notification_page'>
		<div className='container'>
			<Title text='Настройка уведомлений' />
			<div className='notification_source'>
				<DropDownList open={false} defaultValue={'email'} elems={platforms} cb={setCurrentPlatform} />
				<Field type='text' placeholder='Введите адрес' />
				<div className='notification_source__send-btn'>
					<Button text={'Отправить тестовое уведомление'} onClick={sendTestNotification} />
				</div>
			</div>




		</div>
	</div>
}


export default NotificationsPage