import React from 'react';

import Title from '../../components/common/title/Title';
import Examples from '../../layouts/homePage/examples/Examples';

import './homePage.scss';

const HomePage = () => {
	const list = ['Настройка по дням недели и времени, чтобы не уведомлять ночью и на выходных',
		'Фильтры по технологиям',
		'Фильтры по описанию',
		'Фильтры по названию',
		'Можно добавить критерий срочных дедлайнов, к примеру, не уведомлять, если дедлайн через один день',
		'Можно настраивать фильтры для некоторых источников(Максимальная гибкость.)']

	const sites = ['https://freelance.ru', 'https://freelance.habr.com', 'https://www.fl.ru', 'https://www.weblancer.net']

	return <div className='container'>
		<div className='homePage_content'>
			<Title text={'Агрегатор заказов с фриланс бирж'} />
			<h3>Основная цель: удобный, быстрый, избирательный способ получения уведомлений о нужных заказах по вашим настроенным фильтрам.
				Имеется два типа фильтров позитивные и негативные.
				Сначала применяются фильтры для выбора заказов, потом негативные отсеивают заказы, которые вам не подходят.
				К примеру "Я хочу получать заказы, которые содержат в названии backend и не хочу получать заказы, которые содержат в названии nodejs".</h3>
			<div className='home-page_content_advantage mt-1'>
				<p>Преимущества:</p>
				<ul className='home-page_content_list'>
					{list.map(item => <li key={item}>{item}</li>)}
				</ul>
			</div>
			<p className='mt-1'>На данный момент на сайте доступны следующие биржи:
				{sites.map((item, index) => {
					return <span  key={item}><a href={item}>{item}</a>
						{index === sites.length-1? '' : ','}
					</span>
				})}
			</p>
			<p className='mt-1'>Чтобы начать, создайте аккаунт и авторизуйтесь на этом сайте, в правом верхнем углу можете сконфигурировать фильтры.</p>
			<Examples />
			<p className='mt-1' style={{ fontWeight: 'bold' }}>
				Весь функционал работает, предложениям и замечаниям писать
				<a href='https://t.me/robertmakrytski'>https://t.me/robertmakrytski</a>
			</p>
		</div>
	</div>
}

export default HomePage