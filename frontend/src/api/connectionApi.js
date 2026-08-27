import axios from 'axios';

const BASE_URL = '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue = [];
const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return api(originalRequest);
        }).catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true });
        const newToken = data.accessToken;
        sessionStorage.setItem('accessToken', newToken);
        processQueue(null, newToken);
        originalRequest.headers['Authorization'] = 'Bearer ' + newToken;
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        sessionStorage.removeItem('accessToken');
        window.dispatchEvent(new Event('session-expired'));
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

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

export const saveConnection = async (connectionData) => api.post('/connections/saved', connectionData);
export const getSavedConnections = async () => (await api.get('/connections/saved')).data;
export const connectSavedConnection = async (id) => (await api.post(`/connections/saved/${id}/connect`)).data;
export const deleteSavedConnection = async (id) => api.delete(`/connections/saved/${id}`);
export const disconnectDatabase = async (connectionToken) => api.delete('/connection/session', {
  headers: { 'X-Connection-Token': connectionToken },
});

export const register = async (data) => (await api.post('/auth/register', data)).data;
export const login = async (data) => (await api.post('/auth/login', data)).data;
export const logout = async () => api.post('/auth/logout');

/**
 * Backend health check
 */
export const checkHealth = async () => {
  const response = await api.get('/connection/health');
  return response.data;
};

export default api;
