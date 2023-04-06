import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

import Field from '../../common/field/Field';

const Period = ({updatePeriod}) => {
	const [currentPeriod, setPeriod] = useState(1)
	
	const { id } = useParams()

	const handlerPeriod = (period) => {
		setPeriod(period)
	}

	const validatePeriod = period => {
		if (period < 1) {
			setPeriod(1)
			changePeriod(1)
			return
		}
		if (period > 30) {
			setPeriod(30)
			changePeriod(30)
			return 
		}
		changePeriod(period)
	}
	
	const changePeriod = (per) => {
		updatePeriod(per)
		window.localStorage.setItem(`period_${id}`, per)
	}
	
	useEffect(() => {
		const per = window.localStorage.getItem(`period_${id}`)
		if (per) {
			setPeriod(per)
			updatePeriod(per)
		}
	}, [id])


	return (
		<div className='orders__period'>
			<p>Выберите период, за который вам будут приходить заказы. От 1 дня до 30.</p>
			<Field type={'number'} defaultValue={currentPeriod} cb={handlerPeriod} onBlur={(e) => validatePeriod(+e.target.value)} />
		</div>
	)
}

export default Period;