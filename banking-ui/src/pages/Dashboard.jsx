import { useState, useEffect } from 'react';
import axios from 'axios';
import { Card } from '../components/ui.jsx';

const QUICK = [
  { label: 'Customers',    icon: '👥', href: '/customers',    color: 'bg-blue-500'   },
  { label: 'Accounts',     icon: '🏦', href: '/accounts',     color: 'bg-green-500'  },
  { label: 'Transactions', icon: '💸', href: '/transactions', color: 'bg-purple-500' },
  { label: 'Loans',        icon: '📋', href: '/loans',        color: 'bg-orange-500' },
  { label: 'KYC',          icon: '🪪', href: '/kyc',          color: 'bg-teal-500'   },
  { label: 'Fraud',        icon: '🚨', href: '/fraud',        color: 'bg-red-500'    },
  { label: 'OTP',          icon: '🔐', href: '/otp',          color: 'bg-indigo-500' },
  { label: 'Statement',    icon: '📊', href: '/statement',    color: 'bg-yellow-500' },
];

export default function Dashboard() {
  const [health,  setHealth]  = useState(null);
  const [loading, setLoading] = useState(true);

  const checkHealth = () => {
    setLoading(true);
    axios.get('/actuator/health')
      .then(({ data }) => setHealth(data))
      .catch(() => setHealth({ status: 'DOWN' }))
      .finally(() => setLoading(false));
  };

  useEffect(() => { checkHealth(); }, []);

  const statusColor = (s) =>
    s === 'UP' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700';

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">🏠 Dashboard</h1>

      {/* Health */}
      <Card title="API Gateway Health"
        action={<button onClick={checkHealth} className="text-xs text-blue-600 hover:underline">🔄 Refresh</button>}>
        {loading ? (
          <span className="text-gray-400 text-sm">Checking…</span>
        ) : (
          <div className="flex flex-wrap gap-3">
            <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium ${statusColor(health?.status)}`}>
              {health?.status === 'UP' ? '✅' : '❌'} Gateway: {health?.status}
            </span>
            {health?.components && Object.entries(health.components).map(([k, v]) => (
              <span key={k} className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium ${statusColor(v.status)}`}>
                {v.status === 'UP' ? '✅' : '❌'} {k}
              </span>
            ))}
          </div>
        )}
      </Card>

      {/* Quick Access */}
      <h2 className="text-base font-semibold text-gray-700 mt-6 mb-3">Quick Access</h2>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {QUICK.map(({ label, icon, href, color }) => (
          <a key={href} href={href}
            className="bg-white rounded-xl shadow hover:shadow-md p-4 flex items-center gap-3 transition-shadow">
            <div className={`${color} text-white text-xl w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0`}>
              {icon}
            </div>
            <span className="font-medium text-gray-700 text-sm">{label}</span>
          </a>
        ))}
      </div>

      {/* Info */}
      <div className="mt-6 bg-blue-50 rounded-xl p-4 border border-blue-100 text-sm text-blue-800">
        <p className="font-medium mb-1">📌 How to use</p>
        <p>1. Use the sidebar to navigate to any module. 2. All forms auto-submit to the API via the gateway. 3. Results appear below each form. 4. Your JWT token is stored in the browser and sent automatically with every request.</p>
      </div>
    </div>
  );
}

