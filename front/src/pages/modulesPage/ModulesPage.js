import React, { useEffect, useState } from 'react'
import { useNavigate } from "react-router-dom";

import { moduleService } from '../../services/parser/endponits/moduleService'

import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import ModuleCard from '../../components/moduleCard/ModuleCard';
import Btn from '../../components/button/Button';
import List from '@mui/material/List';
import Item from '../../components/item/Item'


import './modulesPage.scss'

const ModulesPage = () => {
	const [moduleName, setModuleName] = useState('')
	const [modules, setModules] = useState([])
	const [isFetching, setIsFetching] = useState(true)

	const navigate = useNavigate();

	const addModule = () => {
		moduleService
			.addModule(moduleName)
			.then(response => {
				setModules(prev => [...prev, response.data])
			})
	}

	useEffect(() => {
		moduleService
			.getModules()
			.then(response => setModules(response.data))
			.finally(() => setIsFetching(false))
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

	return <div className='modules'>
		<div className='container'>
			<div className='modules__adding-form'>
				<TextField id="standard-basic" label="Введите название модуля" variant="standard" placeholder='Название модуля' onChange={changeModuleName} />
				<div className='modules__add-module-btn'>
					<Btn text={'Добавить модуль'} onClick={addModule} />

				</div>
			</div>
			<p>Теперь вам надо создать модуль, который позволит вам выбрать несколько источников заказов и установить активный фильтр для этого модуля,
			 который будет фильтровать ваши заказы.
			  К примеру: можно использовать разные фильтры для заказов из разных сайтов.</p>
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