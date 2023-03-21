import React from 'react'

import ClearIcon from '@mui/icons-material/Clear';

const Word = ({ item, remove }) => {
	const removeItem = () => {
		remove(item.id)
	}

	return <div className='word'>
		<div className='word__name'>{item.name}</div>
		<div className='word__remove' onClick={removeItem} ><ClearIcon /></div>
	</div>
}

export default React.memo(Word)