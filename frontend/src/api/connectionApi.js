import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

/**
 * Bağlantı testini yapar - bağlanıp kapar
 * @param {Object} connectionData - host, port, database, username, password, dbType
 */
export const testConnection = async (connectionData) => {
  const response = await api.post('/connection/test', connectionData);
  return response.data;
};

/**
 * Tam bağlantı kurar - sessionId döner
 * @param {Object} connectionData - host, port, database, username, password, dbType
 */
export const connectToDatabase = async (connectionData) => {
  const response = await api.post('/connection/connect', connectionData);
  return response.data;
};

/**
 * Backend health check
 */
export const checkHealth = async () => {
  const response = await api.get('/connection/health');
  return response.data;
};

export default api;
