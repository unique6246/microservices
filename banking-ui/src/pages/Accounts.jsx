import { useState, useEffect } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, Tabs, Td, Th, JsonBox, errMsg } from '../components/ui.jsx';

const TABS = ['Create','List','Search','Freeze/Unfreeze','Daily Limit','PIN','Close Account'];

export default function Accounts() {
  const [tab, setTab] = useState('Create');
  const [msg, setMsg] = useState({ type:'', text:'' });
  const M = (type, text) => setMsg({ type, text });

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-5">🏦 Account Management</h1>
      <Tabs tabs={TABS} active={tab} onChange={t => { setTab(t); setMsg({}); }} />
      {tab === 'Create'          && <CreateAccount    msg={msg} M={M} />}
      {tab === 'List'            && <ListAccounts     msg={msg} M={M} />}
      {tab === 'Search'          && <SearchAccount    msg={msg} M={M} />}
      {tab === 'Freeze/Unfreeze' && <FreezeUnfreeze   msg={msg} M={M} />}
      {tab === 'Daily Limit'     && <DailyLimit       msg={msg} M={M} />}
      {tab === 'PIN'             && <PinManagement    msg={msg} M={M} />}
      {tab === 'Close Account'   && <CloseAccount     msg={msg} M={M} />}
    </div>
  );
}

/* ── Create Account ───────────────────────────────────────────────────────── */
function CreateAccount({ msg, M }) {
  const [form, setForm] = useState({ accountType:'SAVINGS', customerId:'', nomineeName:'', maturityDate:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f => ({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const payload = { accountType: form.accountType, customerId: Number(form.customerId), nomineeName: form.nomineeName || undefined };
      if (form.accountType === 'FIXED_DEPOSIT' && form.maturityDate) payload.maturityDate = form.maturityDate;
      const { data } = await api.post('/accounts', payload);
      setResult(data); M('success', `Account created: ${data.accountInfo?.accountNumber}`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Open a New Account">
      <form onSubmit={onSubmit} className="space-y-3 max-w-md">
        <Select label="Account Type" value={form.accountType} onChange={v => sf('accountType',v)}
          options={['SAVINGS','CURRENT','FIXED_DEPOSIT','LOAN']} />
        <Input label="Customer ID" type="number" value={form.customerId} onChange={v => sf('customerId',v)} required />
        <Input label="Nominee Name (optional)" value={form.nomineeName} onChange={v => sf('nomineeName',v)} />
        {form.accountType === 'FIXED_DEPOSIT' && (
          <Input label="Maturity Date" type="date" value={form.maturityDate} onChange={v => sf('maturityDate',v)} />
        )}
        <Alert {...msg} />
        <Btn type="submit" disabled={loading}>{loading ? 'Creating…' : '🏦 Create Account'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── List Accounts ────────────────────────────────────────────────────────── */
function ListAccounts({ msg, M }) {
  const [accounts, setAccounts] = useState([]);
  const load = async () => {
    try {
      const { data } = await api.get('/accounts?size=100');
      setAccounts(data.content ?? (Array.isArray(data) ? data : []));
    } catch (e) { M('error', errMsg(e)); }
  };
  useEffect(() => { load(); }, []);

  return (
    <Card title="All Accounts"
      action={<button onClick={load} className="text-xs text-blue-600 hover:underline">🔄 Refresh</button>}>
      <Alert {...msg} />
      <div className="overflow-x-auto mt-2 max-h-[500px] overflow-y-auto">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50 sticky top-0">
            <tr>{['Account No','Type','Customer ID','Balance','Status','Daily Limit','Min Balance','IFSC'].map(h => <Th key={h}>{h}</Th>)}</tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {accounts.map(a => (
              <tr key={a.accountNumber} className="hover:bg-gray-50">
                <Td><code className="text-xs bg-gray-100 px-1 rounded">{a.accountNumber}</code></Td>
                <Td><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                  a.accountType==='SAVINGS'?'bg-blue-100 text-blue-700':
                  a.accountType==='CURRENT'?'bg-green-100 text-green-700':
                  a.accountType==='FIXED_DEPOSIT'?'bg-yellow-100 text-yellow-700':'bg-purple-100 text-purple-700'}`}>{a.accountType}</span></Td>
                <Td>{a.customerId}</Td>
                <Td>₹{Number(a.balance).toLocaleString()}</Td>
                <Td><span className={`px-2 py-0.5 rounded-full text-xs font-medium ${a.status==='ACTIVE'?'bg-green-100 text-green-700':'bg-red-100 text-red-700'}`}>{a.status}</span></Td>
                <Td>₹{Number(a.dailyTxnLimit).toLocaleString()}</Td>
                <Td>₹{Number(a.minBalance).toLocaleString()}</Td>
                <Td>{a.ifscCode}</Td>
              </tr>
            ))}
            {!accounts.length && <tr><td colSpan={8} className="text-center py-8 text-gray-400">No accounts found</td></tr>}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

/* ── Search Account ───────────────────────────────────────────────────────── */
function SearchAccount({ msg, M }) {
  const [accNo, setAccNo]           = useState('');
  const [custId, setCustId]         = useState('');
  const [result, setResult]         = useState(null);

  const byNumber = async () => {
    if (!accNo) return;
    try { const { data } = await api.get(`/accounts/account/${accNo}`); setResult(data); M('success','Found'); }
    catch (e) { M('error', errMsg(e)); setResult(null); }
  };
  const byCustomer = async () => {
    if (!custId) return;
    try { const { data } = await api.get(`/accounts/customer/${custId}`); setResult(data); M('success','Found'); }
    catch (e) { M('error', errMsg(e)); setResult(null); }
  };

  return (
    <Card title="Search Account">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-2xl">
        <div className="space-y-3">
          <Input label="By Account Number" value={accNo} onChange={setAccNo} placeholder="e.g. ACC1234567890" />
          <Btn onClick={byNumber}>🔍 Search by Account No</Btn>
        </div>
        <div className="space-y-3">
          <Input label="By Customer ID" type="number" value={custId} onChange={setCustId} />
          <Btn onClick={byCustomer} color="teal">🔍 Search by Customer</Btn>
        </div>
      </div>
      <Alert {...msg} />
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── Freeze / Unfreeze ───────────────────────────────────────────────────── */
function FreezeUnfreeze({ msg, M }) {
  const [accNo,  setAccNo]  = useState('');
  const [reason, setReason] = useState('Suspicious activity');
  const [result, setResult] = useState(null);

  const freeze = async () => {
    try { const { data } = await api.patch(`/accounts/${accNo}/freeze?reason=${encodeURIComponent(reason)}`); setResult(data); M('success','Account frozen.'); }
    catch (e) { M('error', errMsg(e)); }
  };
  const unfreeze = async () => {
    try { const { data } = await api.patch(`/accounts/${accNo}/unfreeze`); setResult(data); M('success','Account unfrozen.'); }
    catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="Freeze / Unfreeze Account">
      <div className="space-y-3 max-w-md">
        <Input label="Account Number" value={accNo} onChange={setAccNo} required />
        <Input label="Reason (for freeze)" value={reason} onChange={setReason} />
        <Alert {...msg} />
        <div className="flex gap-3">
          <Btn onClick={freeze}   color="orange">🥶 Freeze Account</Btn>
          <Btn onClick={unfreeze} color="green">🔓 Unfreeze Account</Btn>
        </div>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── Daily Limit ─────────────────────────────────────────────────────────── */
function DailyLimit({ msg, M }) {
  const [accNo,  setAccNo]  = useState('');
  const [limit,  setLimit]  = useState('');
  const [result, setResult] = useState(null);

  const update = async () => {
    try { const { data } = await api.patch(`/accounts/${accNo}/limits?dailyLimit=${limit}`); setResult(data); M('success','Daily limit updated.'); }
    catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="Update Daily Transaction Limit">
      <div className="space-y-3 max-w-sm">
        <Input label="Account Number" value={accNo} onChange={setAccNo} />
        <Input label="New Daily Limit (₹)" type="number" value={limit} onChange={setLimit} placeholder="e.g. 100000" />
        <Alert {...msg} />
        <Btn onClick={update}>💰 Update Limit</Btn>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── PIN Management ──────────────────────────────────────────────────────── */
function PinManagement({ msg, M }) {
  const [accNo,  setAccNo]  = useState('');
  const [pin,    setPin]    = useState('');
  const [result, setResult] = useState(null);

  const setPin_ = async () => {
    try { const { data } = await api.post(`/accounts/${accNo}/pin/set?pin=${pin}`); setResult(data); M('success','PIN set successfully.'); }
    catch (e) { M('error', errMsg(e)); }
  };
  const verifyPin = async () => {
    try { const { data } = await api.post(`/accounts/${accNo}/pin/verify?pin=${pin}`); setResult(data); M('success', data.valid ? 'PIN is correct ✅' : 'PIN is incorrect ❌'); }
    catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="TPIN Management">
      <div className="space-y-3 max-w-sm">
        <Input label="Account Number" value={accNo} onChange={setAccNo} />
        <Input label="4-Digit PIN" type="password" value={pin} onChange={setPin} placeholder="••••" />
        <Alert {...msg} />
        <div className="flex gap-3">
          <Btn onClick={setPin_}  color="blue">🔒 Set PIN</Btn>
          <Btn onClick={verifyPin} color="teal">✔ Verify PIN</Btn>
        </div>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

/* ── Close Account ───────────────────────────────────────────────────────── */
function CloseAccount({ msg, M }) {
  const [accNo,  setAccNo]  = useState('');
  const [result, setResult] = useState(null);

  const close = async () => {
    if (!confirm(`Close account ${accNo}? Balance must be zero.`)) return;
    try { const { data } = await api.delete(`/accounts/${accNo}`); setResult(data); M('success','Account closed.'); }
    catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="Close Account">
      <p className="text-sm text-yellow-700 bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4">
        ⚠️ Account must have zero balance before closing.
      </p>
      <div className="space-y-3 max-w-sm">
        <Input label="Account Number" value={accNo} onChange={setAccNo} />
        <Alert {...msg} />
        <Btn onClick={close} color="red">🗑 Close Account</Btn>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

