import React from 'react';

import sources from '../../../images/home/sources.jpg';
import filters from '../../../images/home/filters.jpg';
import negativeFilters from '../../../images/home/negative-filters.jpg';
import orders from '../../../images/home/orders.jpg';

import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

import './examples.scss'

const Examples = () => {
	const steps = [
		{
			img: sources,
			description: 'Пример возможных добавленных источников.'
		},
		{
			img: filters,
			description: 'Пример конфигурации фильтров.'
		},
		{
			img: negativeFilters,
			description: 'Пример конфигурации негативных фильтров.'
		},
		{
			img: orders,
			description: 'Пример возможных отфильтрованных заказов.'
		},
	]

	console.log(steps)

	return (
		<div className='examples'>
			<h4>Примеры:</h4>
			<div className='examples_items'>
				{steps.map(item => {
					return <Accordion className='examples_item' key={item.description}>
				        <AccordionSummary
				          expandIcon={<ExpandMoreIcon />}
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