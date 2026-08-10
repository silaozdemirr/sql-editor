import api from './connectionApi';

export const executeQuery = async (sessionId, sql) => {
  const response = await api.post('/query/execute', { sessionId, sql });
  return response.data;
};
