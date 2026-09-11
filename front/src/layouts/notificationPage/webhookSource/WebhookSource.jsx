import React, { useEffect, useState } from 'react';

import TextField from '@mui/material/TextField';

import { coreService } from '../../../services/parser/endponits/coreService';

const WebhookSource = (props) => {
	const { webhookUrl, updateWebhookUrl } = props;

	const [url, setUrl] = useState('')
	const [error, setError] = useState('')

	useEffect(() => {
		setUrl(webhookUrl || '')
	}, [webhookUrl])

	const saveWebhookUrl = e => {
		const value = e.target.value.trim()

		if (value === (webhookUrl || '')) {
			return
		}

		coreService.changeWebhookUrl(value)
			.then(() => {
				setError('')
				updateWebhookUrl(value)
			})
			.catch(() => setError('Адрес не принят: нужен внешний http(s) адрес'))
	}

	return <div className='notification_source'>
		<TextField
			fullWidth
			label='Webhook (необязательно)'
			value={url}
			onChange={(e) => setUrl(e.target.value)}
			onBlur={saveWebhookUrl}
			variant='standard'
			placeholder='https://example.com/hooks/alert-job'
			error={Boolean(error)}
			helperText={error || 'Заказы будут дублироваться POST-запросом с JSON. Пусто — выключено.'}
		/>
	</div>
}

export default WebhookSource;
