import React from 'react'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import Btn from '../../button/Button'
import Item from '../../item/Item'
import SourceCard from '../../sourceCard/SourceCard'

import { sourceService } from '../../../services/parser/endponits/sourceService'

const SourceList = ({ sources, setSources }) => {
	const [items, setItems] = useState([])
	const [isShowingSources, setIsShowingSources] = useState(window.localStorage.getItem('isShowingSources') == 'true')

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

	return <>
		<Btn onClick={handleshowingSources} text={isShowingSources ? 'Скрыть источники' : 'Показать источники'} variant='contained' />
		<div className='sourceList'>
			{isShowingSources && <Items items={items} deleteSource={deleteSource} />}
		</div>
	</>
}

const Items = ({ items, deleteSource }) => {

	return <>{
		items.length > 0 && items.map((item, index) => {
			return <Item key={index}><SourceCard removeCard={deleteSource} item={item} /></Item>
		}
		)
	}</>
}

export default SourceList