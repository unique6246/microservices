import { useState } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, JsonBox, errMsg } from '../components/ui.jsx';

const PURPOSES = ['TRANSACTION_AUTH','TPIN_RESET','LOGIN','PASSWORD_RESET'];

export default function Otp() {
  const [genForm, setGenForm] = useState({ customerId:'', purpose:'TRANSACTION_AUTH' });
  const [verForm, setVerForm] = useState({ customerId:'', otp:'', purpose:'TRANSACTION_AUTH' });
  const [genResult, setGenResult] = useState(null);
  const [verResult, setVerResult] = useState(null);
  const [msg, setMsg] = useState({ type:'', text:'' });
  const [loading, setLoading] = useState('');

  const generate = async (e) => {
    e.preventDefault(); setLoading('gen'); setMsg({});
    try {
      const { data } = await api.post('/otp/generate', {
        customerId: Number(genForm.customerId), purpose: genForm.purpose
      });
      setGenResult(data);
      setMsg({ type:'success', text:`OTP generated! (Check response for dev testing)` });
      // Pre-fill verify form
      setVerForm(v => ({ ...v, customerId: genForm.customerId, purpose: genForm.purpose, otp: data.otp || '' }));
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading('');
  };

  const verify = async (e) => {
    e.preventDefault(); setLoading('ver'); setMsg({});
    try {
      const { data } = await api.post('/otp/verify', {
        customerId: Number(verForm.customerId), otp: verForm.otp, purpose: verForm.purpose
      });
      setVerResult(data);
      setMsg({ type: data.valid ? 'success' : 'error', text: data.valid ? '✅ OTP is valid!' : '❌ OTP is invalid or expired' });
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading('');
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">🔐 OTP Management</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

        {/* ── Generate ── */}
        <Card title="Generate OTP">
          <form onSubmit={generate} className="space-y-3">
            <Input label="Customer ID" type="number" value={genForm.customerId}
              onChange={v => setGenForm(f=>({...f,customerId:v}))} required />
            <Select label="Purpose" value={genForm.purpose}
              onChange={v => setGenForm(f=>({...f,purpose:v}))} options={PURPOSES} />
            <Btn type="submit" disabled={loading==='gen'} full>
              {loading==='gen' ? 'Generating…' : '📨 Generate OTP'}
            </Btn>
          </form>
          {genResult && (
            <div className="mt-4">
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-center">
                <p className="text-xs text-yellow-700 font-medium mb-1">OTP (dev mode only)</p>
                <p className="text-3xl font-mono font-bold text-yellow-800 tracking-widest">{genResult.otp}</p>
              </div>
              <JsonBox data={genResult} />
            </div>
          )}
        </Card>

        {/* ── Verify ── */}
        <Card title="Verify OTP">
          <form onSubmit={verify} className="space-y-3">
            <Input label="Customer ID" type="number" value={verForm.customerId}
              onChange={v => setVerForm(f=>({...f,customerId:v}))} required />
            <Select label="Purpose" value={verForm.purpose}
              onChange={v => setVerForm(f=>({...f,purpose:v}))} options={PURPOSES} />
            <Input label="OTP Code" value={verForm.otp}
              onChange={v => setVerForm(f=>({...f,otp:v}))} placeholder="6-digit OTP" required />
            <Btn type="submit" color="teal" disabled={loading==='ver'} full>
              {loading==='ver' ? 'Verifying…' : '✔ Verify OTP'}
            </Btn>
          </form>
          {verResult && <JsonBox data={verResult} />}
        </Card>
      </div>

      <Alert {...msg} />

      <div className="mt-4 bg-blue-50 border border-blue-200 rounded-xl p-4 text-sm text-blue-800">
        <p className="font-medium mb-1">📌 OTP Usage</p>
        <ul className="list-disc list-inside space-y-1 text-xs">
          <li><strong>TRANSACTION_AUTH</strong> — Authorize high-value transactions</li>
          <li><strong>TPIN_RESET</strong> — Reset your 4-digit TPIN</li>
          <li>OTPs are valid for a limited time and can only be used once</li>
          <li>In dev mode the OTP is returned in the response (remove in production)</li>
        </ul>
      </div>
    </div>
  );
}

