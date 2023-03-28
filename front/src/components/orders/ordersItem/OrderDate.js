import React from 'react'

import Moment from 'react-moment';

const OrderDate = ({date}) => {
	return <Moment local locale="ru" format="HH:mm DD.MM.YYYY" interval={0}> 
			{date}
		</Moment>
}

export default OrderDate;