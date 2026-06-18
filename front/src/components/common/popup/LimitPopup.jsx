import React from 'react';

import Popup from './Popup';
import Btn from '../button/Button';

const LimitPopup = ({ handleClose, open }) => {
	return <Popup
			handleClose={handleClose}
			open={open}
			title={'Вы превысили лимит'}
			content={'Вы превысили максимальное количество. Удалите, чтобы добавить новое.'}
			actions={<Btn onClick={handleClose} text={'Закрыть'} />}
		/>
}

export default LimitPopup