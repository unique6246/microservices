import { useState } from 'react';
import api from '../api/client.js';
import { Input, Alert, Btn, Card, Td, Th, errMsg } from '../components/ui.jsx';

export default function Statement() {
  const now    = new Date();
  const firstOfMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01T00:00:00`;
  const lastOfMonth  = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(new Date(now.getFullYear(), now.getMonth()+1, 0).getDate()).padStart(2,'0')}T23:59:59`;

  const [accNo,   setAccNo]   = useState('');
  const [from,    setFrom]    = useState(firstOfMonth);
  const [to,      setTo]      = useState(lastOfMonth);
  const [txns,    setTxns]    = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState('');
  const [msg,     setMsg]     = useState({ type:'', text:'' });

  const loadStatement = async () => {
    if (!accNo) { setMsg({ type:'error', text:'Account number is required' }); return; }
    setLoading('stmt'); setMsg({});
    try {
      const { data } = await api.get(`/transactions/account/${accNo}/statement?from=${from}&to=${to}`);
      setTxns(Array.isArray(data) ? data : []);
      setMsg({ type:'success', text:`${data.length} transactions in range` });
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading('');
  };

  const loadSummary = async () => {
    if (!accNo) { setMsg({ type:'error', text:'Account number is required' }); return; }
    setLoading('sum'); setMsg({});
    try {
      const { data } = await api.get(`/transactions/account/${accNo}/summary?from=${from}&to=${to}`);
      setSummary(data);
      setMsg({ type:'success', text:'Summary loaded' });
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading('');
  };

  const totalCredits = txns.filter(t => t.transactionType === 'CREDIT').reduce((s,t) => s + Number(t.amount), 0);
  const totalDebits  = txns.filter(t => t.transactionType !== 'CREDIT').reduce((s,t) => s + Number(t.amount), 0);

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">📊 Account Statement & Summary</h1>

      {/* Filters */}
      <Card title="Filter Criteria">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
          <Input label="Account Number" value={accNo} onChange={setAccNo} placeholder="ACC1234567890" />
          <Input label="From Date/Time" type="datetime-local" value={from.slice(0,16)} onChange={v => setFrom(v + ':00')} />
          <Input label="To Date/Time"   type="datetime-local" value={to.slice(0,16)}   onChange={v => setTo(v + ':59')}   />
        </div>
        <div className="flex gap-3">
          <Btn onClick={loadStatement} disabled={loading==='stmt'}>
            {loading==='stmt' ? 'Loading…' : '📄 Load Statement'}
          </Btn>
          <Btn onClick={loadSummary} color="teal" disabled={loading==='sum'}>
            {loading==='sum' ? 'Loading…' : '📊 Load Summary'}
          </Btn>
        </div>
        <Alert {...msg} />
      </Card>

      {/* Summary Card */}
      {summary && (
        <div className="mt-5 grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[
            { label:'Total Credits',  val: `₹${Number(summary.totalCredits  ?? 0).toLocaleString()}`, color:'bg-green-50 border-green-200 text-green-800' },
            { label:'Total Debits',   val: `₹${Number(summary.totalDebits   ?? 0).toLocaleString()}`, color:'bg-red-50 border-red-200 text-red-800' },
            { label:'Net Balance',    val: `₹${Number(summary.netBalance    ?? summary.totalCredits - summary.totalDebits ?? 0).toLocaleString()}`, color:'bg-blue-50 border-blue-200 text-blue-800' },
            { label:'Total Txns',     val: summary.transactionCount ?? txns.length, color:'bg-gray-50 border-gray-200 text-gray-800' },
          ].map(({ label, val, color }) => (
            <div key={label} className={`rounded-xl border p-4 ${color}`}>
              <p className="text-xs font-medium opacity-70 mb-1">{label}</p>
              <p className="text-xl font-bold">{val}</p>
            </div>
          ))}
        </div>
      )}

      {/* Quick summary from transactions if summary not loaded */}
      {txns.length > 0 && !summary && (
        <div className="mt-5 grid grid-cols-3 gap-4">
          <div className="rounded-xl border bg-green-50 border-green-200 text-green-800 p-4">
            <p className="text-xs font-medium opacity-70 mb-1">Total Credits</p>
            <p className="text-xl font-bold">₹{totalCredits.toLocaleString()}</p>
          </div>
          <div className="rounded-xl border bg-red-50 border-red-200 text-red-800 p-4">
            <p className="text-xs font-medium opacity-70 mb-1">Total Debits</p>
            <p className="text-xl font-bold">₹{totalDebits.toLocaleString()}</p>
          </div>
          <div className="rounded-xl border bg-blue-50 border-blue-200 text-blue-800 p-4">
            <p className="text-xs font-medium opacity-70 mb-1">Net</p>
            <p className="text-xl font-bold">₹{(totalCredits - totalDebits).toLocaleString()}</p>
          </div>
        </div>
      )}

      {/* Transaction Table */}
      {txns.length > 0 && (
        <Card title={`Statement — ${txns.length} transactions`}>
          <div className="overflow-x-auto max-h-[500px] overflow-y-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 sticky top-0">
                <tr>{['Date','Ref No','Type','Amount','Balance After','Description','Fraud?'].map(h=><Th key={h}>{h}</Th>)}</tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {txns.map((t, i) => (
                  <tr key={i} className={`hover:bg-gray-50 ${t.fraudFlag ? 'bg-red-50' : ''}`}>
                    <Td>{t.createdAt ? new Date(t.createdAt).toLocaleString() : '-'}</Td>
                    <Td><code className="text-xs">{t.referenceNumber || t.id}</code></Td>
                    <Td>
                      <span className={`px-2 py-0.5 rounded text-xs font-medium ${t.transactionType==='CREDIT'?'bg-green-100 text-green-700':'bg-red-100 text-red-700'}`}>
                        {t.transactionType}
                      </span>
                    </Td>
                    <Td className={t.transactionType==='CREDIT'?'text-green-700 font-semibold':'text-red-700 font-semibold'}>
                      {t.transactionType==='CREDIT' ? '+' : '-'}₹{Number(t.amount).toLocaleString()}
                    </Td>
                    <Td>{t.balanceAfter != null ? `₹${Number(t.balanceAfter).toLocaleString()}` : '-'}</Td>
                    <Td>{t.description || t.remarks || '-'}</Td>
                    <Td>{t.fraudFlag ? <span className="text-red-600 text-xs font-bold">⚠ YES</span> : 'No'}</Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
}

