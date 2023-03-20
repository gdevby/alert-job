import React from 'react'
import { AiOutlineClose } from "react-icons/ai";

const Word = ({ item, remove }) => {
	const removeItem = () => {
		remove(item.id)
	}

	return <div className='word'>
		<div className='word__name'>{item.name}</div>
		<div className='word__remove' onClick={removeItem} ><AiOutlineClose /></div>
	</div>
}

export default React.memo(Word)