import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'

import { sourceService } from '../../services/parser/endponits/sourceService'

import SourceCard from '../../components/sourceCard/SourceCard'

const FiltersPage = () => {

	const [sourse, setSources] = useState([])


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
	
	const deleteSource = id => {
		sourceService.deleteSource(id).then(() => {
			const newSources = sourse.filter(item => item.id != id)
			setSources(newSources)
		})
	}
	
	useEffect(() => {
		sourceService
		.getSources()
		.then(response => {
			const sources = response.data.map(item => {return {id: item.id, cat: item.siteCategoryDTO, site: item.siteSourceDTO, sub_cat: item.siteSubCategoryDTO}})
			setSources((prev) => [...prev, ...sources])
		})
	}, [])
	
	
	
	
	
	
	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <SourceCard removeCard={deleteSource} item={item} key={index}/>
					}
					)}
				</div>
			</div>
		</div>
	</div>
}

export default FiltersPage