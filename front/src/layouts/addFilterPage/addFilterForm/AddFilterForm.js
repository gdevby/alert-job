import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom';

import Btn from '../../../components/common/button/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import TextField from '@mui/material/TextField';
import LimitPopup from '../../../components/common/popup/LimitPopup';
import DescriptionPriceWords from '../../../components/filters/descriptionPriceWords/DescriptionPriceWords';
import Alert from '../../../components/common/alert/Alert';

import { useDispatch, useSelector } from 'react-redux';

import { filterService } from '../../../services/parser/endponits/filterService'
import { setCurrentFilter } from '../../../store/slices/filterSlice';


const AddFilterForm = ({ updateFilter }) => {
	const [filterName, setFilterName] = useState('')
	const [minPrice, setMinPrice] = useState('')
	const [maxPrice, setMaxPrice] = useState('')
	const [isAdded, setIsAdded] = useState(true)
	const [isLimit, setIsLimit] = useState(false)
	const [isOpenForAll, setOpenForAll] = useState(false)
	const [alert, setAlert] = useState(false)

	const { module_id, filter_id } = useParams()

	const { isChoose, isNew, currentFilter } = useSelector(state => state.filter)
	const dispatch = useDispatch()
	const navigate = useNavigate()

	const addFilter = event => {
		if (!filterName.trim()) return
		filterService
			.addFilter(module_id, { name: filterName, minValue: null, maxValue: null })
			.then(response => {
				const id = response.data.id
				dispatch(
					setCurrentFilter({
						description: [],
						title: [],
						negativeDescription: [],
						negativeTitle: [],
						maxPrice: '',
						minPrice: '',
						id: id,
						name: filterName,
						openForAll: false
					})
				)
				return id
			})
			.then((id) => {
				filterService
					.updateCurrentFilter(module_id, id)
					.finally(() => {
						navigate(`/page/edit-filter/${module_id}/${id}`)
					})
				setIsAdded(false)
			})
			.catch(e => {
				if (e.message === 'limit') {
					setIsLimit(true)
				}
				if (e.response?.data?.message.endsWith('exists')) {
					showAlert()
				}
			})
	}
	
	const showAlert = () => {
		setAlert(true)
		setTimeout(() => {
			setAlert(false)
		}, 2000)
	}

	const updateCurrentFilter = (type) => {
		if (!isNew || isChoose || !isAdded) {
			const data = {
				name: type === 'name' ? filterName : null,
				minValue: minPrice == '' ? 0 : minPrice,
				maxValue: maxPrice == '' ? 0 : maxPrice,
			}
			updateFilter(data)
		}
	}

	useEffect(() => {
		if (!isNew || isChoose) {
			setFilterName(currentFilter.name)
			setMinPrice(currentFilter.minPrice || '')
			setMaxPrice(currentFilter.maxPrice || '')
			setOpenForAll(currentFilter.openForAll)
		}
	}, [isChoose, isNew])

	useEffect(() => {
		setIsAdded(isNew)
	}, [isNew])

	const handlerForAll = (e) => {
		setOpenForAll(e.target.checked)
		const data = {
			name: null,
			minValue: minPrice == '' ? 0 : minPrice,
			maxValue: maxPrice == '' ? 0 : maxPrice,
			openForAll: e.target.checked
		}
		updateFilter(data)

	}

	return <div className='mt-1'>
		<LimitPopup handleClose={() => setIsLimit(false)}
			open={isLimit} />
		<TextField
			id="fitler-name"
			label="Введите название"
			variant="standard"
			placeholder='Введите название'
			type='text'
			value={filterName}
			onChange={(e) => setFilterName(e.target.value)}
			onBlur={() => updateCurrentFilter('name')}
			className='w100'
		/>
		<Alert open={alert} content={'Такой фильтр уже существует'} type={'warning'} />
		{!isAdded && <div className='mt-1'>
			<FormControlLabel control={<Checkbox checked={isOpenForAll} size={'small'} onChange={handlerForAll} />} label={'Заказы для всех, не требует премиума (только для fl.ru)'} />
		</div>}
		<div className='addFilter__button'>
			{isAdded ? <Btn text={'Добавить фильтр'} onClick={addFilter} /> : ''}
		</div>
		{!isAdded && <div className='price_block'>
			<TextField
				id="min-price"
				label="Минимальная цена"
				variant="standard"
				placeholder='Минимальная цена'
				className='price_block_input'
				type='number'
				value={minPrice}
				onChange={(e) => setMinPrice(e.target.value)}
				onBlur={() => updateCurrentFilter('minPrice')}
			/>
			<TextField
				id="max-price"
				label="Максимальная цена"
				variant="standard"
				placeholder='Максимальная цена'
				className='price_block_input'
				type='number'
				value={maxPrice}
				onChange={(e) => setMaxPrice(e.target.value)}
				onBlur={() => updateCurrentFilter('maxPrice')}
			/>
			<DescriptionPriceWords filter_id={filter_id} type={''} setIsLimit={setIsLimit} />
		</div>}
	</div>
}

export default AddFilterForm