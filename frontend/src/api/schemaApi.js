import api from './connectionApi';

/** Bağlı oturumun tablo ve view listesini getirir. */
export const getSchema = async (sessionId) => {
  const response = await api.get('/schema', { params: { sessionId } });
  return response.data;
};

/** Bir tabloya ait kolonları getirir. */
export const getTableColumns = async (sessionId, tableName) => {
  const response = await api.get(`/schema/${encodeURIComponent(tableName)}/columns`, {
    params: { sessionId },
  });
  return response.data;
};
