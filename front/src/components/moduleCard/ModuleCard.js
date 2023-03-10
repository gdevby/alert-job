import React from 'react'
import { AiOutlineClose } from "react-icons/ai";

import Button from '../button/Button';

import './moduleCard.scss'

const ModuleCard = ({ item, removeCard, openModule }) => {



	const deleteModule = () => {
		removeCard(item.id)
	}

	return <div className='modules__item' key={item.id}>
		<div>{item.name}</div>
		<Button className='modules__item_open-module' onClick={() => openModule(item.id)} text={'Открыть модуль'} />
		<div className='modules__item_remove' onClick={() => deleteModule(item.id)}>
			<AiOutlineClose />
		</div>
	</div>
}

export default React.memo(ModuleCard)