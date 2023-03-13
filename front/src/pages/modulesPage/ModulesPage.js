import React, { useEffect, useState } from 'react'
import { moduleService } from '../../services/parser/endponits/moduleService'
import { useNavigate } from "react-router-dom";

import TextField from '@mui/material/TextField';

import ModuleCard from '../../components/moduleCard/ModuleCard';
import Btn from '../../components/button/Button';
import List from '@mui/material/List';
import { styled } from '@mui/material/styles';
import Paper from '@mui/material/Paper';


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

	const Item = styled(Paper)(({ theme }) => ({
		backgroundColor: theme.palette.mode === 'dark' ? '#1A2027' : '#fff',
		...theme.typography.body2,
		padding: theme.spacing(0.5),
		textAlign: 'center',
		color: theme.palette.text.secondary,
	}));

	return <div className='modules'>
		<div className='container'>
			<div className='modules__adding-form'>
				<TextField id="standard-basic" label="Введите название модуля" variant="standard" placeholder='Название модуля' onChange={changeModuleName} />


				<div className='modules__add-module-btn'>
					<Btn text={'Добавить модуль'} onClick={addModule} />

				</div>
			</div>
			<List className='modules__items'>
				{modules.length > 0 && modules.map(item => <Item key={item.id}><ModuleCard item={item}
					removeCard={deleteModule}
					openModule={openModule}
				/></Item>)}
			</List>
		</div>

	</div>
}

export default ModulesPage