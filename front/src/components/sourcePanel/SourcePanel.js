import React, { useState, useEffect } from 'react'

import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { parserService } from '../../services/parser/endponits/parserService'
import { sourceService } from '../../services/parser/endponits/sourceService'



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
					let cat = response.data.map(item => ({ id: item.id, name: item.nativeLocName }))
					setCategories(cat)
				})
		}

	}, [currentSite.id])

	useEffect(() => {
		if (currentCat.id) {
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
			sourceService.addSource({
				siteSource: Number(currentSite.id),
				siteCategory: Number(currentCat.id),
				siteSubCategory: currentSubCat.id,
				flRuForAll: false
			}
			).then(response => {
				addSource({ currentSite, currentCat, currentSubCat, id: response.data.id })
			})
		}
	}

	

	

	return <div className='source_panel'>

		<div className='source_panel-addingSource'>
			<div className='source_panel-addingSource__title'>
				Сперва вам надо указать источник заказов, откуда вы будете получать заказы, чтобы потом применять фильтры,
				 выберите для начала сайт, потом категорию и подкатегорию, например "Все подкатегории"</div>
			<div className='source_panel-addingSource__content'>
				<div>
					<DropDownList defaultValue={'Выберите сайт'} elems={sites} open={false} cb={setCurrentSite} />
				</div>
				<div className='cat'>
					<DropDownList defaultValue={'Выберите категорию'} elems={categories} open={false} cb={setCurrentCat} />
				</div>
				<div className='subcat'>
					<DropDownList defaultValue={'Выберите подкатегорию'} elems={subcategories} open={false} cb={setCurrentSubCat} />
				</div>
				<div>
				<Button onClick={addingSource} text={'Добавить источник'} />
			</div>
			</div>
		</div>
		
	</div>
}

export default SourcePanel