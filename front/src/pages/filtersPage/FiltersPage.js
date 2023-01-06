import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'
import Button from '../../components/button/Button'

import { sourceService } from '../../services/parser/endponits/sourceService'

const FiltersPage = () => {

	const [sourse, setSources] = useState([])


	const addSource = data => {
		setSources([...sourse, data])
	}
	
	const deleteSource = event => {
		const id = event.target.id
		sourceService.deleteSource(id).then(() => {
			const newSources = sourse.filter(item => item.id != id)
			setSources(newSources)
		})
	}
	
	useEffect(() => {
		sourceService
		.getSources()
		.then(response => {
			setSources(response.data)
		})
	}, [])
	
	
	
	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <div className='source-card' key={index}>
							<h5>{item.currentSite.name}</h5>
							<p>{item.currentCat.name}, {item.currentSubCat.name}</p>
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