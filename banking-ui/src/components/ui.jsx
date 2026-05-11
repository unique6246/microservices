// ── Shared UI primitives used across all pages ────────────────────────────────

export const Input = ({ label, type = 'text', value, onChange, placeholder, required, readOnly }) => (
  <div>
    <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
    <input
      type={type}
      value={value}
      onChange={e => onChange && onChange(e.target.value)}
      placeholder={placeholder}
      required={required}
      readOnly={readOnly}
      className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none disabled:bg-gray-50"
    />
  </div>
);

export const Select = ({ label, value, onChange, options, required }) => (
  <div>
    <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      required={required}
      className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none bg-white"
    >
      {options.map(o => (
        <option key={typeof o === 'string' ? o : o.value} value={typeof o === 'string' ? o : o.value}>
          {typeof o === 'string' ? o : o.label}
        </option>
      ))}
    </select>
  </div>
);

export const Alert = ({ type, text }) =>
  text ? (
    <div className={`p-3 rounded-lg text-sm font-medium ${type === 'error' ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-green-50 text-green-700 border border-green-200'}`}>
      {type === 'error' ? '❌ ' : '✅ '}{text}
    </div>
  ) : null;

export const Th = ({ children }) => (
  <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap">{children}</th>
);

export const Td = ({ children }) => (
  <td className="px-3 py-2 text-sm text-gray-700">{children}</td>
);

export const Btn = ({ onClick, children, color = 'blue', type = 'button', disabled = false, size = 'md', full = false }) => {
  const colors = {
    blue:   'bg-blue-600 hover:bg-blue-700',
    green:  'bg-green-600 hover:bg-green-700',
    red:    'bg-red-500 hover:bg-red-600',
    orange: 'bg-orange-500 hover:bg-orange-600',
    gray:   'bg-gray-500 hover:bg-gray-600',
    teal:   'bg-teal-600 hover:bg-teal-700',
  };
  const sizes = { sm: 'px-3 py-1.5 text-xs', md: 'px-4 py-2 text-sm' };
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`${colors[color]} ${sizes[size]} ${full ? 'w-full' : ''} text-white rounded-lg font-medium transition-colors disabled:opacity-50 whitespace-nowrap`}>
      {children}
    </button>
  );
};

export const Card = ({ title, children, action }) => (
  <div className="bg-white rounded-xl shadow p-5">
    {(title || action) && (
      <div className="flex justify-between items-center mb-4">
        {title && <h2 className="text-base font-semibold text-gray-800">{title}</h2>}
        {action}
      </div>
    )}
    {children}
  </div>
);

export const Tabs = ({ tabs, active, onChange }) => (
  <div className="flex flex-wrap gap-1 bg-gray-100 p-1 rounded-xl mb-5">
    {tabs.map(t => (
      <button key={t} onClick={() => onChange(t)}
        className={`px-4 py-1.5 text-sm rounded-lg font-medium transition-colors ${active === t ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
        {t}
      </button>
    ))}
  </div>
);

export const Table = ({ headers, children, empty }) => (
  <div className="overflow-x-auto">
    <table className="min-w-full divide-y divide-gray-200 text-sm">
      <thead className="bg-gray-50">
        <tr>{headers.map(h => <Th key={h}>{h}</Th>)}</tr>
      </thead>
      <tbody className="divide-y divide-gray-100 bg-white">{children}</tbody>
    </table>
    {empty && <p className="text-center py-6 text-gray-400 text-sm">{empty}</p>}
  </div>
);

export const JsonBox = ({ data }) => (
  <pre className="bg-gray-900 text-green-400 rounded-lg p-4 text-xs overflow-auto max-h-64 mt-3">
    {JSON.stringify(data, null, 2)}
  </pre>
);

/** Extract error message from Axios error */
export const errMsg = (e) => {
  const d = e?.response?.data;
  if (!d) return e?.message || 'Unknown error';
  if (typeof d === 'string') return d;
  return d.message || d.error || d.detail || JSON.stringify(d);
};

