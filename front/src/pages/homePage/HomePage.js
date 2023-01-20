import React from 'react'


import Title from '../../components/title/Title'

const HomePage = () => {
	return <div className='container'>
		<div className='homePage_content'>
			<Title text={'Система уведомлений о заказах'} />
			<h3>Основная цель: удобный, быстрый, избирательный способ получения уведомлений о нужных заказах по вашим настроенным фильтрам для команд и одиночных исполнителей.
				Для команд имеет смысл использовать дополнительные возможности фильтра(отрицания),
				 это позволяет пропускать заказ по вашим критериям(это позволяет получать заказы для команд, к примеру 
				 "Я хочу получать заказы, которые содержат в названии backend и не хочу получать заказы, которые содержат в названии nodejs")</h3>
			<p>Преимущества:</p>
			<ul>
				<li>Настройка по дням недели и времени</li>
				<li>Фильтры по технологиям</li>
				<li>Фильтры по описанию</li>
				<li>Остановка на время</li>
				<li>Отложенные получение на некоторое время</li>
				<li>Добавить критерий срочных дедлайнов, к примеру не уведомлять если дедлайн через один день</li>
			</ul>
			<br></br>
			Чтобы начать, создайте аккаунт и авторизуйтесь на этом сайте, в правом верхнем углу можете сконфигурировать фильтры.
			<h3>
				<b>
					Проект находится в запуске, сейчас проходим обкат системы,
					 по всем вопросам и предложениям, замечаниям писать
					 <a href='https://t.me/robertmakrytski'>https://t.me/robertmakrytski</a> 
				</b>
			</h3>
		</div>
	</div>
}

export default HomePage