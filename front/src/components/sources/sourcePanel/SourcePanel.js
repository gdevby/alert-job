import React, { useState, useEffect, useRef } from 'react'
import Autocomplete from '@mui/material/Autocomplete'
import TextField from '@mui/material/TextField'

import DropDownList from '../../common/dropDownList/DropDowList'
import Btn from '../../common/button/Button'

import { parserService } from '../../../services/parser/endponits/parserService'

import './sourcePanel.scss'

const SourcePanel = ({ addSource, module_id }) => {
	const [currentSite, setCurrentSite] = useState('')
	const [currentCat, setCurrentCat] = useState('')
	const [currentSubCat, setCurrentSubCat] = useState({})
	const [sites, setSites] = useState([])
	const [categories, setCategories] = useState([])
	const [subcategories, setSubcategories] = useState([])
	const catRef = useRef(null)
	const subCatRef = useRef(null)

	useEffect(() => {
		parserService
			.getSites()
			.then(response => {
				setSites(response.data)
			})
	}, [])

	useEffect(() => {
		if (currentSite.id) {
			setCategories([])
			parserService
				.getCategories(currentSite.id)
				.then(response => {
					let cat = response.data.map(item => ({ id: item.id, name: item.nativeLocName }))
					setCategories(cat)
					setCurrentCat('')
					setSubcategories([])
					setCurrentSubCat('')
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
					if (currentSite.id === 4 || currentSite.id === 5 || currentSite.id === 8) {
						setSubcategories([...subcat])
						console.log('SUBCAT', subcat[0])
						setCurrentSubCat(subcat[0])
					}else {
						setSubcategories([{ id: null, name: 'Все подкатегории' }, ...subcat])
						setCurrentSubCat({ id: null, name: 'Все подкатегории' })
					}
					
				})
		}

	}, [currentCat.id])


	const addingSource = () => {
		if (Number(currentSite.id) && Number(currentCat.id)) {
			/*if ((currentSite.id == 4 || currentSite.id == 5) && currentSubCat.name === 'Все подкатегории') {
				subcategories.forEach(item => {
					if (item.id) {
						addSource({ currentSite, currentCat, currentSubCat: item })
					}
					
				})
			}else {*/
				addSource({ currentSite, currentCat, currentSubCat })
//			}
			
		}
	}

	const handleAutocompleteOpening = (ref) => {
		const inputElement = ref.current.querySelector('input');
		setTimeout(() => {
			inputElement.value = '';
		}, 0);
	}

	const handleCurrentCatChange = (_, values) => {
		const { id, name } = values
		setCurrentCat({ id, name })
	}

	const handleCurrentSubCatChange = (_, values) => {
		const { id, name } = values
		setCurrentSubCat({ id, name })
	}

	const categoryOptions = categories.map(({ id, name }) => ({ label: name, id, name }))
	const subCategoryOptions = subcategories.map(({ id, name }) => ({ label: name, id, name }))

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
					<Autocomplete
						value={currentCat?.name ?? null}
						options={categoryOptions}
						disabled={!categoryOptions.length}
						clearIcon={false}
						renderInput={(params) => <TextField {...params} ref={catRef} label="Выберите категорию" placeholder={currentCat?.name ?? null} />}
						isOptionEqualToValue={(option, value) => option.name === value}
						size='small'
						selectOnFocus={false}
						onChange={handleCurrentCatChange}
						onOpen={() => handleAutocompleteOpening(catRef)}
					/>
				</div>
				<div className='subcat'>
					<Autocomplete
						value={currentSubCat?.name ?? null}
						options={subCategoryOptions}
						disabled={!subCategoryOptions.length}
						clearIcon={false}
						renderInput={(params) => <TextField {...params} ref={subCatRef} label="Выберите подкатегорию" placeholder={currentSubCat?.name ?? null} />}
						isOptionEqualToValue={(option, value) => option.name === value}
						size='small'
						selectOnFocus={false}
						onChange={handleCurrentSubCatChange}
						onOpen={() => handleAutocompleteOpening(subCatRef)}
					/>
				</div>
				<div className='add-source'>
					<Btn onClick={addingSource} text={'Добавить источник'} variant='contained' />
				</div>
			</div>
		</div>

	</div>
}

export default SourcePanel