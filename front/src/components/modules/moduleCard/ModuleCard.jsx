import React, { useState } from 'react'

import ListItem from '@mui/material/ListItem';
import ButtonGroup from '@mui/material/ButtonGroup';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Fab from '@mui/material/Fab';
import Switch from '@mui/material/Switch';

import './moduleCard.scss'

const ModuleCard = ({ item, removeCard, openModule, updateModule }) => {
	const [status, setStatus] = useState(item.available)
	
	const deleteModule = () => {
		removeCard(item)
	}
	
	const handleStatus = (e) => {
		updateModule(e.target.checked, item.id)
		setStatus(prev => !prev)
	}

	return <ListItem disablePadding className='modules__item-box'>
		<div className='modules__item' key={item.id}>
			<div className='modules__item__module-name'>{item.name}</div>
			<div className='modules__item_status'>
				<Switch
					checked={status}
					onChange={handleStatus}
					inputProps={{ 'aria-label': 'controlled' }}
					size="small"
				/>
				{status ? 'Включен' : 'Выключен'}
			</div>
			<ButtonGroup variant="outlined" aria-label="outlined button group" className='modules__item_button-group'>
				<Fab color="primary" aria-label="edit" size="small" onClick={() => openModule(item)}>
					<EditIcon />
				</Fab>
				<Fab color="error" aria-label="remove" size="small" onClick={() => deleteModule(item.id)}>
					<DeleteIcon />
				</Fab>
			</ButtonGroup>
		</div>
	</ListItem>
}

export default React.memo(ModuleCard)