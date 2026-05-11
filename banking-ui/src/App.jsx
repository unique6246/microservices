import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout     from './components/Layout.jsx';
import Login      from './pages/Login.jsx';
import Dashboard  from './pages/Dashboard.jsx';
import Customers  from './pages/Customers.jsx';
import Accounts   from './pages/Accounts.jsx';
import Transactions from './pages/Transactions.jsx';
import Loans      from './pages/Loans.jsx';
import Kyc        from './pages/Kyc.jsx';
import Otp        from './pages/Otp.jsx';
import Fraud      from './pages/Fraud.jsx';
import Statement  from './pages/Statement.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<Layout />}>
          <Route path="/"             element={<Dashboard />} />
          <Route path="/customers"    element={<Customers />} />
          <Route path="/accounts"     element={<Accounts />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/loans"        element={<Loans />} />
          <Route path="/kyc"          element={<Kyc />} />
          <Route path="/otp"          element={<Otp />} />
          <Route path="/fraud"        element={<Fraud />} />
          <Route path="/statement"    element={<Statement />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

