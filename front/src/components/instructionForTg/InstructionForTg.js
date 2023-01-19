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
				<li>Открыть телеграм;</li>
				<li>в левом верхнем углу в поиске вставить это название бота <b>userinfobot</b>;
					
				</li>
				<img src={seacrh_image}/>
				<li>после чего, найти этот аккаунт и нажать;
					
				</li>
				<img src={seacrhed}/>
				<li>теперь нажмите кнопку start в открывшемся диалоге.
					<br/> В результате у вас появится данные о вашем аккаунте. Найдите поле айди, скопируйте его и вставьте в поле вверху.
				</li>
				<img src={start}/>
				<li>теперь надо добавиться к боту, который будет присылать вам уведомления: 
				<a href='https://t.me/alertJobGdevBybot' target='_blank'>https://t.me/alertJobGdevBybot</a>
				</li>
				<img src={alertBot} />
				<li>
					после этого нажмите кнопку <b>"Отправить тестовое уведомление"</b> справо вверху, чтобы было отправлено тестовое сообщение
				</li>
			</ol>
		</div>
		
	</div>
}

export default InstructionForTg