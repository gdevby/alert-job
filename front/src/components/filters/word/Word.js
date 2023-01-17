import React from 'react'
import Button from '../../button/Button'

const Word = ({ item, remove }) => {
	const removeItem = () => {
		remove(item.id)
	}

	return <div>
		<div>{item.name}</div>
		<Button text={'удалить'} onClick={removeItem}/> 
	</div>
}

export default React.memo(Word)