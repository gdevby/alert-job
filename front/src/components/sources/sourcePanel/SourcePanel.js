import React, { useState, useEffect } from 'react'

import DropDownList from '../../common/dropDownList/DropDowList'
import Btn from '../../common/button/Button'

import { parserService } from '../../../services/parser/endponits/parserService'

import './sourcePanel.scss'

const SourcePanel = ({ addSource, module_id }) => {
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
					let cat = response.data.map(item => ({ id: item.id, name: item.nativeLocName }))
					setCategories(cat)
				})
		}

	}, [currentSite.id])

	useEffect(() => {
		if (currentCat.id) {
			setCurrentSubCat([])
			parserService
				.getSubcategories(currentCat.id)
				.then(response => {
					let subcat = response.data.map(item => ({ id: item.id, name: item.nativeLocName }))
					setSubcategories([{ id: null, name: 'Все подкатегории' }, ...subcat])
				})
		}

	}, [currentCat.id])


	const addingSource = () => {
		if (Number(currentSite.id) && Number(currentCat.id)) {
			addSource({ currentSite, currentCat, currentSubCat })
		}
	}

	const handleCurrentSubCat = data => {

		if (data.id != 0) {
			setCurrentSubCat(data)
		} else {
			setCurrentSubCat({
				id: null,
				name: 'Все подкатегории'
			})
		}

	}

	return <div className='source_panel'>

		<div className='source_panel-addingSource'>
			<div className='source_panel-addingSource__title'>
				Сперва вам надо указать источник заказов, откуда вы будете получать заказы, чтобы потом применять фильтры,
				выберите для начала сайт, потом категорию и подкатегорию, например "Все подкатегории"</div>
			<div className='source_panel-addingSource__content'>
				<div className='site'>
					<DropDownList label={'Выберите сайт'} elems={sites} onClick={setCurrentSite} defaultLabe={'Выберите сайт'} />
				</div>
				<div className='cat'>
					<DropDownList label={'Выберите категорию'} elems={categories} onClick={setCurrentCat} defaultLabe={'Выберите категорию'} />

				</div>
				<div className='subcat'>
					<DropDownList defaultValue={0} label={'Выберите подкатегорию'} elems={subcategories} onClick={handleCurrentSubCat} defaultLabe={'Выберите подкатегорию'} />
				</div>
				<div>
					<Btn onClick={addingSource} text={'Добавить источник'} variant='contained' />
				</div>
			</div>
		</div>

	</div>
}

export default SourcePanel