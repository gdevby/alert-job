import React, { useEffect, useState } from 'react'
import { moduleService } from '../../services/parser/endponits/moduleService'
import { useNavigate } from "react-router-dom";

import TextField from '@mui/material/TextField';

import ModuleCard from '../../components/moduleCard/ModuleCard';
import Field from '../../components/field/Field';
import Btn from '../../components/button/Button';
import List from '@mui/material/List';

import './modulesPage.scss'

const ModulesPage = () => {
	const [moduleName, setModuleName] = useState('')
	const [modules, setModules] = useState([])

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

	/*<Field 
					label={'Введите название модуля'}
					placeholder={'Название модуля'}
					defaultValue={moduleName}
					cb={setModuleName}
					
				/>*/

	return <div className='modules'>
		<div className='container'>
			<div className='modules__adding-form'>
				<TextField id="standard-basic" label="Введите название модуля" variant="standard" placeholder='Название модуля' onChange={changeModuleName} />


				<div className='modules__add-module-btn'>
					<Btn text={'Добавить модуль'} onClick={addModule} />

				</div>
			</div>
			<List className='modules__items'>
					{modules.length > 0 && modules.map(item => <ModuleCard key={item.id} item={item}
						removeCard={deleteModule}
						openModule={openModule}
					/>)}
			</List>
		</div>

	</div>
}

export default ModulesPage