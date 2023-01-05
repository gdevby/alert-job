import React, { useState, useEffect } from 'react'

import seacrh_image from '../../images/instructionForTg/seacrh_image.jpg'
import seacrhed from '../../images/instructionForTg/seacrhed.jpg'
import start from '../../images/instructionForTg/start.jpg'

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
					
				</li>
				<img src={start}/>
			</ol>
		</div>
		В результате у вас появится данные о вашем аккаунте. Найдите поле айди, скопируйте его и вставьте в наш сайт.
	</div>
}

export default InstructionForTg