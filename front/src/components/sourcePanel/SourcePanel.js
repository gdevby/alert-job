import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { parserService } from '../../services/parser/endponits/parserService'

import './sourcePanel.scss'

const SourcePanel = ({ addSource }) => {
	const [currentSite, setCurrentSite] = useState('')
	const [currentCat, setCurrentCat] = useState('')
	const [currentSubCat, setCurrentSubCat] = useState([])
	const [sites, setSites] = useState([])
	const [categories, setCategories] = useState([])
	const [subcategories, setSubcategories] = useState([])


	useEffect(() => {
		parserService
			.getSites()
			.then(response => {
				setSites(response.data)
			})
	}, [])

	useEffect(() => {
		if (currentSite.id) {
			parserService
				.getCategories(currentSite.id)
				.then(response => {
					let cat = response.data.map(item => item.category)
					setCategories(cat)
				})
		}

	}, [currentSite.id])

	useEffect(() => {
		if (currentCat.id) {
			parserService
				.getSubcategories(currentCat.id)
				.then(response => {
					let subcat = response.data.map(item => item.subCategory)
					setSubcategories(subcat)
				})
		}

	}, [currentCat.id])


	const addingSource = () => {
		addSource({ currentSite, currentCat, currentSubCat })
	}

	return <div className='source_panel'>
		<div>
			<DropDownList defaultValue={'Выберите сайт'} elems={sites} open={false} cb={setCurrentSite} />
		</div>
		<div className='cat'>
			<DropDownList defaultValue={'Категорию'} elems={categories} open={false} cb={setCurrentCat} />
		</div>
		<div className='subcat'>
			<DropDownList defaultValue={'Выберите подкатегорию'} elems={subcategories} open={false} cb={setCurrentSubCat} />
		</div>
		<div>
			<Button onClick={addingSource} text={'Добавить источник'} />
		</div>
		<div>
			<Link to='/page/adding-filter'>Добавить новый фильтр</Link>
		</div>
	</div>
}

export default SourcePanel