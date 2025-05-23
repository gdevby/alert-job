import React, { useState } from 'react'
import { useParams } from 'react-router-dom'

import TitleWords from '../../../components/filters/titleWords/TitleWords'
import DescriptionWords from '../../../components/filters/descriptionWords/DescriptionWords'
import LimitPopup from '../../../components/common/popup/LimitPopup'

const Filters = ({ type = '' }) => {
	const [isLimit, setIsLimit] = useState(false)

	const { filter_id } = useParams()

	return <>
		<LimitPopup open={isLimit} handleClose={() => setIsLimit(false)} />
		<div className='wordsContains_block'>
			<div>
				<TitleWords filter_id={filter_id} type={type} setIsLimit={setIsLimit} />
			</div>
			<div>
				<DescriptionWords filter_id={filter_id} type={type} setIsLimit={setIsLimit} />
			</div>
		</div>
	</>
}

export default Filters