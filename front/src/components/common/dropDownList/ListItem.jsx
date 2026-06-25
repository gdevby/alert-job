import React from 'react'


const ListItem = ({ item, onClick }) => {
	
	const choseItem = () => {
		onClick(item)
	}
	
	
	return <div id={item.id} onClick={choseItem} className='list_item'>{item.name || ''}</div>
}

export default ListItem