import React from 'react';

import sources from '../../../images/home/sources.jpg';
import filters from '../../../images/home/filters.jpg';
import source from '../../../images/home/source.jpg';
import orders from '../../../images/home/orders.jpg';

import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';

import './examples.scss'

const Examples = () => {
	const steps = [
		{
			img: sources,
			description: 'Функционал добавления источников заказов для вида деятельности'
		},
		{
			img: source,
			description: 'Конфигурация источников, откуда мы получаем заказы'
		},
		{
			img: filters,
			description: 'Конфигурация фильтров, по которым мы будем получать заказы'
		},
		{
			img: orders,
			description: 'Возможные отфильтрованные заказы'
		},
	]

	console.log(steps)

	return (
		<div className='examples'>
			<h4>Примеры:</h4>
			<div className='examples_items'>
				{steps.map(item => {
					return <Accordion expanded={true} className='examples_item' key={item.description}>
				        <AccordionSummary
				          aria-controls="panel1a-content"
				          id="panel1a-header"
				        >
				          <Typography>{item.description}</Typography>
				        </AccordionSummary>
				        <AccordionDetails>
				          <div className='examples_item_image'><img src={item.img} alt=''/></div>
				        </AccordionDetails>
				      </Accordion> 
				})}
			</div>
		</div>
	)
}

export default Examples;