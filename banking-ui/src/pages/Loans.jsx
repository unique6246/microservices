import { useState } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, Tabs, Td, Th, JsonBox, errMsg } from '../components/ui.jsx';

const TABS = ['Apply Loan','My Loans','EMI Schedule','Pay EMI','Overdue Loans'];

export default function Loans() {
  const [tab, setTab] = useState('Apply Loan');
  const [msg, setMsg] = useState({ type:'', text:'' });
  const M = (type, text) => setMsg({ type, text });

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-5">📋 Loan Management</h1>
      <Tabs tabs={TABS} active={tab} onChange={t => { setTab(t); setMsg({}); }} />
      {tab === 'Apply Loan'    && <ApplyLoan    msg={msg} M={M} />}
      {tab === 'My Loans'      && <MyLoans      msg={msg} M={M} />}
      {tab === 'EMI Schedule'  && <EmiSchedule  msg={msg} M={M} />}
      {tab === 'Pay EMI'       && <PayEmi       msg={msg} M={M} />}
      {tab === 'Overdue Loans' && <OverdueLoans msg={msg} M={M} />}
    </div>
  );
}

function ApplyLoan({ msg, M }) {
  const [form, setForm] = useState({
    customerId:'', principalAmount:'', tenureMonths:'', loanType:'PERSONAL',
    linkedSavingsAccount:'', nomineeName:''
  });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const RATES = { PERSONAL:'12%', HOME:'8.5%', AUTO:'9%', EDUCATION:'7%' };

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const { data } = await api.post('/loans/apply', {
        customerId: Number(form.customerId),
        principalAmount: Number(form.principalAmount),
        tenureMonths: Number(form.tenureMonths),
        loanType: form.loanType,
        linkedSavingsAccount: form.linkedSavingsAccount,
        nomineeName: form.nomineeName || undefined,
      });
      setResult(data); M('success', `Loan applied! EMI: ₹${Number(data.emiAmount).toFixed(2)}`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Apply for a Loan">
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4 text-xs text-blue-800">
        <strong>Rates:</strong> {Object.entries(RATES).map(([k,v]) => `${k}: ${v}`).join(' · ')}
        &nbsp;|&nbsp;<strong>Min amount:</strong> ₹10,000
      </div>
      <form onSubmit={onSubmit} className="space-y-3 max-w-lg">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Customer ID"       type="number" value={form.customerId}      onChange={v=>sf('customerId',v)}      required />
          <Select label="Loan Type"         value={form.loanType}            onChange={v=>sf('loanType',v)}    options={['PERSONAL','HOME','AUTO','EDUCATION']} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Principal Amount (₹)" type="number" value={form.principalAmount} onChange={v=>sf('principalAmount',v)} required />
          <Input label="Tenure (months)"       type="number" value={form.tenureMonths}    onChange={v=>sf('tenureMonths',v)}    required />
        </div>
        <Input label="Linked Savings Account" value={form.linkedSavingsAccount} onChange={v=>sf('linkedSavingsAccount',v)} required />
        <Input label="Nominee Name (optional)" value={form.nomineeName} onChange={v=>sf('nomineeName',v)} />
        <Alert {...msg} />
        <Btn type="submit" disabled={loading}>{loading ? 'Applying…' : '📋 Apply for Loan'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

function MyLoans({ msg, M }) {
  const [custId, setCustId] = useState('');
  const [loans,  setLoans]  = useState([]);

  const load = async () => {
    try {
      const { data } = await api.get(`/loans/customer/${custId}`);
      setLoans(Array.isArray(data) ? data : []); M('success',`${data.length ?? '?'} loans found`);
    } catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="Customer Loans">
      <div className="flex gap-3 mb-4 max-w-sm">
        <Input label="Customer ID" type="number" value={custId} onChange={setCustId} />
        <div className="pt-5"><Btn onClick={load}>🔍 Fetch</Btn></div>
      </div>
      <Alert {...msg} />
      {loans.length > 0 && (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>{['ID','Type','Principal','EMI','Tenure','Paid','Status','Next EMI'].map(h=><Th key={h}>{h}</Th>)}</tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loans.map(l => (
                <tr key={l.id} className="hover:bg-gray-50">
                  <Td>{l.id}</Td>
                  <Td>{l.loanType}</Td>
                  <Td>₹{Number(l.principalAmount).toLocaleString()}</Td>
                  <Td>₹{Number(l.emiAmount).toFixed(2)}</Td>
                  <Td>{l.tenureMonths}m</Td>
                  <Td>{l.emisPaid}/{l.totalEmis}</Td>
                  <Td><span className={`px-2 py-0.5 rounded text-xs font-medium ${l.status==='ACTIVE'?'bg-green-100 text-green-700':'bg-red-100 text-red-700'}`}>{l.status}</span></Td>
                  <Td>{l.nextEmiDate}</Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function EmiSchedule({ msg, M }) {
  const [loanId, setLoanId] = useState('');
  const [schedule, setSchedule] = useState([]);

  const load = async () => {
    try {
      const { data } = await api.get(`/loans/${loanId}/schedule`);
      setSchedule(Array.isArray(data) ? data : []); M('success',`${data.length} EMIs`);
    } catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="EMI Repayment Schedule">
      <div className="flex gap-3 mb-4 max-w-sm">
        <Input label="Loan ID" type="number" value={loanId} onChange={setLoanId} />
        <div className="pt-5"><Btn onClick={load}>📅 Load Schedule</Btn></div>
      </div>
      <Alert {...msg} />
      {schedule.length > 0 && (
        <div className="overflow-x-auto max-h-96 overflow-y-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 sticky top-0">
              <tr>{['EMI #','Due Date','Principal','Interest','Penalty','Total','Status'].map(h=><Th key={h}>{h}</Th>)}</tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {schedule.map(e => (
                <tr key={e.emiNumber} className="hover:bg-gray-50">
                  <Td>{e.emiNumber}</Td>
                  <Td>{e.dueDate}</Td>
                  <Td>₹{Number(e.principalComponent).toFixed(2)}</Td>
                  <Td>₹{Number(e.interestComponent).toFixed(2)}</Td>
                  <Td>₹{Number(e.penaltyAmount || 0).toFixed(2)}</Td>
                  <Td>₹{Number(e.totalPaid || e.emiAmount).toFixed(2)}</Td>
                  <Td><span className={`px-2 py-0.5 rounded text-xs ${e.status==='PAID'?'bg-green-100 text-green-700':e.status==='OVERDUE'?'bg-red-100 text-red-700':'bg-yellow-100 text-yellow-700'}`}>{e.status}</span></Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function PayEmi({ msg, M }) {
  const [loanId, setLoanId] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const pay = async () => {
    setLoading(true); M('','');
    try {
      const { data } = await api.post(`/loans/${loanId}/pay-emi`);
      setResult(data); M('success',`EMI #${data.emiNumber} paid! ₹${Number(data.totalPaid).toFixed(2)}`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Pay Next EMI">
      <div className="space-y-3 max-w-sm">
        <Input label="Loan ID" type="number" value={loanId} onChange={setLoanId} />
        <Alert {...msg} />
        <Btn onClick={pay} color="green" disabled={loading}>{loading ? 'Processing…' : '💳 Pay Next EMI'}</Btn>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

function OverdueLoans({ msg, M }) {
  const [loans, setLoans] = useState([]);
  const load = async () => {
    try { const { data } = await api.get('/loans/overdue'); setLoans(Array.isArray(data) ? data : []); M('success',`${data.length ?? '?'} overdue`); }
    catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="Overdue Loans (Admin)"
      action={<Btn onClick={load} size="sm" color="red">🔍 Load Overdue</Btn>}>
      <Alert {...msg} />
      {loans.length > 0 ? (
        <div className="overflow-x-auto mt-3">
          <table className="min-w-full text-sm divide-y divide-gray-200">
            <thead className="bg-gray-50"><tr>{['ID','Customer','Type','Principal','EMI','Status','Next EMI'].map(h=><Th key={h}>{h}</Th>)}</tr></thead>
            <tbody className="divide-y divide-gray-100">
              {loans.map(l => (
                <tr key={l.id} className="hover:bg-gray-50">
                  <Td>{l.id}</Td><Td>{l.customerId}</Td><Td>{l.loanType}</Td>
                  <Td>₹{Number(l.principalAmount).toLocaleString()}</Td>
                  <Td>₹{Number(l.emiAmount).toFixed(2)}</Td>
                  <Td><span className="px-2 py-0.5 rounded text-xs bg-red-100 text-red-700">{l.status}</span></Td>
                  <Td>{l.nextEmiDate}</Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-gray-400 mt-3 text-center py-4">Click "Load Overdue" to fetch</p>
      )}
    </Card>
  );
}

