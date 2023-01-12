import axios from 'axios';

export const API_URL = `${window.location.origin}/parser/api/`;

const parser_api = axios.create({
	baseURL: API_URL,
});

export default parser_api