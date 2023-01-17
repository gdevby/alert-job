import React from 'react'

const Word = ({ item, remove }) => {
	const removeItem = () => {
		remove(item.id)
	}

	return <div>
		<div>{item.name}</div>
		<div onClick={removeItem}>удалить</div>
	</div>
}

export default React.memo(Word)