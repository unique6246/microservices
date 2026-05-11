import { useState, useEffect } from 'react';
import api from '../api/client.js';
import { Input, Select, Alert, Btn, Card, Table, Th, Td, errMsg } from '../components/ui.jsx';

const BLANK = { firstName:'', lastName:'', gender:'MALE', address:'', phoneNumber:'', email:'' };

export default function Customers() {
  const [form,      setForm]      = useState(BLANK);
  const [customers, setCustomers] = useState([]);
  const [loading,   setLoading]   = useState(false);
  const [msg,       setMsg]       = useState({ type:'', text:'' });

  const sf = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const load = async () => {
    try {
      const { data } = await api.get('/customers?size=100');
      setCustomers(data.content ?? (Array.isArray(data) ? data : []));
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
  };

  useEffect(() => { load(); }, []);

  const onCreate = async (e) => {
    e.preventDefault(); setLoading(true); setMsg({});
    try {
      await api.post('/customers', form);
      setMsg({ type:'success', text:'Customer registered successfully!' });
      setForm(BLANK); load();
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
    setLoading(false);
  };

  const onDelete = async (id) => {
    if (!confirm(`Delete customer #${id}?`)) return;
    try {
      await api.delete(`/customers/${id}`);
      setMsg({ type:'success', text:`Customer #${id} deleted.` }); load();
    } catch (e) { setMsg({ type:'error', text: errMsg(e) }); }
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">👥 Customer Management</h1>
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">

        {/* ── Create ── */}
        <Card title="Register New Customer">
          <form onSubmit={onCreate} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <Input label="First Name" value={form.firstName} onChange={v => sf('firstName', v)} required />
              <Input label="Last Name"  value={form.lastName}  onChange={v => sf('lastName', v)}  required />
            </div>
            <Select label="Gender" value={form.gender} onChange={v => sf('gender', v)}
              options={['MALE','FEMALE','OTHER']} />
            <Input label="Address"      value={form.address}     onChange={v => sf('address', v)}     required />
            <Input label="Phone Number" value={form.phoneNumber} onChange={v => sf('phoneNumber', v)} placeholder="+919876543210" required />
            <Input label="Email"        type="email" value={form.email} onChange={v => sf('email', v)} required />
            <Alert {...msg} />
            <Btn type="submit" disabled={loading} full>
              {loading ? 'Creating…' : '➕ Create Customer'}
            </Btn>
          </form>
        </Card>

        {/* ── List ── */}
        <Card title="All Customers"
          action={<button onClick={load} className="text-xs text-blue-600 hover:underline">🔄 Refresh</button>}>
          <div className="overflow-auto max-h-[480px]">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>{['ID','Name','Email','Phone','Gender',''].map(h => <Th key={h}>{h}</Th>)}</tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {customers.map(c => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <Td>{c.id}</Td>
                    <Td>{c.firstName} {c.lastName}</Td>
                    <Td>{c.email}</Td>
                    <Td>{c.phoneNumber}</Td>
                    <Td>{c.gender}</Td>
                    <Td>
                      <button onClick={() => onDelete(c.id)}
                        className="text-xs text-red-500 hover:text-red-700 font-medium">🗑 Delete</button>
                    </Td>
                  </tr>
                ))}
                {!customers.length && (
                  <tr><td colSpan={6} className="text-center py-8 text-gray-400">No customers yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
}

