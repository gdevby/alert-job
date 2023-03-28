import React, { useEffect, useState } from 'react';

import DropDownList from '../../../components/common/dropDownList/DropDowList';
import { SingleInputTimeRangeField } from '@mui/x-date-pickers-pro/SingleInputTimeRangeField';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import Btn from '../../../components/common/button/Button'
import Item from '../../../components/common/item/Item'

import { coreService } from '../../../services/parser/endponits/coreService';

const AlertTime = (props) => {
	
	const { shedule } = props;
	
	const weekList =
		[
			{ id: 1, name: 'Понедельник' },
			{ id: 2, name: 'Вторник' },
			{ id: 3, name: 'Среда' },
			{ id: 4, name: 'Четверг' },
			{ id: 5, name: 'Понедельник' },
			{ id: 6, name: 'Суббот' },
			{ id: 7, name: 'Воскресенье' }
		]
	const [value, setValue] = useState()
	const [addedAlertDays, setAddedAlertDays] = useState([])
	const [newDay, setNewDay] = useState({})

	const handleValue = (time) => {
		console.log(new Date(time))
		setValue(time)
	}

	const handleDay = (value) => {
		setNewDay(value)
	}

	const addAlertTime = () => {
		const data = {
			alertDate: newDay.id,
			startAlert: new Date(value[0]).getHours(),
			endAlert: new Date(value[1]).getHours()
		}
		coreService.addAlertTime(data)
	}
	
	useEffect(() => {
		shedule && setAddedAlertDays(shedule)
	}, [shedule])

	return <div className='mt-1'>
		<p>Выберите дни и время, в которое буду приходить уведомления</p>
		<div className='mt-1'>
			<DropDownList label={'Дни недели'} defaultLabe={'Дни недели'} elems={weekList} onClick={handleDay} />
		</div>
		<div className='mt-1'>
			<LocalizationProvider dateAdapter={AdapterMoment}>
				<SingleInputTimeRangeField
					label="Период времени"
					value={value}
					onChange={handleValue}
					format="HH"
					size='small'
				/>
			</LocalizationProvider>
		</div>
		<div>
			<Btn text={'Добавить'} onClick={addAlertTime} />
		</div>
		<div>
			{addedAlertDays.map(item => <Item>
				<div>{item.alertDate}</div>
				<div>{item.startAlert} - {item.endAlert}</div>
			</Item>)}
		</div>
	</div>
}

export default AlertTime;