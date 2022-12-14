import React from 'react'

import { Routes, Route } from "react-router-dom";

import HomePage from '../../pages/homePage/HomePage'
import FiltersPage from '../../pages/filtersPage/FiltersPage'
import NotificationsPage from '../../pages/notificationsPage/NotificationsPage'
import AddFilterPage from '../../pages/addFilterPage/AddFilterPage'



const Router = () => {
	return <Routes>
			<Route path="/" element={<HomePage />} />
			<Route path="/page/filters" element={<FiltersPage />} />
			<Route path="/page/notifications" element={<NotificationsPage />} />
			<Route path="/page/adding-filter" element={<AddFilterPage />} />
			<Route path="*" element={<div>не найдено</div>} />
		</Routes>
}

export default Router