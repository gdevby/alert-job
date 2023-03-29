import React, { lazy, Suspense  } from 'react'
import { Routes, Route } from "react-router-dom";
import { useAuth } from '../../../hooks/useAuth';

import CircularProgress from '@mui/material/CircularProgress';
//import HomePage from '../../../pages/homePage/HomePage'
//import FiltersPage from '../../../pages/filtersPage/FiltersPage'
//import NotificationsPage from '../../../pages/notificationsPage/NotificationsPage'
//import AddFilterPage from '../../../pages/addFilterPage/AddFilterPage'
//import ModulesPage from '../../../pages/modulesPage/ModulesPage';


const FiltersPage = lazy(() => import('../../../pages/filtersPage/FiltersPage') )
const HomePage = lazy(() => import('../../../pages/homePage/HomePage') )
const NotificationsPage = lazy(() => import('../../../pages/notificationsPage/NotificationsPage') )
const AddFilterPage = lazy(() => import('../../../pages/addFilterPage/AddFilterPage') )
const ModulesPage = lazy(() => import('../../../pages/modulesPage/ModulesPage') )
const Router = () => {
	const { isAuth } = useAuth()

	return <Routes>
		
		{isAuth && <>
			<Route path="/page/filters/:id" element={<Suspense fallback={<CircularProgress />}><FiltersPage /></Suspense>} />
			<Route path="/page/modules" element={<Suspense fallback={<CircularProgress />}><ModulesPage /></Suspense>} />
			<Route path="/page/notifications" element={<Suspense fallback={<CircularProgress />}><NotificationsPage /></Suspense>} />
			<Route path="/page/adding-filter/:module_id" element={<Suspense fallback={<CircularProgress />}><AddFilterPage /></Suspense>} />
			<Route path="/page/edit-filter/:module_id/:filter_id" element={<Suspense fallback={<CircularProgress />}><AddFilterPage /></Suspense>} />
		</>}
		<Route path="/" element={<Suspense fallback={<CircularProgress />}><HomePage /></Suspense>} />
		<Route path="/index.html" element={<Suspense fallback={<CircularProgress />}><HomePage /></Suspense>} />
		<Route path="*" element={<Suspense fallback={<CircularProgress />}><HomePage /></Suspense>} />
	</Routes>
}

export default Router