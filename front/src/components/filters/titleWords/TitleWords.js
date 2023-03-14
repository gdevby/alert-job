import React, { useState, useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux';

import Button from '../../button/Button'
import Words from '../word/Words'

import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle'
import useDebounce from '../../../hooks/use-debounce'

import { filterService } from '../../../services/parser/endponits/filterService'

const TitleWords = ({ filter_id }) => {
	const [isOpen, setIsOpen] = useState(false)
	const [words, setWords] = useState([])
	const [selectValue, setSelectValue] = useState('')
	const [result, setResult] = useState([])
	const [page, setPage] = useState(0)
	const [nextPage, setNextPage] = useState(false)
	const [searchedWords, setSearchedWords] = useState([])
	const [isFetching, setIsFetching] = useState(true)
	const [totalCount, setTotalCount] = useState(0)

	const listRef = React.createRef()

	const debouncedSearchTerm = useDebounce(selectValue, 1000)

	const { titleWords } = useSelector(state => state.filter.currentFilter)
	const { isNew } = useSelector(state => state.filter)

	const openSearch = () => {
		setIsOpen(true)
		setPage(0)
		getWords('')
	}

	const addWord = (word) => {
		filterService
			.addWordToFilter('title-word', filter_id, word.id)
			.then(() => {
				setWords((prev) => [...prev, word])
				setResult((prev) => [...prev, word]);
				setIsOpen(false)
				setSelectValue('')
			})
	}


	const getWords = (text, currentPage = 0) => {
		if (currentPage == 0 || totalCount != result.length) {
			filterService
				.getWords('title-word', text, currentPage)
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
				.addWord(selectValue, 'title-word')
				.then(response => {
					addWord(response.data)
				})
		}
	}

	const closePopup = () => {
		setIsOpen(false)
	}

	const remove = (id) => {
		filterService
			.deleteWord('title-word', filter_id, id)
			.then(() => {
				setWords((prev) => prev.filter(item => item.id !== id))
			})
	}

	const handleSelect = (event) => {
		console.log(event.target)
		const word = { id: event.target.id, name: event.target.textContent.trim() }
		addWord(word)
	}

	useEffect(() => {
		if (titleWords && !isNew) {
			setWords((prev) => [...prev, ...titleWords])
		}
	}, [])

	/*useEffect(() => {
		if (filter_id) {
			filterService
			.getCurrentFilter()
			.then((response) => {
				response.data.titlesDTO &&setWords(response.data.titlesDTO)
			})
		}
	}, [filter_id])*/

	useEffect(() => {
		if (!isFetching) {
			if (debouncedSearchTerm) {
				getWords(debouncedSearchTerm, 0)
			} else {
				setResult([]);
			}
		}

	}, [debouncedSearchTerm])

	useEffect(() => {
		if (isFetching) {
			getWords(selectValue, page)
		}
	}, [isFetching])

	const scrollHandler = (e) => {
		const coordinates = e.target.getBoundingClientRect()
		const top = e.target.scrollTop
		const fullHeight = e.target.scrollHeight
		if (Math.floor(top + coordinates.height) + 1 == fullHeight) {
			setIsFetching(true)
		}

	};

	useEffect(() => {
		const list = listRef.current
		if (list) {
			list.addEventListener('scroll', scrollHandler);

			return function() {
				list.removeEventListener('scroll', scrollHandler);
			};
		}

		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [])

	return <>
		<Dialog open={isOpen} onClose={closePopup}>
			<DialogTitle>
				<div className='searchPopup__header'>
					<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
					Поиск по ключевым словам
					<input type='text' onChange={changeWord} value={selectValue} />
				</div>
			</DialogTitle>
			<DialogContent className='scroll' ref={listRef}>
				<div className='searchPopup__body-list'>
					{result && result.map(item => <div className='searchPopup__body-list__item'
						id={item.id} key={item.name}
						onClick={handleSelect}>{item.name}</div>
					)}
				</div>
			</DialogContent>
			<DialogActions>
				<Button onClick={add} text={'Добавить'} variant='contained' />
			</DialogActions>
		</Dialog>
		<div>
			<p>Уведомлять, если в названии содержится</p>
			<Button text={'Добавить'} onClick={openSearch} variant='contained' />
		</div>
		<div className='addedWords'>
			<Words items={words} remove={remove} />
		</div>
	</>
}

export default TitleWords