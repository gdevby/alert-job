import React from 'react'

import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/Delete';

const SourceCard = ({ item, removeCard }) => {

	const remove = () => {
		removeCard(item.id)
	}

	return <>
		<TableRow
			key={item.id}
			sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
		>
			<TableCell component="th" scope="row">
				{item.site?.name || ''}
			</TableCell>
			<TableCell align="right">{item.cat?.nativeLocName || ''}</TableCell>
			<TableCell align="right">{item.sub_cat.id ? item.sub_cat?.nativeLocName : 'Все подкатегории'}</TableCell>
			<TableCell align="right" id={item.id}>
				<DeleteIcon onClick={remove}  className='source-card__remove' /></TableCell>
		</TableRow>
	</>
}

export default React.memo(SourceCard)