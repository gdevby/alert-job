import React, { useState, useEffect } from 'react'

import SourcePanel from '../../components/sourcePanel/SourcePanel'
import DropDownList from '../../components/dropDownList/DropDowList'
import Btn from '../../components/button/Button'

import { sourceService } from '../../services/parser/endponits/sourceService'

import SourceCard from '../../components/sourceCard/SourceCard'

import { filterService } from '../../services/parser/endponits/filterService'
import { ordersService } from '../../services/parser/endponits/orderService'

import { removeCurrentFilter } from '../../store/slices/filterSlice'

import { setCurrentFilter, setIsNew } from '../../store/slices/filterSlice'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom'
import { styled } from '@mui/material/styles';
import Paper from '@mui/material/Paper';

import './filtersPage.scss'

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

	const deleteSource = source_id => {
		sourceService.deleteSource(id, source_id).then(() => {
			const newSources = sourse.filter(item => item.id != source_id)
			setSources(newSources)
		})
	}

	useEffect(() => {
		console.log(isChoose)
		if (isChoose) {
			setFilter(currentFilter)
		}
	}, [isChoose])

	useEffect(() => {
		sourceService
			.getSources(id)
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
		navigate(`/page/adding-filter/${id}`)
	}

	const editFilter = () => {
		dispatch(
			setIsNew({
				isNew: false
			})
		)
		navigate(`/page/edit-filter/${id}/${filter.id}`)
	}

	const removeFilter = () => {
		filterService
			.deleteFilter(id, filter.id)
			.then(() => {
				setCurrentFilters(prev => prev.filter(item => item.id != filter.id))
				dispatch(removeCurrentFilter())
				setFilter('')
			})
	}

	const handleCurrentFilter = data => {
		filterService
			.updateCurrentFilter(id, data.id)
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
			.getFilters(id)
			.then(response => {
				setCurrentFilters(response.data)
			})

		filterService
			.getCurrentFilter(id)
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

	const showOrders = () => {
		ordersService
			.getOrders(id)
			.then(console.log)
	}


	const Item = styled(Paper)(({ theme }) => ({
		backgroundColor: theme.palette.mode === 'dark' ? '#1A2027' : '#fff',
		...theme.typography.body2,
		padding: theme.spacing(0.5),
		textAlign: 'center',
		color: theme.palette.text.secondary,
	}));

	return <div className='filtersPage'>
		<div className='container'>
			<div>
				<SourcePanel addSource={addSource} module_id={id} />
				<div className='sourceList'>
					{sourse.length > 0 && sourse.map((item, index) => {
						return <Item key={index}><SourceCard removeCard={deleteSource} item={item} /></Item>
					}
					)}
				</div>
				<div className='current_filter'>
					<div className='current_filter__title'>Теперь создайте фильтр с помощью кнопки "Добавить новый фильтр", который будет заказам из источника заказов</div>
					<div className='current_filter__content'>
						<DropDownList className='current_filter__list' defaultValue={filter.id} label={'Выберите фильтр'} elems={currentFilters} onClick={handleCurrentFilter} defaultLabe={'Выберите фильтр'} />
						{filter && <div className='current_filter__content-actions'>
							<Btn onClick={editFilter} text={'Редактировать фильтр'} variant='contained' />
							<Btn onClick={removeFilter} text={'Удалить фильтр'} variant='contained' />
						</div>}
						<div>
							<Btn onClick={addNewFilter} text={'Добавить новый фильтр'} variant='contained' />
						</div>
					</div>
				</div>
				<div>
					<Btn onClick={showOrders} text={'Показать заказы'} variant='contained' />
				</div>
			</div>
		</div>
	</div>
}

export default FiltersPage