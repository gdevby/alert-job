import React, { useState, useEffect, useRef } from 'react'
import { useDispatch, useSelector } from 'react-redux';

import Button from '../../button/Button'
import Words from '../word/Words'

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
	}

	const addWord = (word) => {
		filterService
			.addWordToFilter('title-word', filter_id, word.id)
			.then(() => {
				setWords((prev) => [...prev, word])
				setIsOpen(false)
			})
	}


	const getWords = (text, currentPage = 0) => {
		if (page == 0 || totalCount != result.length) {
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
		}else {
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
		const word = { id: event.target.id, name: event.target.textContent.trim() }
		addWord(word)
	}

	useEffect(() => {
		if (titleWords && !isNew) {
			setWords((prev) => [...prev, ...titleWords])
		}
	}, [])
	useEffect(() => {
		if (debouncedSearchTerm) {
			getWords(debouncedSearchTerm, 0)
		} else {
			setResult([]);
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
		if (top + coordinates.height == fullHeight) {
			setIsFetching(true)
		}

	};

	useEffect(() => {
		const list = listRef.current
		list.addEventListener('scroll', scrollHandler);

		return function() {
			list.removeEventListener('scroll', scrollHandler);
		};
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [])

	return <>
		<div className={isOpen ? 'searchPopup searchPopup__open' : 'searchPopup searchPopup__close'}>
			<div className='searchPopup__content'>
				<div className='searchPopup__header'>
					<div className='searchPopup__header-close' onClick={closePopup}>Закрыть</div>
					<input type='text' onChange={changeWord} value={selectValue} />
				</div>
				<div className='searchPopup__body'>
					<div className='searchPopup__body-list' ref={listRef}>
						{result && result.map(item => <div className='searchPopup__body-list__item'
							id={item.id} key={item.id}
							onClick={handleSelect}>{item.name}</div>
						)}
					</div>
				</div>
				<div className='searchPopup__footer'>
					<Button onClick={add} text={'Добавить'} />
				</div>
			</div>
		</div>
		Уведомлять, если технологии содержат
		<Button text={'Добавить'} onClick={openSearch} />
		<div className='addedWords'>
			<Words items={words} remove={remove} />
		</div>
	</>
}

export default TitleWords