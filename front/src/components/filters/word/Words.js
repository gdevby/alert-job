import React, { useState, useEffect } from 'react'

import Word from '../word/Word'

const Words = ({items, remove}) => {
	const [words, setWords] = useState([])
	
	useEffect(() => {
		setWords(items)
	}, [items])
	
	const removeItem = (id) => {
		setWords((prev) => prev.filter(item => item.id !== id))
		remove(id)
	}
	
	return <>
		{
			words && words.map((item, index) => <Word key={index} item={item} remove={removeItem}/>)
		}
	</>
	
}

export default Words