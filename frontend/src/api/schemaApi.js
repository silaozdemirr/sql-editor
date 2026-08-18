import api from './connectionApi';

/** Bağlı oturumun tablo ve view listesini getirir. */
export const getSchema = async (connectionToken) => {
  const response = await api.get('/schema', { headers: { 'X-Connection-Token': connectionToken } });
  return response.data;
};

/** Bir tabloya ait kolonları getirir. */
export const getTableColumns = async (connectionToken, tableName) => {
  const response = await api.get(`/schema/${encodeURIComponent(tableName)}/columns`, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};

/** Bir tablonun DDL kodunu getirir. */
export const getTableDDL = async (connectionToken, tableName) => {
  const response = await api.get(`/schema/${encodeURIComponent(tableName)}/ddl`, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};

export const getErd = async (connectionToken) => {
  const response = await api.get('/schema/erd', {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};
