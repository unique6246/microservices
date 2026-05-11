import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';

const NAV = [
  { to: '/',            icon: '🏠', label: 'Dashboard'     },
  { to: '/customers',   icon: '👥', label: 'Customers'     },
  { to: '/accounts',    icon: '🏦', label: 'Accounts'      },
  { to: '/transactions',icon: '💸', label: 'Transactions'  },
  { to: '/loans',       icon: '📋', label: 'Loans'         },
  { to: '/kyc',         icon: '🪪', label: 'KYC Documents' },
  { to: '/otp',         icon: '🔐', label: 'OTP'           },
  { to: '/fraud',       icon: '🚨', label: 'Fraud'         },
  { to: '/statement',   icon: '📊', label: 'Statement'     },
];

export default function Layout() {
  const navigate  = useNavigate();
  const hasToken  = !!localStorage.getItem('jwt_token');

  useEffect(() => { if (!hasToken) navigate('/login'); }, [hasToken]);
  if (!hasToken) return null;

  return (
    <div className="flex h-screen bg-gray-100 overflow-hidden">
      {/* ── Sidebar ────────────────────────────────────────────────── */}
      <aside className="w-52 bg-blue-950 text-white flex flex-col flex-shrink-0">
        <div className="px-4 py-5 border-b border-blue-800">
          <div className="text-lg font-bold tracking-tight">🏛️ Banking</div>
          <div className="text-xs text-blue-300 mt-0.5">Admin Portal</div>
        </div>

        <nav className="flex-1 overflow-y-auto p-2">
          {NAV.map(({ to, icon, label }) => (
            <NavLink key={to} to={to} end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-2 px-3 py-2 rounded-lg mb-0.5 text-sm transition-colors
                 ${isActive ? 'bg-blue-600 text-white font-semibold' : 'text-blue-200 hover:bg-blue-800 hover:text-white'}`
              }>
              <span>{icon}</span><span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-blue-800">
          <button
            onClick={() => { localStorage.removeItem('jwt_token'); navigate('/login'); }}
            className="w-full text-left text-xs text-blue-300 hover:text-white px-3 py-1.5 rounded">
            🚪 Logout
          </button>
        </div>
      </aside>

      {/* ── Main ───────────────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto">
        <div className="p-6 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}

