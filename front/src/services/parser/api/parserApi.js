import axios from 'axios';

export const API_URL = `${window.location.origin}/parser-alert-job/api/`;

const parser_api = axios.create({
	baseURL: API_URL,
});

export default parser_api