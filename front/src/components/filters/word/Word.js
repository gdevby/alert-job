import React from 'react'

import ClearIcon from '@mui/icons-material/Clear';
import Item from '../../common/item/Item'

const Word = ({ item, remove }) => {
	const removeItem = () => {
		remove(item.id)
	}

	return <Item className='word'>
		<div className='word__name'>{item.name}</div>
		<div className='word__remove' onClick={removeItem} ><ClearIcon /></div>
	</Item>
}

export default React.memo(Word)