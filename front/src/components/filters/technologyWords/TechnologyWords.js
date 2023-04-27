import React, { useState, useEffect, useRef } from 'react'
import { useSelector } from 'react-redux';
import { useParams } from 'react-router-dom'

import Btn from '../../common/button/Button'
import Words from '../word/Words'
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle'
import ListItem from '../listItem/ListItem';
import CircularProgress from '@mui/material/CircularProgress';

import useDebounce from '../../../hooks/use-debounce'

import { filterService } from '../../../services/parser/endponits/filterService'

const TechnologyWords = ({ filter_id, type, setIsLimit }) => {
	const [isOpen, setIsOpen] = useState(false)
	const [words, setWords] = useState([])
	const [selectValue, setSelectValue] = useState('')
	const [result, setResult] = useState([])
	const [page, setPage] = useState(0)
	const [nextPage, setNextPage] = useState(false)
	const [searchedWords, setSearchedWords] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [totalCount, setTotalCount] = useState(0)
	const [preloader, setPreloader] = useState(false)

	const { module_id } = useParams()

	const listRef = React.createRef()
	const inputRef = React.createRef()

	const debouncedSearchTerm = useDebounce(selectValue, 1000)

	const technologyWords =
		type == '' ? useSelector(state => state.filter.currentFilter.technologyWords)
			: useSelector(state => state.filter.currentFilter.negativeTechnologyWords)

	const { isNew } = useSelector(state => state.filter)

	const openSearch = () => {
		setIsOpen(true)
		setPage(0)
		getWords('')
	}

	const addWord = (word) => {
		filterService
			.addWordToFilter('technology-word', filter_id, word.id, type)
			.then(() => {
				setWords((prev) => [...prev, word])
				if (!searchedWords.includes(word.name)) {
					setResult((prev) => [...prev, word]);	
				}
				//setResult((prev) => [...prev, word]);
				//setIsOpen(false)
				//setSelectValue('')
			}).catch(e => {
				if (e.message === 'limit') {
					//setIsOpen(false)
					setIsLimit(true)
				}
			})
	}


	const getWords = (text, currentPage = 0) => {
		if (currentPage == 0 || totalCount != result.length) {
			filterService
				.getWords('technology-word', text, currentPage, `&module_id=${module_id}`)
				.then(response => {
					setPage((prev) => prev + 1);
					setTotalCount((prev) => response.data.totalElements);

					if (currentPage == 0) {
						setResult(response.data.content);
						setSearchedWords(response.data.content.map(item => item.name));
					} else {
						setSearchedWords((prev) => [...prev, ...response.data.content.map(item => item.name)]);
						setResult((prev) => [...prev, ...response.data.content]);
					}

				})
				.finally(() => {
					setIsFetching(false)
					setPreloader(false)
				})
		} else {
			setIsFetching(false)
		}

	}

	const changeWord = (event) => {
		setSelectValue(event.target.value)

		//getWords(event.target.value)
	}


	const add = () => {
		if (!searchedWords.includes(selectValue)) {
			filterService
				.addWord(selectValue, 'technology-word')
				.then(response => {
					addWord(response.data)
				})
		}
	}

	const closePopup = () => {
		setSelectValue('')
		setIsOpen(false)
	}

	const remove = (id) => {
		filterService
			.deleteWord('technology-word', filter_id, id, type)
			.then(() => {
				setWords((prev) => prev.filter(item => item.id !== id))
			})
	}

	/*useEffect(() => {
		if (filter_id) {
			filterService
			.getCurrentFilter()
			.then((response) => {
				response.data.technologiesDTO && setWords(response.data.technologiesDTO)
			})
		}
	}, [filter_id])*/

	const handleSelect = (item) => {
		const word = { id: item.id, name: item.name }
		addWord(word)
	}

	useEffect(() => {
		if (technologyWords && !isNew) {
			console.log(technologyWords)
			setWords((prev) => [...prev, ...technologyWords])
		}
	}, [])

	useEffect(() => {
		if (!isFetching) {
			if (debouncedSearchTerm.length == 0) {
				setPage(0)
			}
			setPreloader(true)
			getWords(debouncedSearchTerm, 0)
		}
	}, [debouncedSearchTerm])

	useEffect(() => {
		const input = inputRef.current
		if (input) {
			input.focus()
		}
	}, [inputRef])

	useEffect(() => {
		if (isFetching) {
			getWords(selectValue, page)
		}
	}, [isFetching])

	const scrollHandler = (e) => {
		const coordinates = e.target.getBoundingClientRect()
		const top = e.target.scrollTop
		const fullHeight = e.target.scrollHeight
		console.log('top, coordinates.height, fullHeight', top, coordinates.height, fullHeight)
		if (top + coordinates.height == fullHeight) {
			setIsFetching(true)
		}

	};

	useEffect(() => {
		const list = listRef.current
		console.log(list)
		if (list) {
			list.addEventListener('scroll', scrollHandler);

			return function() {
				list.removeEventListener('scroll', scrollHandler);
			};
		}

		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [listRef])

	const checkForExist = (name) => {
		let isExist = false
		for (let i = 0; i < words.length; i++) {
			if (words[i].name === name) {
				isExist = true
				break;
			}
		}	
		return isExist
	}

	return <>
		<Dialog open={isOpen} onClose={closePopup}>
			<DialogTitle>
				<div className='searchPopup__header'>
					<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
					Поиск по ключевым словам
					<input type='text' onChange={changeWord} value={selectValue} ref={inputRef} />
				</div>
			</DialogTitle>
			<DialogContent className='scroll' ref={listRef}>
				<div className='searchPopup__body-head'>
					<div>Слово</div>
					<div>Частота</div>
				</div>
				<div className='searchPopup__body-list' >
					{preloader ? <div className='text-center'><CircularProgress /></div> :
						result && result.map(item => <ListItem key={item.name + item.id} onClick={handleSelect} item={item} 
														isAdded={checkForExist(item.name)}
						/>)}
				</div>
			</DialogContent>
			<DialogActions className='searchPopup__actions'>
				<Btn onClick={add} text={'Добавить'} variant='contained' />
			</DialogActions>
		</Dialog>
		<div className='wordsContains__title'>
			{type == '' ? <p>Уведомлять, если технологии содержат (Только хабр)</p> : <p>Уведомлять, если технологии не содержат (Только хабр)</p>}
			<Btn text={'Добавить'} onClick={openSearch} variant='contained' />
		</div>
		<div className='addedWords'>
			<Words items={words} remove={remove} />
		</div>
	</>
}

export default TechnologyWords