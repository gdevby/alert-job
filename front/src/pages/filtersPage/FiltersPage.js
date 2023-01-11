import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'
import Button from '../../components/button/Button'

import { sourceService } from '../../services/parser/endponits/sourceService'
import { parserService } from '../../services/parser/endponits/parserService'

const FiltersPage = () => {

	const [sourse, setSources] = useState([])


	const addSource = data => {
		console.log(data)
		setSources([...sourse, {cat: data.currentCat, site: data.currentSite, sub_cat: data.currentSubCat, id: data.id}])
	}
	
	const deleteSource = event => {
		const id = event.target.id
		sourceService.deleteSource(id).then(() => {
			const newSources = sourse.filter(item => item.id != id)
			setSources(newSources)
		})
	}
	
	const getSource = async (id, cat_id, subcat_id) => {
		let site = await parserService.getSiteNameById(id)
		let cat = await parserService.getCatNameById(id, cat_id)
		let sub_cat = await parserService.getSubCatNameById(cat_id, subcat_id)
		
		
		return {site: site.data, cat: cat.data.category, sub_cat: sub_cat.data.subCategory}

	}
	
	useEffect(() => {
		sourceService
		.getSources()
		.then(response => {
			const sources = response.data
			for (let i = 0; i < sources.length; i++ ) {
					getSource(sources[i].siteSource, sources[i].siteCategory, sources[i].siteSubCategory)
					.then(response => setSources((prev) => [...prev, {...response, id: sources[i].id}]))			
			}
		})
	}, [])
	
	
	
	
	
	
	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <div className='source-card' key={index}>
							<h5>{item.site?.name || ''}</h5>
							<p>{item.cat?.name || ''}, {item.sub_cat?.name || ''}</p>
							<Button id={item.id} onClick={deleteSource} text={'Удалить источник'} />
						</div>
					}
					)}
				</div>
			</div>
		</div>
	</div>
}

export default FiltersPage