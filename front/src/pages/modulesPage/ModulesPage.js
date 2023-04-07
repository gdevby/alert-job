import React, { useEffect, useMemo, useState } from 'react'
import { useNavigate } from "react-router-dom";
import { useDispatch } from 'react-redux';

import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import ModuleCard from '../../components/modules/moduleCard/ModuleCard';
import Btn from '../../components/common/button/Button';
import List from '@mui/material/List';
import Item from '../../components/common/item/Item'
import Alert from '../../components/common/alert/Alert'
import Popup from '../../components/common/popup/Popup';
import LimitPopup from '../../components/common/popup/LimitPopup';

import { moduleService } from '../../services/parser/endponits/moduleService'
import { changeAuthStatus } from '../../hooks/changeAuthStatus';
import { removeCurrentFilter } from '../../store/slices/filterSlice';

import './modulesPage.scss'

const ModulesPage = () => {
	const [moduleName, setModuleName] = useState('')
	const [modules, setModules] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [isShowAlert, setIsShowAlert] = useState(false)
	const [isOpenPopup, setIsOpenPopup] = useState(false)
	const [isLimit, setIsLimit] = useState(false)
	const [popup, setPopup] = useState({})

	const navigate = useNavigate();
	
	const dispatch = useDispatch()

	const { handleStatus } = changeAuthStatus()

	const addModule = () => {
		const isExist = modules.find(item => item.name == moduleName)
		if (isExist) {
			showAlert()
			return
		}
		if (!moduleName.trim()) return
		moduleService
			.addModule(moduleName)
			.then(response => {
				setModules(prev => [...prev, response.data])
			})
			.catch(e => {
				if (e.response?.data?.message == `module with name ${moduleName} exists`) {
					showAlert()
				}
				console.log(e.message)
				if (e.message == 'limit') {
					setIsLimit(true)
					/*setIsOpenPopup(true)
					setPopup({
						title: 'Вы превысили лимит',
						content: 'Вы превысили максимальное количество. Удалите, чтобы добавить новый.',
						actions: <Btn onClick={handleClosePopup} text={'Закрыть'} />
					})*/
				}
			})
	}

	useEffect(() => {
		moduleService
			.getModules()
			.then(response => {
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
				window.localStorage.removeItem(`period_${id}`)
			})
			.finally(() => setIsOpenPopup(false))
	}
	const openModule = (item) => {
		dispatch(
			removeCurrentFilter()
		)
		navigate(`/page/filters/${item.id}`)
	}

	const changeModuleName = (e) => {
		setModuleName(e.target.value)
	}

	const confirmRemovsModule = (item) => {
		setPopup({
			title: 'Подвердите удаление',
			content: `Вы действительно хотите удалить модуль с именем ${item.name}?`,
			actions: <>
				<Btn onClick={handleClosePopup} text={'Закрыть'} />
				<Btn onClick={() => deleteModule(item.id)} text={'Удалить'} />
			</>
		})
		setIsOpenPopup(true)
	}

	const showAlert = () => {
		setIsShowAlert(true)
		setTimeout(() => setIsShowAlert(false), 2000)
	}

	const handleClosePopup = () => {
		setIsOpenPopup(false)
	}
	
	const updateModule = (status, id) => {
		moduleService.updateModule(null, status, id)
	}

	const showModules = useMemo(
		() => modules.map(item => <Item key={item.id}><ModuleCard item={item}
			removeCard={confirmRemovsModule}
			openModule={openModule}
			updateModule={updateModule}
		/></Item>), [modules]
	)

	return <div className='modules'>
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup}
			title={popup.title}
			content={popup.content}
			actions={popup.actions}
		/>
		<LimitPopup handleClose={() => setIsLimit(false)}
			open={isLimit} />
		<div className='container'>
			<p>Теперь вам надо создать модуль, который позволит вам выбрать несколько источников заказов и установить активный фильтр для этого модуля,
				который будет фильтровать ваши заказы.
				К примеру: можно создать модули для разных сайтов, откуда берутся заказы.</p>
			<div className='modules__adding-form'>
				<TextField id="standard-basic" label="Введите название модуля" variant="standard" placeholder='Название модуля' onChange={changeModuleName} />
				<div className='modules__add-module-btn'>
					<Btn text={'Добавить модуль'} onClick={addModule} />
				</div>
			</div>
			<Alert open={isShowAlert} type={'warning'} content={'Модуль с таким именем уже существует.'} />
			{isFetching ? <CircularProgress /> : <List className='modules__items'>
				{modules.length > 0 && showModules}
			</List>}
		</div>
	</div>
}

export default ModulesPage