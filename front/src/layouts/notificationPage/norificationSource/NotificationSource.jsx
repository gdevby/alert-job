import React, { useEffect, useState } from 'react';

import Btn from '../../../components/common/button/Button';
import TextField from '@mui/material/TextField';
import DropDownList from '../../../components/common/dropDownList/DropDowList';

import { coreService } from '../../../services/parser/endponits/coreService';

const NotificationSource = (props) => {
	const { handleCurrentPlatform, currentPlatform, tgId, alertType, email, updateTelegramId } = props;

	const [telegramId, setTelegramId] = useState('')
	const [type, setType] = useState()
	const [disabled, setDisabled] = useState(false)
	const [currentEmail, setEmail] = useState('')
	const [platform, setPlatform] = useState('')

	const platforms = [{ name: 'email', id: 1 }, { name: 'telegram', id: 2 }];

	useEffect(() => {
		if (tgId) {
			setTelegramId(tgId)
			setDisabled(false)
		}
		if (currentPlatform.name == 'telegram' && !tgId) {
			setDisabled(true)
		}
		setPlatform(currentPlatform)
	}, [tgId, currentPlatform])

	const saveTgId = () => {
		console.log(type)
		if (!type) {
			coreService.changeTgId(telegramId).then(console.log)
			updateTelegramId(telegramId)
		}
	}
	
	const handlePlatform = data => {
		handleCurrentPlatform(data, data.name === 'email')
		setType(data.name === 'email')
		setPlatform(data)
	}

	
	useEffect(() => {
		if (email) {
			setEmail(email)	
		}
	}, [email])
	

	useEffect(() => {
		setType(alertType)
	}, [alertType])

	const sendTestNotification = () => {
		coreService.sendTestMessage().then(console.log)
	}
	
	const handleDisable = (e) => {
		setDisabled(!e.target.value.length)
	}
	
	const changeTelegramId = e => {
		if (e.target.value.trim().length === 0) {
			return setDisabled(true)
		}
		setTelegramId(e.target.value)
		console.log(platform)
		handleCurrentPlatform(platform, true)
		saveTgId()
	}

	return <div className='notification_source'>
		<DropDownList open={false} label={'Тип уведомлений'} defaultValue={currentPlatform.id} elems={platforms} onClick={handlePlatform} defaultLabe='Тип уведомлений' />
		{currentPlatform.name == 'telegram' ?
			<div>
				<TextField
					id="standard-basic"
					label="Введите адрес"
					value={telegramId}
					onBlur={changeTelegramId}
					variant="standard"
					placeholder='Введите айди' 
					onChange={(e) => setTelegramId(e.target.value)} 
					/>
			</div>
			: <p>{currentEmail}</p>}
		<div className='notification_source__send-btn'>
			<Btn text={'Отправить тестовое уведомление'} onClick={sendTestNotification} variant='contained' disabled={currentPlatform.name == 'telegram' && disabled}/>
		</div>
	</div>
}

export default NotificationSource;