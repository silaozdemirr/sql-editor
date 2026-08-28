import api from './connectionApi';

/** Bağlantıya ait tüm veritabanlarını getirir. */
export const getDatabases = async (connectionToken) => {
  const response = await api.get('/schema/databases', { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

/** Bağlı oturumun belirli bir veritabanındaki tablo ve view listesini getirir. */
export const getSchema = async (connectionToken, database = '') => {
  const ts = new Date().getTime();
  const url = database ? `/schema?database=${encodeURIComponent(database)}&_t=${ts}` : `/schema?_t=${ts}`;
  const response = await api.get(url, { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

/** Bir tabloya ait kolonları getirir. */
export const getTableColumns = async (connectionToken, tableName, database = '') => {
  const url = `/schema/${encodeURIComponent(tableName)}/columns` + (database ? `?database=${encodeURIComponent(database)}` : '');
  const response = await api.get(url, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};

/** Bir tablonun DDL kodunu getirir. */
export const getTableDDL = async (connectionToken, database, tableName) => {
  const url = database ? `/schema/${encodeURIComponent(tableName)}/ddl?database=${encodeURIComponent(database)}` : `/schema/${encodeURIComponent(tableName)}/ddl`;
  const response = await api.get(url, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};

export const getErd = async (connectionToken, database = '') => {
  const url = database ? `/schema/erd?database=${encodeURIComponent(database)}` : '/schema/erd';
  const response = await api.get(url, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};

export const createTable = async (data) => {
  const token = sessionStorage.getItem('activeToken');
  const response = await api.post('/ddl/create-table', data, { headers: { 'X-Connection-Token': token } });
  return response.data;
};

export const generateMockData = async (data) => {
  const token = sessionStorage.getItem('activeToken');
  const response = await api.post('/mock/generate', data, { headers: { 'X-Connection-Token': token } });
  return response.data;
};


export const getTaskProgress = async (taskId) => {
  const response = await api.get(`/mock/progress/${taskId}`);
  return response.data;
};

export const cancelMockDataTask = async (taskId) => {
  const response = await api.delete(`/mock/progress/${taskId}`);
  return response.data;
};
