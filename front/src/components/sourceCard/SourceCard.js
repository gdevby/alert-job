import React from 'react'

import Button from '../button/Button'

const SourceCard = ({item, removeCard}) => {
	
	const remove = () => {
		removeCard(item.id)
	}
	
	return <div className='source-card' >
							<h5>{item.site?.name || ''}</h5>
							<p>Категория: {item.cat?.nativeLocName || ''}</p>
							<p>Подкатегория: {item.sub_cat?.nativeLocName || ''}</p>
							<Button id={item.id} onClick={remove} text={'Удалить источник'} />
						</div>
}

export default React.memo(SourceCard)