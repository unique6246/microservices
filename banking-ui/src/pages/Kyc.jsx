import { useState } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, Tabs, Td, Th, JsonBox, errMsg } from '../components/ui.jsx';

const TABS = ['Upload Document','View Documents','KYC Status','Admin Review'];

export default function Kyc() {
  const [tab, setTab] = useState('Upload Document');
  const [msg, setMsg] = useState({ type:'', text:'' });
  const M = (type, text) => setMsg({ type, text });

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-5">🪪 KYC Document Management</h1>
      <Tabs tabs={TABS} active={tab} onChange={t => { setTab(t); setMsg({}); }} />
      {tab === 'Upload Document' && <Upload     msg={msg} M={M} />}
      {tab === 'View Documents'  && <ViewDocs   msg={msg} M={M} />}
      {tab === 'KYC Status'      && <KycStatus  msg={msg} M={M} />}
      {tab === 'Admin Review'    && <AdminReview msg={msg} M={M} />}
    </div>
  );
}

function Upload({ msg, M }) {
  const [form, setForm] = useState({ customerId:'', documentType:'AADHAAR', documentNumber:'', documentUrl:'' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const sf = (k,v) => setForm(f=>({...f,[k]:v}));

  const onSubmit = async (e) => {
    e.preventDefault(); setLoading(true); M('','');
    try {
      const { data } = await api.post('/kyc/documents/upload', { ...form, customerId: Number(form.customerId) });
      setResult(data); M('success', `Document uploaded! ID: ${data.id}`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Upload KYC Document">
      <form onSubmit={onSubmit} className="space-y-3 max-w-md">
        <Input label="Customer ID" type="number" value={form.customerId} onChange={v=>sf('customerId',v)} required />
        <Select label="Document Type" value={form.documentType} onChange={v=>sf('documentType',v)}
          options={['AADHAAR','PAN','PASSPORT','DRIVING_LICENSE','VOTER_ID']} />
        <Input label="Document Number" value={form.documentNumber} onChange={v=>sf('documentNumber',v)}
          placeholder="e.g. 1234-5678-9012 or ABCDE1234F" required />
        <Input label="Document URL / Path" value={form.documentUrl} onChange={v=>sf('documentUrl',v)}
          placeholder="/docs/aadhaar.jpg" required />
        <Alert {...msg} />
        <Btn type="submit" disabled={loading}>{loading ? 'Uploading…' : '📤 Upload Document'}</Btn>
      </form>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

function ViewDocs({ msg, M }) {
  const [custId, setCustId] = useState('');
  const [docs,   setDocs]   = useState([]);

  const load = async () => {
    try {
      const { data } = await api.get(`/kyc/${custId}/documents`);
      setDocs(Array.isArray(data) ? data : []); M('success', `${data.length} documents`);
    } catch (e) { M('error', errMsg(e)); }
  };

  return (
    <Card title="View KYC Documents">
      <div className="flex gap-3 mb-4 max-w-sm">
        <Input label="Customer ID" type="number" value={custId} onChange={setCustId} />
        <div className="pt-5"><Btn onClick={load}>🔍 Fetch</Btn></div>
      </div>
      <Alert {...msg} />
      {docs.length > 0 && (
        <div className="overflow-x-auto mt-3">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50"><tr>{['ID','Type','Doc Number','Status','Rejection Reason','Uploaded'].map(h=><Th key={h}>{h}</Th>)}</tr></thead>
            <tbody className="divide-y divide-gray-100">
              {docs.map(d => (
                <tr key={d.id} className="hover:bg-gray-50">
                  <Td>{d.id}</Td>
                  <Td>{d.documentType}</Td>
                  <Td><code className="text-xs">{d.documentNumber}</code></Td>
                  <Td><span className={`px-2 py-0.5 rounded text-xs font-medium ${
                    d.status==='VERIFIED'?'bg-green-100 text-green-700':
                    d.status==='REJECTED'?'bg-red-100 text-red-700':'bg-yellow-100 text-yellow-700'}`}>{d.status}</span></Td>
                  <Td>{d.rejectionReason || '-'}</Td>
                  <Td>{d.uploadedAt ? new Date(d.uploadedAt).toLocaleDateString() : '-'}</Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function KycStatus({ msg, M }) {
  const [custId, setCustId] = useState('');
  const [status, setStatus] = useState(null);

  const load = async () => {
    try {
      const { data } = await api.get(`/kyc/${custId}/status`);
      setStatus(data); M('success', `KYC Status: ${data.kycStatus}`);
    } catch (e) { M('error', errMsg(e)); setStatus(null); }
  };

  const COLOR = { VERIFIED:'bg-green-100 text-green-700', REJECTED:'bg-red-100 text-red-700', UNDER_REVIEW:'bg-blue-100 text-blue-700', PENDING:'bg-yellow-100 text-yellow-700' };

  return (
    <Card title="KYC Status Check">
      <div className="flex gap-3 mb-4 max-w-sm">
        <Input label="Customer ID" type="number" value={custId} onChange={setCustId} />
        <div className="pt-5"><Btn onClick={load}>🔍 Check Status</Btn></div>
      </div>
      <Alert {...msg} />
      {status && (
        <div className="mt-3 p-4 bg-gray-50 rounded-xl border">
          <p className="text-sm text-gray-600">Customer ID: <strong>{status.customerId}</strong></p>
          <p className="mt-2 text-sm">KYC Status:
            <span className={`ml-2 px-3 py-1 rounded-full text-sm font-semibold ${COLOR[status.kycStatus] || 'bg-gray-100 text-gray-700'}`}>
              {status.kycStatus}
            </span>
          </p>
        </div>
      )}
    </Card>
  );
}

function AdminReview({ msg, M }) {
  const [docId,          setDocId]          = useState('');
  const [status,         setStatus]         = useState('VERIFIED');
  const [rejectionReason, setRejectionReason] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const review = async () => {
    setLoading(true); M('','');
    try {
      let url = `/kyc/documents/${docId}/review?status=${status}`;
      if (status === 'REJECTED' && rejectionReason) url += `&rejectionReason=${encodeURIComponent(rejectionReason)}`;
      const { data } = await api.put(url);
      setResult(data); M('success', `Document #${docId} marked as ${status}`);
    } catch (e) { M('error', errMsg(e)); }
    setLoading(false);
  };

  return (
    <Card title="Admin: Review KYC Document">
      <div className="space-y-3 max-w-sm">
        <Input label="Document ID" type="number" value={docId} onChange={setDocId} required />
        <Select label="Decision" value={status} onChange={setStatus} options={['VERIFIED','REJECTED','UNDER_REVIEW']} />
        {status === 'REJECTED' && (
          <Input label="Rejection Reason" value={rejectionReason} onChange={setRejectionReason} placeholder="e.g. Blurry image" />
        )}
        <Alert {...msg} />
        <Btn onClick={review} color={status==='VERIFIED'?'green':'red'} disabled={loading}>
          {loading ? 'Saving…' : status==='VERIFIED' ? '✅ Approve Document' : '❌ Reject Document'}
        </Btn>
      </div>
      {result && <JsonBox data={result} />}
    </Card>
  );
}

