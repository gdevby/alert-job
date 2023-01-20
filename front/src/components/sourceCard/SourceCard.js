import React from 'react'
import { AiOutlineClose } from "react-icons/ai";

const SourceCard = ({ item, removeCard }) => {

	const remove = () => {
		removeCard(item.id)
	}

	return <div className='source-card' >
		<div>
			<h4>Сайт</h4>
			<p>{item.site?.name || ''}</p>
		</div>
		<div className='source-card__cat'>
			<h4>Категория</h4>
			<p>{item.cat?.nativeLocName || ''}</p>
		</div>
		<div>
			<h4>Подкатегория</h4>
			<p>{item.sub_cat.id? item.sub_cat?.nativeLocName: 'Все подкатегории'}</p>
		</div>
		<div id={item.id} onClick={remove} className='source-card__remove'>
			<AiOutlineClose/>
		</div>
	</div>
}

export default React.memo(SourceCard)