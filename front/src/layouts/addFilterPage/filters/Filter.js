import React from 'react'
import { useParams } from 'react-router-dom'

import TitleWords from '../../../components/filters/titleWords/TitleWords'
import DescriptionWords from '../../../components/filters/descriptionWords/DescriptionWords'
import TechnologyWords from '../../../components/filters/technologyWords/TechnologyWords'
import Btn from '../../../components/button/Button'

const Filters = ({type = ''}) => {
	
	const { module_id, filter_id } = useParams()
	
	
	return <>
			<div className='wordsContains_block'>
					<div>
						<TechnologyWords filter_id={filter_id} type={type}/>
					</div>
					<div>
						<TitleWords filter_id={filter_id} type={type}/>
					</div>
					<div>
						<DescriptionWords filter_id={filter_id} type={type}/>
					</div>
				</div>
		</>
}

export default Filters