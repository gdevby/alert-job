import axios from 'axios';

export const API_URL = `${window.location.origin}/core/api/`;

const core_api = axios.create({
	baseURL: API_URL,
});

core_api.interceptors.response.use(res => {
	return res;
}, error => {
	if (error.code == "ERR_NETWORK") {
		return Promise.reject({...error, code: 302})	
	}
	return Promise.reject(error)
	;
});

export default core_api