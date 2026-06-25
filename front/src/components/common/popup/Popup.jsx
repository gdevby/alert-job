import React from 'react'

import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Button from '../button/Button'

const Popup = ({ open, handleClose, title, content, actions }) => {
	return <Dialog
		open={open}
		onClose={handleClose}
		aria-labelledby="alert-dialog-title"
		aria-describedby="alert-dialog-description"
	>
		<DialogTitle id="alert-dialog-title">
			{title}
		</DialogTitle>
		<DialogContent>
			<DialogContentText id="alert-dialog-description">
				{content}
			</DialogContentText>
		</DialogContent>
		<DialogActions>
		{actions}
			
		</DialogActions>
	</Dialog>
}

export default Popup