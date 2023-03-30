import React, { useEffect, useState } from 'react';

import DropDownList from '../../../components/common/dropDownList/DropDowList';
import Btn from '../../../components/common/button/Button';
import Item from '../../../components/common/item/Item';
import Field from '../../../components/common/field/Field';

import { coreService } from '../../../services/parser/endponits/coreService';

import './alertTime.scss'

const AlertTime = (props) => {

	const { shedule } = props;

	const weekList =
		[
			{ id: 1, name: 'Понедельник' },
			{ id: 2, name: 'Вторник' },
			{ id: 3, name: 'Среда' },
			{ id: 4, name: 'Четверг' },
			{ id: 5, name: 'Пятница' },
			{ id: 6, name: 'Суббота' },
			{ id: 7, name: 'Воскресенье' }
		]

	const [addedAlertDays, setAddedAlertDays] = useState([])
	const [newDay, setNewDay] = useState({})
	const [startAlert, setStartAlert] = useState(0)
	const [endAlert, setEndAlert] = useState(23)

	const handleDay = (value) => {
		setNewDay(value)
		setStartAlert(0)
		setEndAlert(23)
	}
	console.log(Math.sign(new Date().getTimezoneOffset()))
	console.log(new Date())

	const addAlertTime = async () => {
		if (!newDay.id) return;
		const data = {
			alertDate: newDay.id,
			startAlert,
			endAlert,
			timeZone: getTimeZone(new Date().getTimezoneOffset() / 60)
		}
		try {
			const newAlert = await coreService.addAlertTime(data)
			setAddedAlertDays(prev => [...prev, newAlert.data])
		} catch (e) {
			console.log(e)
		}
	}
	
	const getTimeZone = (zone) => {
		if (Math.sign(zone) == 1) {
			return `-${zone}`
		}
		if (Math.sign(zone) == -1) {
			return `+${-zone}`
		}
	}

	const getDayById = (id) => {
		const day = weekList.find(item => item.id == id)
		return day.name
	}

	useEffect(() => {
		console.log(shedule)
		if (shedule) {
			setAddedAlertDays(shedule)
		}
	}, [shedule])

	const removeTime = async (e) => {
		const id = e.target.id
		try {
			await coreService.removeAlertTime(id)
			setAddedAlertDays(prev => prev.filter(item => item.id != id))
		} catch (e) {
			console.log(e)
		}
	}

	const handlerStartAlertValue = (value) => {
		if (startAlert >= 24) setStartAlert(23)
		if (startAlert < 0) setStartAlert(0)
		if (startAlert > endAlert) setStartAlert(endAlert)
	}

	const handlerEndAlertValue = (value) => {
		if (endAlert >= 24) setEndAlert(23)
		if (endAlert < 0) setEndAlert(0)
		if (endAlert < startAlert) setEndAlert(startAlert)
	}

	return <div className='mt-1'>
		<p>Выберите дни и время, в которое буду приходить уведомления</p>
		<div className='mt-1 alert-time_prop'>
			<DropDownList label={'Дни недели'} defaultLabe={'Дни недели'} elems={weekList} onClick={handleDay} />
			<div className='alert-time_inputs'>
				<span>с </span><Field type="number" cb={(numb) => setStartAlert(Number(numb))} defaultValue={startAlert} onBlur={handlerStartAlertValue} />
				<span>до </span><Field type="number" cb={(numb) => setEndAlert(Number(numb))} defaultValue={endAlert} onBlur={handlerEndAlertValue} />
			</div>
		</div>
		<div className='mt-1'>
			<Btn text={'Добавить'} onClick={addAlertTime} />
		</div>
		<div className='mt-1'>
			<p>Установленные периоды:</p>
			{addedAlertDays.length > 0 ?
				<div className='alert-time_added mt-1'>
					{addedAlertDays.map(item => <Item key={item.endAlert}>
						<div>{getDayById(item.alertDate)}</div>
						<div>{item.startAlert} - {item.endAlert}</div>
						<Btn text={'Удалить'} id={item.id} onClick={removeTime} />
					</Item>)}
				</div> : <p>Пока ничего не добавлено.</p>}
		</div>
	</div>
}

export default AlertTime;