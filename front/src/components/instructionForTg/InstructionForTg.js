import React, { useState, useEffect } from 'react'

import seacrh_image from '../../images/instructionForTg/seacrh_image.jpg'
import seacrhed from '../../images/instructionForTg/seacrhed.jpg'
import start from '../../images/instructionForTg/start.jpg'
import alertBot from '../../images/instructionForTg/alert-bot.jpg'

import './instructionForTg.scss'

const InstructionForTg = () => {
	return <div className='instruction'>
		<h2>Чтобы получать уведомления в телеграм, вам надо</h2>
		<div className='instruction__list-block'>
			<ol className='instruction__list'>
				<li>Открыть телеграм ссылку на телеграм бота и добавить его контакт в свой телеграм  
				<a href='https://t.me/userinfobot' target='_blank'>https://t.me/userinfobot</a> или через поиск по никнейму userinfobot
				</li>
				<li>Теперь нажмите кнопку start в открывшемся диалоге.
					<br/> В результате у вас появится данные о вашем аккаунте. Найдите поле айди, скопируйте его и вставьте в поле вверху и нажмите сохранить.
				</li>
				<img src={start}/>
				<li>Теперь надо добавиться к боту и нажать кнопку старт, который будет присылать вам уведомления о заказах: 
				<a href='https://t.me/alertJobGdevBybot' target='_blank'>https://t.me/alertJobGdevBybot</a>
				</li>
				<img src={alertBot} />
				<li>
					После этого нажмите кнопку <b>"Отправить тестовое уведомление"</b> справо вверху, чтобы было отправлено тестовое уведомление
				</li>
			</ol>
		</div>
		
	</div>
}

export default InstructionForTg