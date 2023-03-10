import React from 'react'

import { Routes, Route } from "react-router-dom";

import HomePage from '../../pages/homePage/HomePage'
import FiltersPage from '../../pages/filtersPage/FiltersPage'
import NotificationsPage from '../../pages/notificationsPage/NotificationsPage'
import AddFilterPage from '../../pages/addFilterPage/AddFilterPage'
import ModulesPage from '../../pages/modulesPage/ModulesPage';



const Router = () => {
	return <Routes>
			<Route path="/" element={<HomePage />} />
			<Route path="/page/filters/:id" element={<FiltersPage />} />
			<Route path="/page/modules" element={<ModulesPage />} />
			<Route path="/page/notifications" element={<NotificationsPage />} />
			<Route path="/page/adding-filter/:module_id" element={<AddFilterPage />} />
			<Route path="/page/edit-filter/:module_id/:filter_id" element={<AddFilterPage />} />
			<Route path="/index.html" element={<HomePage />} />
			<Route path="*" element={<div>не найдено</div>} />
		</Routes>
}

export default Router