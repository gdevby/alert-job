import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'

import SourcePanel from '../../../components/sources/sourcePanel/SourcePanel'
import SourceList from '../../../components/sources/sourcesList/SourcesList'
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '../../../components/common/alert/Alert'
import Popup from '../../../components/common/popup/Popup';

import { sourceService } from '../../../services/parser/endponits/sourceService'


const Sources = () => {
	const [sourse, setSources] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [alert, setAlert] = useState(false)
	const [isOpenPopup, setIsOpenPopup] = useState(false)


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
				if (e.response?.data?.message == 'the limit for added sources') {
					setIsOpenPopup(true)
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


	const handleClosePopup = () => {
		setIsOpenPopup(false)
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
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup} title={'Вы превысили лимит'}
			content={'Вы превысили максимальное количество источников. Удалите, чтобы добавить новый.'} />
		<SourcePanel addSource={addSource} module_id={id} />
		<Alert open={alert} content={'Такой источник уже существует'} type={'warning'} />
		{isFetching ? <div style={{ 'textAlign': 'center' }}><CircularProgress /></div> : <SourceList setSources={setSources} sources={sourse} />}
	</>
}

export default Sources