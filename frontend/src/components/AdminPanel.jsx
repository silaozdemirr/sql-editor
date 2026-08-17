import React, { useState, useEffect } from 'react';
import { fetchUsers, updateUserRole, deleteUser } from '../api/adminApi';

export default function AdminPanel({ onClose }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);

  const loadUsers = async () => {
    try {
      const data = await fetchUsers();
      setUsers(data);
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleRoleChange = async (id, role) => {
    try {
      await updateUserRole(id, role);
      setUsers(users.map(u => u.id === id ? { ...u, role_name: role } : u));
    } catch (err) {
      alert('Rol güncellenemedi: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Bu kullanıcıyı silmek istediğinize emin misiniz?')) return;
    try {
      await deleteUser(id);
      setUsers(users.filter(u => u.id !== id));
    } catch (err) {
      alert('Kullanıcı silinemedi: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="admin-panel-overlay">
      <div className="admin-panel">
        <div className="admin-panel-header">
          <h2>Admin Paneli</h2>
          <button className="icon-button" onClick={onClose} title="Kapat">✕</button>
        </div>
        {error && <div className="schema-error" style={{ margin: '1rem' }}>{error}</div>}
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>E-posta</th>
                <th>Görünen Ad</th>
                <th>Rol</th>
                <th>İşlemler</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.email}</td>
                  <td>{user.displayName}</td>
                  <td>
                    <select
                      className="form-input admin-select"
                      value={user.role_name}
                      onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    >
                      <option value="ADMIN">ADMIN</option>
                      <option value="EDITOR">EDITOR</option>
                      <option value="READ_ONLY">READ_ONLY</option>
                    </select>
                  </td>
                  <td>
                    <button className="btn-secondary btn-sm" onClick={() => handleDelete(user.id)}>
                      Sil
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && !error && (
                <tr>
                  <td colSpan="5" className="text-center text-muted">Kullanıcı bulunamadı.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
