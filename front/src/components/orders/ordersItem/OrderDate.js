import React from 'react'

import Moment from 'react-moment';

const OrderDate = ({date}) => {
	return <Moment format="hh:mm DD.MM.YYYY">
			{date}
		</Moment>
}

export default OrderDate