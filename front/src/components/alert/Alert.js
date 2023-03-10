import React, { useState } from 'react'
import { AiOutlineClose } from "react-icons/ai";

const Alert = () => {
	return <div className='alert'>
		<div className='alert__content'>
			<div className='alert__body'></div>
			<div className='alert__action'><AiOutlineClose /></div>
		</div>
	</div>
}

export default Alert