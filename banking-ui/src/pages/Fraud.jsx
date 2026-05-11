import { useState, useEffect } from 'react';
import api from '../api/client.js';
import { Input, Alert, Btn, Card, Td, Th, errMsg } from '../components/ui.jsx';

export default function Fraud() {
  const [txns,    setTxns]    = useState([]);
  const [page,    setPage]    = useState(0);
  const [total,   setTotal]   = useState(0);
  const [loading, setLoading] = useState(false);
  const [msg,     setMsg]     = useState({ type:'', text:'' });

  const load = async (p = 0) => {
    setLoading(true);
    try {
      const { data } = await api.get(`/transactions/fraud/flagged?page=${p}&size=20`);
      setTxns(data.content ?? (Array.isArray(data) ? data : []));
      setTotal(data.totalElements ?? (Array.isArray(data) ? data.length : 0));
      setPage(p);
      setMsg({ type:'success', text:`Showing ${data.content?.length ?? data.length ?? 0} fraud-flagged transactions` });
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading(false);
  };

  useEffect(() => { load(0); }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">🚨 Fraud Detection</h1>

      <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-5 text-sm text-red-800">
        <p className="font-medium mb-1">🔍 Fraud Detection Rules</p>
        <ul className="list-disc list-inside space-y-1 text-xs">
          <li><strong>LARGE_AMOUNT</strong> — Transaction ≥ ₹5,00,000</li>
          <li><strong>ROUND_AMOUNT</strong> — Amount is a round number (multiple of ₹10,000)</li>
          <li><strong>HIGH_VELOCITY</strong> — More than 5 transactions in 1 minute from the same account</li>
          <li>Flagged transactions are still processed but tagged for review</li>
        </ul>
      </div>

      <Card title={`Fraud-Flagged Transactions${total > 0 ? ` (${total} total)` : ''}`}
        action={<Btn onClick={() => load(0)} size="sm" color="red" disabled={loading}>🔄 Refresh</Btn>}>
        <Alert {...msg} />

        <div className="overflow-x-auto mt-3 max-h-[500px] overflow-y-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-red-50 sticky top-0">
              <tr>{['Reference No','Account No','Type','Amount','Fraud Reasons','Date','Status'].map(h=><Th key={h}>{h}</Th>)}</tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {txns.map((t, i) => (
                <tr key={i} className="hover:bg-red-50">
                  <Td><code className="text-xs bg-gray-100 px-1 rounded">{t.referenceNumber || t.id}</code></Td>
                  <Td>{t.accountNumber}</Td>
                  <Td>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${t.transactionType==='CREDIT'?'bg-green-100 text-green-700':'bg-orange-100 text-orange-700'}`}>
                      {t.transactionType}
                    </span>
                  </Td>
                  <Td className="font-semibold">₹{Number(t.amount).toLocaleString()}</Td>
                  <Td>
                    {t.fraudReasons ? (
                      <div className="flex flex-wrap gap-1">
                        {(Array.isArray(t.fraudReasons) ? t.fraudReasons : [t.fraudReasons]).map(r => (
                          <span key={r} className="px-1.5 py-0.5 bg-red-100 text-red-700 text-xs rounded font-medium">{r}</span>
                        ))}
                      </div>
                    ) : <span className="text-red-600 text-xs font-medium">⚠ FLAGGED</span>}
                  </Td>
                  <Td>{t.createdAt ? new Date(t.createdAt).toLocaleString() : '-'}</Td>
                  <Td>{t.status || 'COMPLETED'}</Td>
                </tr>
              ))}
              {!txns.length && !loading && (
                <tr><td colSpan={7} className="text-center py-10 text-gray-400">No fraud-flagged transactions found</td></tr>
              )}
              {loading && (
                <tr><td colSpan={7} className="text-center py-10 text-gray-400">Loading…</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {total > 20 && (
          <div className="flex justify-end gap-2 mt-3">
            <Btn onClick={() => load(page - 1)} disabled={page === 0} size="sm" color="gray">← Prev</Btn>
            <span className="text-sm text-gray-600 self-center">Page {page + 1}</span>
            <Btn onClick={() => load(page + 1)} disabled={(page + 1) * 20 >= total} size="sm" color="gray">Next →</Btn>
          </div>
        )}
      </Card>
    </div>
  );
}

