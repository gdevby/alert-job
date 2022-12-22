import axios from 'axios';

export const API_URL = `http://aj.by/parser-alert-job/api/`;

const parser_api = axios.create({
	baseURL: API_URL,
});

export default parser_api