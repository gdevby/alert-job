import React, { useState, useEffect } from 'react'
import Alert from '@mui/material/Alert';
import Collapse from '@mui/material/Collapse';

import './alert.scss'

const CasAlert = ({open, type, content }) => {
	const [isOpen, setIsOpen] = useState(false)
	
	useEffect(() => {
		setIsOpen(open)
	}, [open])
	
	return <Collapse in={isOpen} className='alert'>
				<Alert severity={type}>{content}</Alert>
			</Collapse>
}

export default CasAlert