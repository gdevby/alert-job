import React from 'react'

const ListItem = ({ item, onClick, isAdded }) => {

	const handleSelect = () => {
		if (!isAdded) {
			onClick(item)	
		}
	}
	
	console.log(item)
	
	return <div className={isAdded? 'searchPopup__body-list__item added': 'searchPopup__body-list__item'}
		id={item.id}
		onClick={handleSelect}>
		<div>{item.name}</div>
		<div>{item.counter}</div>
	</div>
}

export default React.memo(ListItem);