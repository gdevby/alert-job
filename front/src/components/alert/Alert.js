import React, { useState } from 'react'
import { AiOutlineClose } from "react-icons/ai";

const Alert = () => {
	return <div className='alert'>
		<div className='alert-content'>
			<div className='alert-content__body'></div>
			<div className='alert-content__action'><AiOutlineClose/></div>
		</div>
	</div>
}

export default Alert