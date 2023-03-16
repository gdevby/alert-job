import React from 'react'
import { AiOutlineClose } from "react-icons/ai";
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
			<TableCell align="right" id={item.id} onClick={remove} className='source-card__remove'>
				<DeleteIcon /></TableCell>
		</TableRow>
	</>

	/*return <div className='source-card' >
		<div>
			<h4>Сайт</h4>
			<p>{item.site?.name || ''}</p>
		</div>
		<div className='source-card__cat'>
			<h4>Категория</h4>
			<p>{item.cat?.nativeLocName || ''}</p>
		</div>
		<div>
			<h4>Подкатегория</h4>
			<p>{item.sub_cat.id? item.sub_cat?.nativeLocName: 'Все подкатегории'}</p>
		</div>
		<div id={item.id} onClick={remove} className='source-card__remove'>
			<AiOutlineClose/>
		</div>
	</div>*/
}

export default React.memo(SourceCard)