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

export const generateSqlWithAi = async (connectionToken, prompt, dbType, apiKey, signal) => {
  const response = await api.post('/ai/generate', { prompt, dbType }, {
    headers: { 
      'X-Connection-Token': connectionToken,
      'X-Gemini-Api-Key': apiKey
    },
    signal
  });
  return response.data;
};

export const cancelStreamQuery = async (connectionToken, queryId) => {
  return await api.post(`/query/cancel/${queryId}`, null, {
    headers: { 'X-Connection-Token': connectionToken },
  });
};

export const executeStreamQuery = async (connectionToken, payload, onChunk, onComplete, onError) => {
  const token = sessionStorage.getItem('accessToken');
  try {
    const response = await fetch('/api/query/executeStream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Connection-Token': connectionToken,
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    });
    
    if (!response.ok) {
        let errStr = "HTTP Hatası: " + response.status;
        try { const errJson = await response.json(); errStr = errJson.message || errStr; } catch(e){}
        throw new Error(errStr);
    }
    
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let isCanceled = false;

    let arrayBuffer = [];
    const flushInterval = setInterval(() => {
        if (arrayBuffer.length > 0 && !isCanceled) {
            onChunk([...arrayBuffer]);
            arrayBuffer = [];
        }
    }, 150);

    const cleanup = () => {
        isCanceled = true;
        clearInterval(flushInterval);
        reader.cancel();
    };

    try {
      while (true) {
        if (isCanceled) break;
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        
        let boundary = buffer.indexOf('\n');
        while (boundary !== -1) {
            const line = buffer.slice(0, boundary).trim();
            buffer = buffer.slice(boundary + 1);
            if (line) {
                try {
                    const obj = JSON.parse(line);
                    arrayBuffer.push(obj);
                } catch(e) {}
            }
            boundary = buffer.indexOf('\n');
        }
      }
      if (arrayBuffer.length > 0 && !isCanceled) {
          onChunk([...arrayBuffer]);
      }
      onComplete();
    } catch (e) {
      if (!isCanceled) onError(e);
    } finally {
        clearInterval(flushInterval);
    }

    return cleanup;
  } catch (err) {
    onError(err);
    return () => {};
  }
};

export const deleteRow = async (tableName, oldRowValues, connectionToken) => {
  const response = await api.post('/query/delete', {
    tableName,
    oldRowValues
  }, {
    headers: { 'X-Connection-Token': connectionToken },
  });
  return response.data;
};
