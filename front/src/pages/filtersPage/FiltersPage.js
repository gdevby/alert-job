import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'
import DropDownList from '../../components/dropDownList/DropDowList'
import Button from '../../components/button/Button'

import { sourceService } from '../../services/parser/endponits/sourceService'

import SourceCard from '../../components/sourceCard/SourceCard'

import { filterService } from '../../services/parser/endponits/filterService'

import { removeCurrentFilter } from '../../store/slices/filterSlice'

import { setCurrentFilter, setIsNew } from '../../store/slices/filterSlice'
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useParams } from 'react-router-dom'
import { moduleService } from '../../services/parser/endponits/moduleService'

const FiltersPage = () => {

	const [sourse, setSources] = useState([])
	const [currentFilters, setCurrentFilters] = useState([])
	const [filter, setFilter] = useState('')
	
	const { id } = useParams()

	const dispatch = useDispatch()
	const navigate = useNavigate()

	const { currentFilter, isChoose } = useSelector(state => state.filter)

	const addSource = data => {
		const newSource = {
			cat: {
				...data.currentCat,
				nativeLocName: data.currentCat.name
			},
			site: {
				...data.currentSite
			},
			sub_cat: {
				...data.currentSubCat,
				nativeLocName: data.currentSubCat.name
			},
			id: data.id
		}
		setSources([...sourse, newSource])
	}

	const deleteSource = id => {
		sourceService.deleteSource(id).then(() => {
			const newSources = sourse.filter(item => item.id != id)
			setSources(newSources)
		})
	}

	useEffect(() => {
		if (isChoose) {
			setFilter(currentFilter)
		}
	}, [isChoose])

	useEffect(() => {
		sourceService
			.getSources()
			.then(response => {
				const sources = response.data.map(item => { return { id: item.id, cat: item.siteCategoryDTO, site: item.siteSourceDTO, sub_cat: item.siteSubCategoryDTO } })
				setSources((prev) => [...prev, ...sources])
			})
	}, [])


	const addNewFilter = () => {
		dispatch(
			setIsNew({
				isNew: true
			})
		)
		dispatch(removeCurrentFilter())
		navigate('/page/adding-filter')
	}

	const editFilter = () => {
		dispatch(
			setIsNew({
				isNew: false
			})
		)
		navigate(`/page/edit-filter/${filter.id}`)
	}

	const removeFilter = () => {
		filterService
			.deleteFilter(filter.id)
			.then(() => {
				setCurrentFilters(prev => prev.filter(item => item.id != filter.id))
				dispatch(removeCurrentFilter())
				setFilter('')
			})
	}

	const handleCurrentFilter = data => {
		filterService
			.updateCurrentFilter(data.id)
			.then(() => {
				setFilter(data)
				dispatch(
					setCurrentFilter({
						description: data.descriptionsDTO,
						title: data.titlesDTO,
						technologies: data.technologiesDTO,
						maxPrice: data.maxValue,
						minPrice: data.minValue,
						id: data.id,
						name: data.name
					})
				)
			})
	}

	useEffect(() => {
		filterService
			.getFilters()
			.then(response => {
				setCurrentFilters(response.data)
			})

		filterService
			.getCurrentFilter()
			.then((response) => {
				if (response.data !== '') {
					setFilter(response.data)
					dispatch(
						setCurrentFilter({
							description: response.data.descriptionsDTO,
							title: response.data.titlesDTO,
							technologies: response.data.technologiesDTO,
							maxPrice: response.data.maxValue,
							minPrice: response.data.minValue,
							id: response.data.id,
							name: response.data.name
						})
					)
				}

			})
	}, [])


	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <SourceCard removeCard={deleteSource} item={item} key={index} />
					}
					)}
				</div>
				<div className='current_filter'>
					<div className='current_filter__title'>Теперь создайте фильтр с помощью кнопки "Добавить новый фильтр", который будет заказам из источника заказов</div>
					<div className='current_filter__content'>
						<DropDownList defaultValue={filter.name || 'Текущий фильтр'} elems={currentFilters} open={false} cb={handleCurrentFilter} />
						{filter && <div className='current_filter__content-actions'>
							<Button onClick={editFilter} text={'Редактировать фильтр'} />
							<Button onClick={removeFilter} text={'Удалить фильтр'} />
						</div>}
						<div>
							<Button onClick={addNewFilter} text={'Добавить новый фильтр'} />
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
}

export default FiltersPage