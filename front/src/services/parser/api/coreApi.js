import axios from 'axios';

export const API_URL = `http://aj.by/core-alert-job/api/`;

const core_api = axios.create({
	baseURL: API_URL,
});

export default core_api