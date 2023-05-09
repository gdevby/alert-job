import React from 'react';

import Title from '../../components/common/title/Title';
import Examples from '../../layouts/homePage/examples/Examples';

import './homePage.scss';

const HomePage = () => {
	const list = ['Настройка по дням недели и времени, чтобы не уведомлять ночью и на выходных',
		'Фильтры по технологиям',
		'Фильтры по описанию',
		'Фильтры по названию']

	const sites = ['https://freelance.ru', 'https://freelance.habr.com', 'https://www.fl.ru', 'https://www.weblancer.net']

	return <div className='container'>
		<div className='homePage_content'>
			<Title text={'Агрегатор заказов с фриланс бирж'} />
			<h3>Цель проекта:
				<ul className='home-page_content_list'>
					<li>Экономия вашего времени с помощью автоматизации поиска заказов на фриланс бирж.</li>
					<li>Своевременно уведомлять о заказе, без необходимости постоянно мониторить заказы каждый день и тратить личное время.</li>
					<li>Получать уведомления в удобную систему для вас: почта, телеграм. Можем расширить список.</li>
				</ul>
			</h3>
			<p className='mt-1'>Если вы ищите заказы каждый день в ручном режиме, то попробуйте переложить поиск на наш сервис.
				Мы его разрабатывали для своих нужд и мы его используем каждый день. Доступны заказы для следующих бирж:
				{sites.map((item, index) => {
					return <span key={item}><a href={item}>{item}</a>
						{index === sites.length - 1 ? '' : ','}
					</span>
				})}
			, если вы хотите видеть заказы с других бирж сообщите нам.</p>
			<div className='home-page_content_advantage mt-1'>
				<p>Возможности системы:</p>
				<ul className='home-page_content_list'>
					{list.map(item => <li key={item}>{item}</li>)}
				</ul>
			</div>
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