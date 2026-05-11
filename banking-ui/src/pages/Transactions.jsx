import { useState } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, Tabs, Td, Th, JsonBox, errMsg } from '../components/ui.jsx';

const TABS = ['Credit','Debit','Transfer','NEFT / RTGS / IMPS','UPI','View Transactions'];

export default function Transactions() {
  const [tab, setTab] = useState('Credit');
  const [msg, setMsg] = useState({ type:'', text:'' });
  const M = (type, text) => setMsg({ type, text });

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-5">💸 Transactions</h1>
      <Tabs tabs={TABS} active={tab} onChange={t => { setTab(t); setMsg({}); }} />
      {tab === 'Credit'              && <Credit       msg={msg} M={M} />}
      {tab === 'Debit'               && <Debit        msg={msg} M={M} />}
      {tab === 'Transfer'            && <Transfer     msg={msg} M={M} />}
      {tab === 'NEFT / RTGS / IMPS'  && <NeftRtgs     msg={msg} M={M} />}
      {tab === 'UPI'                 && <Upi          msg={msg} M={M} />}
      {tab === 'View Transactions'   && <ViewTxns     msg={msg} M={M} />}
    </div>
  );
}

/* ── Credit ──────────────────────────────────────────────────────────────── */
function Credit({ msg, M }) {
  const [form, setForm] = useState({ accountNumber:'', amount:'', idempotencyKey:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const payload = { accountNumber: form.accountNumber, amount: Number(form.amount) };
      if (form.idempotencyKey) payload.idempotencyKey = form.idempotencyKey;
      const { data } = await api.post('/transactions/credit', payload);
      setResult(data); M('success', 'Credit processed successfully!');
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Credit Amount to Account">
      <form onSubmit={onSubmit} className="space-y-3 max-w-sm">
        <Input label="Account Number" value={form.accountNumber} onChange={v=>sf('accountNumber',v)} required />
        <Input label="Amount (₹)" type="number" value={form.amount} onChange={v=>sf('amount',v)} required />
        <Input label="Idempotency Key (optional)" value={form.idempotencyKey} onChange={v=>sf('idempotencyKey',v)} placeholder="unique-key-001" />
        <Alert {...msg} />
        <Btn type="submit" color="green" disabled={loading}>{loading ? 'Processing…' : '💳 Credit Account'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── Debit ───────────────────────────────────────────────────────────────── */
function Debit({ msg, M }) {
  const [form, setForm] = useState({ accountNumber:'', amount:'', idempotencyKey:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const payload = { accountNumber: form.accountNumber, amount: Number(form.amount) };
      if (form.idempotencyKey) payload.idempotencyKey = form.idempotencyKey;
      const { data } = await api.post('/transactions/debit', payload);
      setResult(data); M('success', 'Debit processed successfully!');
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Debit Amount from Account">
      <form onSubmit={onSubmit} className="space-y-3 max-w-sm">
        <Input label="Account Number" value={form.accountNumber} onChange={v=>sf('accountNumber',v)} required />
        <Input label="Amount (₹)" type="number" value={form.amount} onChange={v=>sf('amount',v)} required />
        <Input label="Idempotency Key (optional)" value={form.idempotencyKey} onChange={v=>sf('idempotencyKey',v)} placeholder="unique-key-002" />
        <Alert {...msg} />
        <Btn type="submit" color="orange" disabled={loading}>{loading ? 'Processing…' : '💳 Debit Account'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── Internal Transfer ───────────────────────────────────────────────────── */
function Transfer({ msg, M }) {
  const [form, setForm] = useState({ fromAccount:'', toAccount:'', amount:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const { data } = await api.post('/transactions/transfer', { ...form, amount: Number(form.amount) });
      setResult(data); M('success', 'Transfer completed!');
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Internal Transfer Between Accounts">
      <form onSubmit={onSubmit} className="space-y-3 max-w-sm">
        <Input label="From Account" value={form.fromAccount} onChange={v=>sf('fromAccount',v)} required />
        <Input label="To Account"   value={form.toAccount}   onChange={v=>sf('toAccount',v)}   required />
        <Input label="Amount (₹)" type="number" value={form.amount} onChange={v=>sf('amount',v)} required />
        <Alert {...msg} />
        <Btn type="submit" disabled={loading}>{loading ? 'Transferring…' : '↔️ Transfer'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── NEFT / RTGS / IMPS ──────────────────────────────────────────────────── */
function NeftRtgs({ msg, M }) {
  const [form, setForm] = useState({
    fromAccount:'', beneficiaryAccountNumber:'', beneficiaryName:'',
    ifscCode:'', bankName:'', amount:'', channel:'NEFT', remarks:''
  });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const { data } = await api.post('/transactions/neft', { ...form, amount: Number(form.amount) });
      setResult(data); M('success', `${form.channel} transfer initiated!`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="NEFT / RTGS / IMPS Transfer">
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4 text-xs text-blue-800">
        <strong>Rules:</strong> NEFT — any amount (free) · RTGS — min ₹2,00,000 (₹24 fee) · IMPS — any amount (₹15 fee cap)
      </div>
      <form onSubmit={onSubmit} className="space-y-3 max-w-lg">
        <Select label="Channel" value={form.channel} onChange={v=>sf('channel',v)} options={['NEFT','RTGS','IMPS']} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="From Account"      value={form.fromAccount}              onChange={v=>sf('fromAccount',v)}              required />
          <Input label="Amount (₹)" type="number" value={form.amount}            onChange={v=>sf('amount',v)}                   required />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Beneficiary Account No" value={form.beneficiaryAccountNumber} onChange={v=>sf('beneficiaryAccountNumber',v)} required />
          <Input label="Beneficiary Name"        value={form.beneficiaryName}          onChange={v=>sf('beneficiaryName',v)}          required />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="IFSC Code"  value={form.ifscCode}  onChange={v=>sf('ifscCode',v)}  placeholder="HDFC0001234" required />
          <Input label="Bank Name"  value={form.bankName}  onChange={v=>sf('bankName',v)}  required />
        </div>
        <Input label="Remarks (optional)" value={form.remarks} onChange={v=>sf('remarks',v)} />
        <Alert {...msg} />
        <Btn type="submit" disabled={loading}>{loading ? 'Processing…' : `🏧 Send ${form.channel}`}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── UPI ─────────────────────────────────────────────────────────────────── */
function Upi({ msg, M }) {
  const [form, setForm] = useState({ fromAccount:'', upiId:'', amount:'', remarks:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const { data } = await api.post('/transactions/upi', { ...form, amount: Number(form.amount) });
      setResult(data); M('success', 'UPI payment sent!');
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="UPI Payment">
      <form onSubmit={onSubmit} className="space-y-3 max-w-sm">
        <Input label="From Account"   value={form.fromAccount} onChange={v=>sf('fromAccount',v)} required />
        <Input label="UPI ID (VPA)"   value={form.upiId}       onChange={v=>sf('upiId',v)}       placeholder="name@upi" required />
        <Input label="Amount (₹)" type="number" value={form.amount} onChange={v=>sf('amount',v)} required />
        <Input label="Remarks"        value={form.remarks}     onChange={v=>sf('remarks',v)} />
        <Alert {...msg} />
        <Btn type="submit" color="teal" disabled={loading}>{loading ? 'Sending…' : '📱 Pay via UPI'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── View Transactions ───────────────────────────────────────────────────── */
function ViewTxns({ msg, M }) {
  const [accNo, setAccNo]   = useState('');
  const [type,  setType]    = useState('CREDIT');
  const [refNo, setRefNo]   = useState('');
  const [txns,  setTxns]    = useState([]);
  const [single, setSingle] = useState(null);

  const byAccount = async () => {
    try { const { data } = await api.get(`/transactions/account/${accNo}`); setTxns(Array.isArray(data) ? data : []); M('success',`${data.length ?? '?'} transactions found`); }
    catch (e) { M('error', errMsg(e)); }
  };
  const byType = async () => {
    try { const { data } = await api.get(`/transactions/type/${type}`); setTxns(Array.isArray(data) ? data : []); M('success',`${data.length ?? '?'} transactions`); }
    catch (e) { M('error', errMsg(e)); }
  };
  const byRef = async () => {
    try { const { data } = await api.get(`/transactions/ref/${refNo}`); setSingle(data); M('success','Found'); }
    catch (e) { M('error', errMsg(e)); setSingle(null); }
  };

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card title="By Account Number">
          <div className="space-y-2">
            <Input label="Account Number" value={accNo} onChange={setAccNo} />
            <Btn onClick={byAccount} size="sm">🔍 Fetch</Btn>
          </div>
        </Card>
        <Card title="By Transaction Type">
          <div className="space-y-2">
            <Select label="Type" value={type} onChange={setType} options={['CREDIT','DEBIT','TRANSFER','NEFT','RTGS','IMPS','UPI']} />
            <Btn onClick={byType} color="teal" size="sm">🔍 Fetch</Btn>
          </div>
        </Card>
        <Card title="By Reference Number">
          <div className="space-y-2">
            <Input label="Reference No" value={refNo} onChange={setRefNo} placeholder="TXN-..." />
            <Btn onClick={byRef} color="orange" size="sm">🔍 Fetch</Btn>
          </div>
        </Card>
      </div>
      <Alert {...msg} />
      {single && <Card title="Transaction Detail"><JsonBox data={single} /></Card>}
      {txns.length > 0 && (
        <Card title={`Transactions (${txns.length})`}>
          <div className="overflow-x-auto max-h-96 overflow-y-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 sticky top-0">
                <tr>{['Ref No','Account','Type','Amount','Status','Date','Fraud?'].map(h=><Th key={h}>{h}</Th>)}</tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {txns.map((t,i) => (
                  <tr key={i} className="hover:bg-gray-50">
                    <Td><code className="text-xs">{t.referenceNumber || t.id}</code></Td>
                    <Td>{t.accountNumber}</Td>
                    <Td><span className={`px-2 py-0.5 rounded text-xs font-medium ${t.transactionType==='CREDIT'?'bg-green-100 text-green-700':'bg-red-100 text-red-700'}`}>{t.transactionType}</span></Td>
                    <Td>₹{Number(t.amount).toLocaleString()}</Td>
                    <Td>{t.status}</Td>
                    <Td>{t.createdAt ? new Date(t.createdAt).toLocaleString() : '-'}</Td>
                    <Td>{t.fraudFlag ? <span className="text-red-600 font-bold">⚠ YES</span> : 'No'}</Td>
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

