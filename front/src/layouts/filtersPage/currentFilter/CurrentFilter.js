import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom';

import DropDownList from '../../../components/common/dropDownList/DropDowList';
import Btn from '../../../components/common/button/Button';
import Popup from '../../../components/common/popup/Popup';

import { filterService } from '../../../services/parser/endponits/filterService';

import { setCurrentFilter, setIsNew, removeCurrentFilter } from '../../../store/slices/filterSlice';

const CurrentFilter = () => {
	const [currentFilters, setCurrentFilters] = useState([])
	const [filter, setFilter] = useState('')
	const [isOpenPopup, setIsOpenPopup] = useState(false)
	const [popup, setPopup] = useState({})

	const { id } = useParams()

	const dispatch = useDispatch()
	const navigate = useNavigate()

	const { currentFilter, isChoose } = useSelector(state => state.filter)

	useEffect(() => {
		console.log(isChoose)
		if (isChoose) {
			setFilter(currentFilter)
		}
	}, [isChoose])


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
			.finally(() => {
				setIsOpenPopup(false)
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

	const confirmRemovesFilter = () => {
		setPopup({
			title: 'Подвердите удаление',
			content: `Вы действительно хотите удалить фильтр с именем ${filter.name}?`,
			actions: <>
				<Btn onClick={handleClosePopup} text={'Закрыть'} />
				<Btn onClick={removeFilter} text={'Удалить'} />
			</>
		})
		setIsOpenPopup(true)
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
	
	const handleClosePopup = () => {
		setIsOpenPopup(false)
	}

	return <div className='current_filter'>
		<Popup
			handleClose={handleClosePopup}
			open={isOpenPopup}
			title={popup.title}
			content={popup.content}
			actions={popup.actions}
		/>
		<div className='current_filter__title'>Теперь создайте фильтр с помощью кнопки "Добавить новый фильтр", который будет фильтровать заказы.</div>
		<div className='current_filter__content'>
			<DropDownList className='current_filter__list' defaultValue={filter.id} label={'Выберите фильтр'} elems={currentFilters} onClick={handleCurrentFilter} defaultLabe={'Выберите фильтр'} />
			<div className='current_filter__content-actions'>
				{filter && <><Btn onClick={editFilter} text={'Редактировать фильтр'} variant='contained' />
					<Btn onClick={confirmRemovesFilter} text={'Удалить фильтр'} variant='contained' /></>}
				<Btn onClick={addNewFilter} text={'Добавить новый фильтр'} variant='contained' />
			</div>
		</div>
	</div>
}

export default CurrentFilter