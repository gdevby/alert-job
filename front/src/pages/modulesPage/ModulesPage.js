import React, { useEffect, useState } from 'react'
import Button from '../../components/button/Button'
import { moduleService } from '../../services/parser/endponits/moduleService'
import { useNavigate } from "react-router-dom";

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

	return <div className='modulesPage'>
		<div className='container'>
			<div>
				<div>
					<label htmlFor='moduleName'></label>
					<input type='text' id='moduleName' value={moduleName} onChange={(event) => setModuleName(event.target.value)} />
				</div>
				<div><Button text={'Добавить модуль'} onClick={addModule} /></div>
			</div>
			<div>
				{modules.length && modules.map(item => <div key={item.id}>
						<div>{item.name}</div>
						<div onClick={() => openModule(item.id)}>Открыть модуль</div>
						<Button text={'Удалить'} onClick={() => deleteModule(item.id)}/>		
				</div>)}
			</div>
		</div>

	</div>
}

export default ModulesPage