import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'

import SourcePanel from '../../../components/sources/sourcePanel/SourcePanel'
import SourceList from '../../../components/sources/sourcesList/SourcesList'
import CircularProgress from '@mui/material/CircularProgress';

import { sourceService } from '../../../services/parser/endponits/sourceService'


const Sources = () => {
	const [sourse, setSources] = useState([])
	const [isFetching, setIsFetching] = useState(true)


	const { id } = useParams()

	const addSource = data => {
		const newSource = {
			cat: {
				...data.currentCat,
				nativeLocName: data.currentCat.name
			},
			site: {
				...data.currentSite
			},
			sub_cat: {
				...data.currentSubCat,
				nativeLocName: data.currentSubCat.name
			},
			id: data.id
		}
		setSources([...sourse, newSource])
	}


	useEffect(() => {
		sourceService
			.getSources(id)
			.then(response => {
				const sources = response.data.map(item => { return { id: item.id, cat: item.siteCategoryDTO, site: item.siteSourceDTO, sub_cat: item.siteSubCategoryDTO } })
				setSources((prev) => [...prev, ...sources])
			})
			.finally(() => setIsFetching(false))
	}, [])

	return <>
		<SourcePanel addSource={addSource} module_id={id} />
		{isFetching ? <div style={{'textAlign': 'center'}}><CircularProgress /></div> : <SourceList setSources={setSources} sources={sourse} />}
	</>
}

export default Sources