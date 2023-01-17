import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { parserService } from '../../services/parser/endponits/parserService'
import { sourceService } from '../../services/parser/endponits/sourceService'
import { filterService } from '../../services/parser/endponits/filterService'

import './sourcePanel.scss'

const SourcePanel = ({ addSource }) => {
	const [currentSite, setCurrentSite] = useState('')
	const [currentCat, setCurrentCat] = useState('')
	const [currentSubCat, setCurrentSubCat] = useState([])
	const [sites, setSites] = useState([])
	const [categories, setCategories] = useState([])
	const [subcategories, setSubcategories] = useState([])
	const [currentFilters, setCurrentFilters] = useState([])
	const [filter, setFilter] = useState('')


	useEffect(() => {
		parserService
			.getSites()
			.then(response => {
				setSites(response.data)
			})
	}, [])
	
	useEffect(() => {
		filterService
		.getFilters()
		.then(response => {
			setCurrentFilters(response.data)
		})
		
	}, [])
	

	useEffect(() => {
		if (currentSite.id) {
			parserService
				.getCategories(currentSite.id)
				.then(response => {
					let cat = response.data.map(item => ({id: item.id, name: item.nativeLocName}))
					setCategories(cat)
				})
		}

	}, [currentSite.id])

	useEffect(() => {
		if (currentCat.id) {
			parserService
				.getSubcategories(currentCat.id)
				.then(response => {
					let subcat = response.data.map(item => ({id: item.id, name: item.nativeLocName}))
					setSubcategories(subcat)
				})
		}

	}, [currentCat.id])


	const addingSource = () => {
		sourceService.addSource({
			siteSource: Number(currentSite.id),
		 	siteCategory: Number(currentCat.id),
		  	siteSubCategory: Number(currentSubCat.id),
		  	flRuForAll: false}
		  ).then(response => {
			addSource({ currentSite, currentCat, currentSubCat, id: response.data.id })
		})
		
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
		<div className='current_filter'>
			<DropDownList defaultValue={'Текущий фильтр'} elems={currentFilters} open={false} cb={setFilter} />
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