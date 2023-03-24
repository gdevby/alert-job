import React from 'react'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import Btn from '../../common/button/Button'
import SourceCard from '../sourceCard/SourceCard'
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

import { sourceService } from '../../../services/parser/endponits/sourceService'

const SourceList = ({ sources, setSources }) => {
	const [items, setItems] = useState([])
	const [isShowingSources, setIsShowingSources] = useState(window.localStorage.getItem('isShowingSources') == null ? true : window.localStorage.getItem('isShowingSources') == 'true')
	const { id } = useParams()

	const deleteSource = source_id => {
		sourceService.deleteSource(id, source_id).then(() => {
			const newSources = items.filter(item => item.id != source_id)
			setSources(newSources)
		})
	}

	const handleshowingSources = () => {
		console.log(isShowingSources)
		setIsShowingSources(prev => !prev)
	}

	useEffect(() => {
		window.localStorage.setItem('isShowingSources', isShowingSources)
	}, [isShowingSources])

	useEffect(() => {
		setItems(sources)
	}, [sources])

	return <div className='show-sources'>
		<Btn  onClick={handleshowingSources} text={isShowingSources ? 'Скрыть источники' : 'Показать источники'} variant='contained' className='mt-1' />
		{(isShowingSources && items.length > 0) &&
			<TableContainer component={Paper} className='source-list'><Table sx={{ minWidth: 650 }} aria-label="simple table">
				<TableHead>
					<TableRow>
						<TableCell>Сайт</TableCell>
						<TableCell align="right">Категория</TableCell>
						<TableCell align="right">Подкатегория</TableCell>
					</TableRow>
				</TableHead>
				<TableBody>
					{items.map((item, index) => <SourceCard key={index} removeCard={deleteSource} item={item} />)}
				</TableBody>
			</Table>
			</TableContainer>}
	</div>
}

export default SourceList