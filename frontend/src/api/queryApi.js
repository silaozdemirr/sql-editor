import api from './connectionApi';

export const executeQuery = async (connectionToken, sql) => {
  const response = await api.post('/query/execute', { sql }, { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

export const explainQuery = async (connectionToken, sql) => {
  const response = await api.post('/query/explain', { sql }, { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

export const getQueryHistory = async (connectionToken) => {
  const response = await api.get('/query/history', { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

export const manageTransaction = async (connectionToken, action) => {
  const response = await api.post(`/query/transaction?action=${action}`, null, { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

export const updateCell = async (tableName, updatedColumn, newValue, oldRowValues, connectionToken) => {
  const response = await api.post('/query/update', {
    tableName,
    updatedColumn,
    newValue,
    oldRowValues
  }, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};
