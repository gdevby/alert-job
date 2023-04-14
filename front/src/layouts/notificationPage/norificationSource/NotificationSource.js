import React, { useEffect, useState } from 'react';

import Btn from '../../../components/common/button/Button';
import TextField from '@mui/material/TextField';
import DropDownList from '../../../components/common/dropDownList/DropDowList';

import { coreService } from '../../../services/parser/endponits/coreService';

const NotificationSource = (props) => {
	const { handleCurrentPlatform, currentPlatform, tgId, alertType } = props;

	const [telegramId, setTelegramId] = useState('')
	const [type, setType] = useState()
	const [disabled, setDisabled] = useState(false)


	const platforms = [{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }];

	useEffect(() => {
		if (tgId) {
			setTelegramId(tgId)
		}
		console.log(!tgId)
		if (currentPlatform.name == 'telegram' && !tgId) {
			setDisabled(true)
		}
	}, [tgId, currentPlatform])

	const saveTgId = () => {
		if (!type) {
			coreService.changeTgId(telegramId).then(console.log)
		}
	}
	

	useEffect(() => {
		setType(alertType)
	}, [alertType])

	const sendTestNotification = () => {
		coreService.sendTestMessage().then(console.log)
	}

	return <div className='notification_source'>
		<DropDownList open={false} label={'Тип уведомлений'} defaultValue={currentPlatform.id} elems={platforms} onClick={handleCurrentPlatform} defaultLabe='Тип уведомлений' />
		{currentPlatform.name == 'telegram' ?
			<div>
				<TextField
					id="standard-basic"
					label="Введите адрес"
					value={telegramId}
					variant="standard"
					placeholder='Введите айди' onChange={(e) => setTelegramId(e.target.value)} />
			</div>
			: <p>Используется почта при регистрации аккаунта</p>}
		<div className='notification_source__send-btn'>
			{currentPlatform.name == 'telegram' && <Btn text={'Сохранить'} onClick={saveTgId} variant='contained' disabled={currentPlatform.name == 'telegram' && disabled}/>}
			<Btn text={'Отправить тестовое уведомление'} onClick={sendTestNotification} variant='contained' disabled={currentPlatform.name == 'telegram' && disabled}/>
		</div>
	</div>
}

export default NotificationSource;