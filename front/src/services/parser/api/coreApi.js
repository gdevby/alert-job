import axios from 'axios';

export const API_URL = `${window.location.origin}/core/api/`;

const core_api = axios.create({
	baseURL: API_URL,
});

export default core_api