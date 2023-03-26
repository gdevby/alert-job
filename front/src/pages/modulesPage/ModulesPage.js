import React, { useEffect, useState } from 'react'
import { useNavigate } from "react-router-dom";

import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import ModuleCard from '../../components/modules/moduleCard/ModuleCard';
import Btn from '../../components/common/button/Button';
import List from '@mui/material/List';
import Item from '../../components/common/item/Item'
import Alert from '../../components/common/alert/Alert'
import Popup from '../../components/common/popup/Popup';

import { moduleService } from '../../services/parser/endponits/moduleService'
import { changeAuthStatus } from '../../hooks/changeAuthStatus';

import './modulesPage.scss'




const ModulesPage = () => {
	const [moduleName, setModuleName] = useState('')
	const [modules, setModules] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [isShowAlert, setIsShowAlert] = useState(false)
	const [isOpenPopup, setIsOpenPopup] = useState(false)

	const navigate = useNavigate();

	const { handleStatus } = changeAuthStatus()

	const addModule = () => {
		const isExist = modules.find(item => item.name == moduleName)
		if (isExist) {
			showAlert()
			return
		}
		moduleService
			.addModule(moduleName)
			.then(response => {
				console.log(response)
				setModules(prev => [...prev, response.data])
			})
			.catch(e => {
				if (e.response?.data?.message == `module with name ${moduleName} exists`) {
					showAlert()
				}
				if (e.response?.data?.message == 'the limit for added modules') {
					setIsOpenPopup(true)
				}
			})
	}

	useEffect(() => {
		moduleService
			.getModules()
			.then(response => {
				console.log(response)
				setModules(response.data)
			})
			.catch(e => {
				if (e.code == 302) {
					handleStatus(false)
				}
			})
			.finally(() => {
				setIsFetching(false)
			})
	}, [])

	const deleteModule = (id) => {
		moduleService
			.deleteModule(id)
			.then(() => {
				setModules(prev => prev.filter(item => item.id != id))
			})
	}
	const openModule = (id) => {
		navigate(`/page/filters/${id}`)
	}

	const changeModuleName = (e) => {
		setModuleName(e.target.value)
	}

	const showAlert = () => {
		setIsShowAlert(true)
		setTimeout(() => setIsShowAlert(false), 2000)
	}

	const handleClosePopup = () => {
		setIsOpenPopup(false)
	}

	return <div className='modules'>
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup} title={'Вы превысили лимит'}
			content={'Вы превысили максимальное количество модулей. Удалите, чтобы добавить новый.'} />
		<div className='container'>
			<p>Теперь вам надо создать модуль, который позволит вам выбрать несколько источников заказов и установить активный фильтр для этого модуля,
				который будет фильтровать ваши заказы.
				К примеру: можно использовать разные фильтры для заказов из разных сайтов.</p>
			<div className='modules__adding-form'>
				<TextField id="standard-basic" label="Введите название модуля" variant="standard" placeholder='Название модуля' onChange={changeModuleName} />
				<div className='modules__add-module-btn'>
					<Btn text={'Добавить модуль'} onClick={addModule} />
				</div>
			</div>
			<Alert open={isShowAlert} type={'warning'} content={'Модуль с таким именем уже существует.'} />
			{isFetching ? <CircularProgress /> : <List className='modules__items'>
				{modules.length > 0 && modules.map(item => <Item key={item.id}><ModuleCard item={item}
					removeCard={deleteModule}
					openModule={openModule}
				/></Item>)}
			</List>}
		</div>
	</div>
}

export default ModulesPage