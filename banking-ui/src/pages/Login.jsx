import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function Login() {
  const [username, setUsername] = useState('testuser');
  const [roles,    setRoles]    = useState('USER,ADMIN');
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const { data } = await axios.post('/auth/token', {
        username,
        roles: roles.split(',').map(r => r.trim()).filter(Boolean),
      });
      if (data.access_token) {
        localStorage.setItem('jwt_token', data.access_token);
        navigate('/');
      } else {
        setError('No access_token in response');
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Login failed');
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-blue-950 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-sm">
        <div className="text-center mb-6">
          <div className="text-5xl mb-3">🏛️</div>
          <h1 className="text-2xl font-bold text-blue-950">Banking Admin</h1>
          <p className="text-sm text-gray-500 mt-1">Obtain a JWT token to access all services</p>
        </div>

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Username</label>
            <input type="text" value={username} onChange={e => setUsername(e.target.value)} required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Roles (comma-separated)</label>
            <input type="text" value={roles} onChange={e => setRoles(e.target.value)}
              placeholder="USER,ADMIN"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
          </div>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm p-3 rounded-lg">❌ {error}</div>
          )}
          <button type="submit" disabled={loading}
            className="w-full bg-blue-700 hover:bg-blue-800 text-white font-semibold py-2.5 rounded-lg transition-colors disabled:opacity-50 text-sm">
            {loading ? 'Requesting token…' : '🔑 Get Token & Login'}
          </button>
        </form>

        <p className="text-xs text-gray-400 text-center mt-5">
          Calls <code className="bg-gray-100 px-1 rounded">/auth/token</code> on the API Gateway (dev mode)
        </p>
      </div>
    </div>
  );
}

