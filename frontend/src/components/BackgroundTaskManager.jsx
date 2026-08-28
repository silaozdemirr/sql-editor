import React, { useState, useEffect } from 'react';
import { FiLoader, FiCheckCircle, FiXCircle, FiX } from 'react-icons/fi';
import { getTaskProgress, cancelMockDataTask } from '../api/schemaApi';
import { FiMinus, FiChevronUp } from 'react-icons/fi';

export default function BackgroundTaskManager() {
  const [tasks, setTasks] = useState({});
  const [isVisible, setIsVisible] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);

  useEffect(() => {
    const handleTaskStarted = (e) => {
      const { taskId, tableName } = e.detail;
      setTasks(prev => ({
        ...prev,
        [taskId]: { taskId, tableName, status: 'RUNNING', progress: 0, message: 'İşlem başlatılıyor...', eta: null }
      }));
      setIsVisible(true);
    };

    window.addEventListener('mockDataTaskStarted', handleTaskStarted);
    return () => window.removeEventListener('mockDataTaskStarted', handleTaskStarted);
  }, []);

  useEffect(() => {
    const activeTasks = Object.values(tasks).filter(t => t.status === 'RUNNING');
    if (activeTasks.length === 0) return;

    const interval = setInterval(() => {
      activeTasks.forEach(async (task) => {
        try {
          const data = await getTaskProgress(task.taskId);
          setTasks(prev => {
            const current = prev[task.taskId];
            if (!current) return prev;
            return {
              ...prev,
              [task.taskId]: {
                ...current,
                status: data.status,
                progress: data.totalRows > 0 ? (data.processedRows / data.totalRows) * 100 : 0,
                processedRows: data.processedRows,
                totalRows: data.totalRows,
                message: data.message,
                eta: data.estimatedTimeRemainingMs
              }
            };
          });
        } catch (err) {
          console.error("Task progress fetch error", err);
        }
      });
    }, 2000);

    return () => clearInterval(interval);
  }, [tasks]);

  if (!isVisible && Object.keys(tasks).length === 0) return null;

  const activeTaskCount = Object.values(tasks).filter(t => t.status === 'RUNNING').length;
  
  if (Object.keys(tasks).length === 0) return null;

  
  const handleCancelTask = async (taskId) => {
    try {
      await cancelMockDataTask(taskId);
      setTasks(prev => ({ ...prev, [taskId]: { ...prev[taskId], status: 'CANCELLED', message: 'İptal ediliyor...' }}));
    } catch (e) {
      console.error(e);
    }
  };

  const formatEta = (ms) => {
    if (!ms || ms <= 0) return "Hesaplanıyor...";
    const totalSeconds = Math.floor(ms / 1000);
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    if (m > 0) return `${m} dk ${s} sn kaldı`;
    return `${s} sn kaldı`;
  };

  return (
    <div style={{
      position: 'fixed',
      bottom: '24px',
      right: '24px',
      width: '350px',
      background: 'var(--bg-card, #242424)',
      border: '1px solid var(--border-muted, #444)',
      borderRadius: '8px',
      boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
      zIndex: 999999,
      display: isVisible ? 'block' : 'none',
      fontFamily: 'Inter, sans-serif'
    }}>
      <div style={{
        padding: '12px 16px',
        borderBottom: isMinimized ? 'none' : '1px solid var(--border-subtle, #333)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <h3 style={{ margin: 0, fontSize: '14px', fontWeight: '600', color: 'var(--text-primary)' }}>
          Arkaplan Görevleri {activeTaskCount > 0 ? `(${activeTaskCount})` : ''}
        </h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button onClick={() => setIsMinimized(!isMinimized)} style={{
            background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: 0
          }} title={isMinimized ? "Büyüt" : "Küçült"}>
            {isMinimized ? <FiChevronUp size={16} /> : <FiMinus size={16} />}
          </button>
          <button onClick={() => setIsVisible(false)} style={{
            background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: 0
          }} title="Kapat">
            <FiX size={16} />
          </button>
        </div>
      </div>
      
      {!isMinimized && (
      <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
        {Object.values(tasks).map(task => (
          <div key={task.taskId} style={{ padding: '16px', borderBottom: '1px solid var(--border-subtle, #333)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <strong style={{ fontSize: '13px', color: 'var(--text-primary)' }}>{task.tableName} - Veri Üretimi</strong>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                {task.status === 'RUNNING' && <button onClick={() => handleCancelTask(task.taskId)} style={{ background: 'transparent', border: '1px solid #ef4444', color: '#ef4444', borderRadius: '4px', cursor: 'pointer', padding: '2px 6px', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }} title="İptal Et"><FiXCircle size={10} /> İptal</button>}
                {task.status === 'RUNNING' && <FiLoader className="spin" size={14} color="#3b82f6" />}
                {task.status === 'DONE' && <FiCheckCircle size={14} color="#10b981" />}
                {task.status === 'CANCELLED' && <FiXCircle size={14} color="#f59e0b" />}
                {task.status === 'ERROR' && <FiXCircle size={14} color="#ef4444" />}
              </div>
            </div>
            
            {task.status === 'RUNNING' && (
              <>
                <div style={{ width: '100%', height: '6px', background: 'var(--bg-body, #111)', borderRadius: '3px', marginBottom: '8px', overflow: 'hidden' }}>
                  <div style={{ height: '100%', background: '#3b82f6', width: `${task.progress}%`, transition: 'width 0.5s ease' }}></div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: 'var(--text-secondary)' }}>
                  <span>{task.processedRows?.toLocaleString()} / {task.totalRows?.toLocaleString()} satır</span>
                  <span>{formatEta(task.eta)}</span>
                </div>
              </>
            )}
            
            {task.status !== 'RUNNING' && (
              <div style={{ fontSize: '12px', color: task.status === 'ERROR' ? '#ef4444' : (task.status === 'CANCELLED' ? '#f59e0b' : '#10b981') }}>
                {task.message}
              </div>
            )}
          </div>
        ))}
      </div>
      )}
      <style>
        {`
          @keyframes spin { 100% { transform: rotate(360deg); } }
          .spin { animation: spin 2s linear infinite; }
        `}
      </style>
    </div>
  );
}
