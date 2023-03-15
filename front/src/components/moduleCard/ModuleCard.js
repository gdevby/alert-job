import React from 'react'
import { AiOutlineClose } from "react-icons/ai";

import Btn from '../button/Button';
import ListItem from '@mui/material/ListItem';
import ButtonGroup from '@mui/material/ButtonGroup';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Fab from '@mui/material/Fab';

import './moduleCard.scss'

const ModuleCard = ({ item, removeCard, openModule }) => {



	const deleteModule = () => {
		removeCard(item.id)
	}
	/*<div className='modules__item_remove' onClick={() => deleteModule(item.id)}>
				<AiOutlineClose />
			</div>
			<Btn className='modules__item_open-module' onClick={() => openModule(item.id)} text={'Открыть модуль'} />
				<Btn className='modules__item_open-module' color={'error'} onClick={() => deleteModule(item.id)} text={'Удалить'} />
			
			*/



	return <ListItem disablePadding className='modules__item-box'>
		<div className='modules__item' key={item.id}>
			<div className='modules__item__module-name'>{item.name}</div>
			<ButtonGroup variant="outlined" aria-label="outlined button group" className='modules__item_button-group'>
				<Fab color="primary" aria-label="edit" size="small" onClick={() => openModule(item.id)}>
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