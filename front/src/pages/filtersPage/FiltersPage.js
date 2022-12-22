import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'

const FiltersPage = () => {

	const [sourse, setSources] = useState([])


	const addSource = data => {
		console.log(data)
		setSources([...sourse, data])
	}
	
	
	
	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <div className='source-card' key={index}>
							<h5>{item.currentSite.name}</h5>
							<p>{item.currentCat.name}, {item.currentSubCat.name}</p>
							<button>Удалить источник</button>
						</div>
					}
					)}
				</div>
			</div>
		</div>
	</div>
}

export default FiltersPage