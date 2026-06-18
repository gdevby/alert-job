import React from 'react';

import Switch from '@mui/material/Switch';

const AlertStatus = (props) => {
	
	const { alertStatus, handleAlertsStatus } = props
	
	return <div className='alert_status'>
		<Switch
			checked={alertStatus}
			onChange={handleAlertsStatus}
			inputProps={{ 'aria-label': 'controlled' }}
			size="small"
		/>
		{alertStatus ? 'Включено' : 'Отключено'}
	</div>
};

export default AlertStatus;