import api from './connectionApi';

export const executeQuery = async (connectionToken, sql) => {
  const response = await api.post('/query/execute', { sql }, { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};
