import React from 'react'

const ListItem = ({ item, onClick }) => {

	const handleSelect = () => {
		onClick(item)
	}

	return <div className='searchPopup__body-list__item'
		id={item.id}
		onClick={handleSelect}>
		<div>{item.name}</div>
		<div>{item.counter}</div>
	</div>
}

export default ListItem;