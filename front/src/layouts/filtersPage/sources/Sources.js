import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'

import SourcePanel from '../../../components/sources/sourcePanel/SourcePanel'
import SourceList from '../../../components/sources/sourcesList/SourcesList'
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '../../../components/common/alert/Alert'
import LimitPopup from '../../../components/common/popup/LimitPopup';
import Btn from '../../../components/common/button/Button'

import { sourceService } from '../../../services/parser/endponits/sourceService'


const Sources = () => {
	const [sourse, setSources] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [alert, setAlert] = useState(false)
	const [isLimit, setIsLimit] = useState(false)


	const { id } = useParams()

	const addSource = data => {
		const { currentSite, currentCat, currentSubCat } = data
		sourceService
			.addSource(id, {
				siteSource: Number(currentSite.id),
				siteCategory: Number(currentCat.id),
				siteSubCategory: currentSubCat.id,
				flRuForAll: false
			}
			).then(response => {
				updateSources(data, response.data.id)
			})
			.catch(e => {
				if (e.response.data.message == 'source exists') {
					showAlert()
				}
				if (e.message == 'limit') {
					setIsLimit(true)
				}
			})

	}

	const showAlert = () => {
		setAlert(true)
		setTimeout(() => {
			setAlert(false)
		}, 2000)
	}

	const updateSources = (data, id) => {
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
			id: id
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
		<LimitPopup handleClose={() => setIsLimit(false)}
			open={isLimit} />
		<SourcePanel addSource={addSource} module_id={id} />
		<Alert open={alert} content={'Такой источник уже существует'} type={'warning'} />
		{isFetching ? <div style={{ 'textAlign': 'center' }}><CircularProgress /></div> : <SourceList setSources={setSources} sources={sourse} />}
	</>
}

export default Sources