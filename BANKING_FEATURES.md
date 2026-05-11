# Real-World Banking Features — Verification Guide

> Base URL (NodePort): `http://<VM_IP>:30080`  
> All requests route through the **API Gateway**.

---

## Table of Contents
1. [Account Management](#1-account-management)
2. [Account Freeze / Unfreeze](#2-account-freeze--unfreeze)
3. [Daily Transaction Limits](#3-daily-transaction-limits)
4. [TPIN (4-digit PIN)](#4-tpin-4-digit-pin)
5. [KYC — Document Upload & Verification](#5-kyc--document-upload--verification)
6. [OTP Generation & Verification](#6-otp-generation--verification)
7. [Loan Management](#7-loan-management)
8. [Transaction — NEFT / RTGS / IMPS](#8-transaction--neft--rtgs--imps)
9. [Transaction — UPI](#9-transaction--upi)
10. [Fraud Detection](#10-fraud-detection)
11. [Account Statement & Summary](#11-account-statement--summary)
12. [Daily Interest (Scheduled)](#12-daily-interest-scheduled)
13. [Loan EMI Overdue (Scheduled)](#13-loan-emi-overdue-scheduled)

---

## 1. Account Management

### Create SAVINGS Account (with min-balance ₹500 auto-applied)
```
POST /api/v1/accounts
{
  "accountType": "SAVINGS",
  "customerId": 1
}
```
**Expected:** `201 Created`, account with `status=ACTIVE`, `dailyTxnLimit=100000`, `minBalance=500`, `interestRate=3.50`

### Create CURRENT Account
```
POST /api/v1/accounts
{ "accountType": "CURRENT", "customerId": 2 }
```
**Expected:** `dailyTxnLimit=1000000`, `minBalance=0`, `interestRate=0`

### Create FIXED_DEPOSIT Account
```
POST /api/v1/accounts
{ "accountType": "FIXED_DEPOSIT", "customerId": 3, "maturityDate": "2027-05-11", "nomineeName": "Jane Doe" }
```
**Expected:** `interestRate=7.00`, `maturityDate` set

### Get Account (verify all new fields)
```
GET /api/v1/accounts/account/{accountNumber}
```
**Verify fields present:** `status`, `dailyTxnLimit`, `usedDailyAmount`, `minBalance`, `interestRate`, `ifscCode`, `branchCode`

### Close Account — Balance Must Be Zero
```
DELETE /api/v1/accounts/{accountNumber}
```
**Expected (non-zero balance):** `422` — *"Cannot close account with non-zero balance"*  
**Expected (zero balance):** `200` — account closed

---

## 2. Account Freeze / Unfreeze

### Freeze Account
```
PATCH /api/v1/accounts/{accountNumber}/freeze?reason=Suspicious+activity
```
**Expected:** `200`, `status=FROZEN`

### Try Transacting on Frozen Account
```
POST /api/v1/transactions/debit
{ "accountNumber": "<frozen-acc>", "amount": 100 }
```
**Expected:** `422` — *"Account is not active. Status: FROZEN"*

### Unfreeze Account
```
PATCH /api/v1/accounts/{accountNumber}/unfreeze
```
**Expected:** `200`, `status=ACTIVE`

### Try to Unfreeze an Already Active Account
```
PATCH /api/v1/accounts/{accountNumber}/unfreeze
```
**Expected:** `422` — *"Account is not frozen. Current status: ACTIVE"*

---

## 3. Daily Transaction Limits

### Update Daily Limit
```
PATCH /api/v1/accounts/{accountNumber}/limits?dailyLimit=5000
```
**Expected:** `200`, `dailyTxnLimit=5000`

### Exceed Daily Limit
```
POST /api/v1/transactions/debit
{ "accountNumber": "<acc>", "amount": 6000 }
```
**Expected:** `422` — *"Daily transaction limit exceeded. Remaining today: ₹5000, Requested: ₹6000"*

### Breach Minimum Balance (SAVINGS)
```
POST /api/v1/transactions/debit
{ "accountNumber": "<savings-acc>", "amount": <balance - 400> }
```
**Expected:** `422` — *"Transaction would breach minimum balance of ₹500"*

---

## 4. TPIN (4-digit PIN)

### Set PIN
```
POST /api/v1/accounts/{accountNumber}/pin/set?pin=1234
```
**Expected:** `200`, `{ "message": "PIN set successfully" }`

### Verify Correct PIN
```
POST /api/v1/accounts/{accountNumber}/pin/verify?pin=1234
```
**Expected:** `200`, `{ "valid": true }`

### Verify Wrong PIN
```
POST /api/v1/accounts/{accountNumber}/pin/verify?pin=9999
```
**Expected:** `422` — *"Invalid PIN"*

### Set PIN Before It's Configured
```
POST /api/v1/accounts/{accountNumber}/pin/verify?pin=0000
```
**Expected:** `422` — *"No PIN set for this account"*

### Set Non-4-Digit PIN
```
POST /api/v1/accounts/{accountNumber}/pin/set?pin=12
```
**Expected:** `422` — *"PIN must be exactly 4 digits"*

---

## 5. KYC — Document Upload & Verification

### Upload Aadhaar Document
```
POST /api/v1/kyc/documents/upload
{
  "customerId": 1,
  "documentType": "AADHAAR",
  "documentNumber": "1234-5678-9012",
  "documentUrl": "/docs/aadhaar-1.jpg"
}
```
**Expected:** `201`, `status=UPLOADED`  
**Side effect:** Customer `kycStatus` moves to `UNDER_REVIEW`

### Upload PAN Document
```
POST /api/v1/kyc/documents/upload
{
  "customerId": 1,
  "documentType": "PAN",
  "documentNumber": "ABCDE1234F",
  "documentUrl": "/docs/pan-1.jpg"
}
```

### Check KYC Status (should be UNDER_REVIEW)
```
GET /api/v1/kyc/1/status
```
**Expected:** `{ "kycStatus": "UNDER_REVIEW" }`

### Admin: Verify Aadhaar Document
```
PUT /api/v1/kyc/documents/1/review?status=VERIFIED
```

### Admin: Verify PAN Document
```
PUT /api/v1/kyc/documents/2/review?status=VERIFIED
```
**Side effect:** Once 2+ documents are VERIFIED → customer `kycStatus=VERIFIED`

### Check Final KYC Status
```
GET /api/v1/kyc/1/status
```
**Expected:** `{ "kycStatus": "VERIFIED" }`

### Admin: Reject a Document
```
PUT /api/v1/kyc/documents/3/review?status=REJECTED&rejectionReason=Blurry+image
```
**Expected:** `200`, `rejectionReason` populated

### Get All Documents for Customer
```
GET /api/v1/kyc/1/documents
```
**Expected:** Array of all KYC documents with their statuses

---

## 6. OTP Generation & Verification

### Generate OTP (TRANSACTION_AUTH)
```
POST /api/v1/otp/generate
{ "customerId": 1, "purpose": "TRANSACTION_AUTH" }
```
**Expected:** `200`, returns OTP in response (dev only) and sends email notification

### Verify Correct OTP
```
POST /api/v1/otp/verify
{ "customerId": 1, "otp": "<otp-from-above>", "purpose": "TRANSACTION_AUTH" }
```
**Expected:** `200`, `{ "valid": true }`

### Verify Expired / Used OTP
```
POST /api/v1/otp/verify
{ "customerId": 1, "otp": "<same-otp>", "purpose": "TRANSACTION_AUTH" }
```
**Expected:** `422` — *"No valid OTP found"*

### Exceed Max OTP Attempts (generate fresh, try 3 wrong values)
```
POST /api/v1/otp/verify  { "otp": "000000" }   # attempt 1
POST /api/v1/otp/verify  { "otp": "000000" }   # attempt 2
POST /api/v1/otp/verify  { "otp": "000000" }   # attempt 3 — locked
```
**Expected on attempt 4:** `422` — *"Maximum OTP attempts exceeded"*

### Generate OTP for TPIN Reset
```
POST /api/v1/otp/generate
{ "customerId": 1, "purpose": "TPIN_RESET" }
```

---

## 7. Loan Management

### Apply for Personal Loan
```
POST /api/v1/loans/apply
{
  "customerId": 1,
  "principalAmount": 500000,
  "tenureMonths": 60,
  "loanType": "PERSONAL",
  "linkedSavingsAccount": "<savings-acc-number>",
  "nomineeName": "Jane Doe"
}
```
**Expected:** `201`, returns `LoanAccount` with:
- `emiAmount` (auto-computed via annuity formula)
- `firstEmiDate` (next month)
- `status=ACTIVE`
- Savings account balance **increased by ₹5,00,000** (disbursement)

**Interest rate applied:** `PERSONAL=12%`, `HOME=8.5%`, `AUTO=9.75%`, `EDUCATION=7.5%`

### Get EMI Repayment Schedule
```
GET /api/v1/loans/{loanId}/schedule
```
**Expected:** 60 rows with `principalComponent`, `interestComponent`, `totalPaid`, `dueDate`

### Get All Loans for Customer
```
GET /api/v1/loans/customer/1
```

### Pay EMI
```
POST /api/v1/loans/{loanId}/pay-emi
```
**Expected:**
- EMI record `status=PAID`, `paidDate` set
- `emisPaid` incremented
- Savings account balance reduced by EMI amount
- If last EMI: `loanStatus=CLOSED`

### Pay Overdue EMI (penalty applies)
> If `dueDate < today` when paying, 2% penalty is charged.

### Apply for Loan When NPA Exists
```
POST /api/v1/loans/apply  (with an existing NPA loan)
```
**Expected:** `422` — *"Cannot apply for a new loan: existing NPA loan found"*

### Apply Below RTGS Minimum (wrong field — verify loan amount validation)
```
POST /api/v1/loans/apply  { "principalAmount": 5000 }
```
**Expected:** `400` — *"Minimum loan amount is 10,000"*

### View Overdue Loans (admin)
```
GET /api/v1/loans/overdue
```

---

## 8. Transaction — NEFT / RTGS / IMPS

### NEFT Transfer (free)
```
POST /api/v1/transactions/neft
{
  "fromAccount": "<acc-number>",
  "beneficiaryAccountNumber": "9876543210",
  "beneficiaryName": "John Doe",
  "ifscCode": "HDFC0001234",
  "bankName": "HDFC Bank",
  "amount": 5000,
  "channel": "NEFT",
  "remarks": "Rent payment"
}
```
**Expected:** `201`, `referenceNumber=TXN-...`, `processingFee=0.00`, `channel=NEFT`

### RTGS Transfer (min ₹2,00,000 required; ₹24 flat fee)
```
POST /api/v1/transactions/neft
{
  "fromAccount": "<acc>",
  "beneficiaryAccountNumber": "1111222233334444",
  "beneficiaryName": "Corp Ltd",
  "ifscCode": "ICIC0001111",
  "bankName": "ICICI Bank",
  "amount": 250000,
  "channel": "RTGS",
  "remarks": "Vendor payment"
}
```
**Expected:** `processingFee=24.00`

### RTGS Below Minimum Amount
```
POST /api/v1/transactions/neft
{ "amount": 50000, "channel": "RTGS", ... }
```
**Expected:** `422` — *"RTGS minimum transfer amount is ₹2,00,000"*

### IMPS Transfer (0.5% fee, max ₹15)
```
POST /api/v1/transactions/neft
{ "amount": 10000, "channel": "IMPS", ... }
```
**Expected:** `processingFee=15.00` (0.5% of 10000 = 50, capped at 15)

### Verify Reference Number
```
GET /api/v1/transactions/ref/{referenceNumber}
```
**Expected:** Full transaction detail with `channel`, `status`, `beneficiaryName`, `processingFee`

---

## 9. Transaction — UPI

### UPI Payment
```
POST /api/v1/transactions/upi
{
  "fromAccount": "<acc-number>",
  "upiId": "john@upi",
  "amount": 250,
  "remarks": "Splitting dinner"
}
```
**Expected:** `201`, `channel=UPI`, `processingFee=0.00`, `referenceNumber=TXN-...`

### Invalid UPI ID Format
```
POST /api/v1/transactions/upi
{ "upiId": "invalid-id-no-at", "amount": 100, ... }
```
**Expected:** `400` — *"Invalid UPI ID format"*

---

## 10. Fraud Detection

### Trigger Velocity Rule (5+ transactions in 10 minutes)
> Make 6+ debit/transfer requests on same account within 10 minutes.

```
# Repeat 6 times:
POST /api/v1/transactions/debit
{ "accountNumber": "<acc>", "amount": 100 }
```
**Expected on 6th+ transaction:**  
- Transaction saved with `fraudFlag=true`
- `fraudReason` contains *"VELOCITY: 6 transactions in the last 10 minutes"*
- `fraudSeverity=LOW` or `MEDIUM`

### Trigger Large Amount Rule (> ₹5,00,000)
```
POST /api/v1/transactions/credit  { "amount": 600000 }
POST /api/v1/transactions/debit   { "amount": 600000 }
```
**Expected:** `fraudFlag=true`, `fraudReason` contains *"LARGE_AMOUNT"*

### Trigger Round Amount Rule (multiple of ₹10,000)
```
POST /api/v1/transactions/debit { "amount": 50000 }
```
**Expected:** `fraudFlag=true`, `fraudReason` contains *"ROUND_AMOUNT"*

### View All Fraud-Flagged Transactions
```
GET /api/v1/transactions/fraud/flagged?page=0&size=20
```
**Expected:** Paginated list of all fraud-flagged transactions

---

## 11. Account Statement & Summary

### Get Statement for Date Range
```
GET /api/v1/transactions/account/{accountNumber}/statement
    ?from=2026-05-01T00:00:00
    &to=2026-05-31T23:59:59
```
**Expected:** Ordered list of all transactions with `referenceNumber`, `channel`, `status`, `processingFee`

### Get Summary (totals)
```
GET /api/v1/transactions/account/{accountNumber}/summary
    ?from=2026-05-01T00:00:00
    &to=2026-05-31T23:59:59
```
**Expected:**
```json
{
  "accountNumber": "ACC-123",
  "fromDate": "2026-05-01",
  "toDate": "2026-05-31",
  "totalCredits": 10000.00,
  "totalDebits": 3500.00,
  "netAmount": 6500.00,
  "transactionCount": 8
}
```

---

## 12. Daily Interest (Scheduled)

The interest job runs at **00:05 daily** (cron: `0 5 0 * * ?`).

**Manual verification flow:**
1. Create a SAVINGS account, credit ₹100,000
2. Note the balance
3. After midnight (or trigger manually via actuator if you expose the endpoint), check balance
4. Expected daily addition: `100000 × 3.5 / 365 = ₹9.58`

**Formula:** `balance × (interestRate / 36500)` per day

---

## 13. Loan EMI Overdue (Scheduled)

The overdue check runs at **00:30 daily** (cron: `0 30 0 * * ?`).

**Manual verification:**
1. Apply for a loan
2. Let the `nextEmiDate` pass (or set it in DB to yesterday for testing)
3. Next morning, check loan status

```
GET /api/v1/loans/overdue
```
**Expected:** Loans where `nextEmiDate < today` AND `status=OVERDUE`

---

## New API Endpoints Summary

### AccountService (`/api/v1/accounts`)
| Method | Path | Description |
|--------|------|-------------|
| `PATCH` | `/{accountNumber}/freeze` | Freeze account |
| `PATCH` | `/{accountNumber}/unfreeze` | Unfreeze account |
| `PATCH` | `/{accountNumber}/limits` | Update daily transaction limit |
| `POST`  | `/{accountNumber}/pin/set` | Set 4-digit TPIN |
| `POST`  | `/{accountNumber}/pin/verify` | Verify TPIN |

### LoanService (`/api/v1/loans`)
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/apply` | Apply for loan |
| `GET`  | `/customer/{customerId}` | Get all loans for customer |
| `GET`  | `/{loanId}/schedule` | EMI repayment schedule |
| `POST` | `/{loanId}/pay-emi` | Pay next EMI |
| `GET`  | `/overdue` | List overdue loans (admin) |

### KYC (`/api/v1/kyc`)
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/documents/upload` | Upload KYC document |
| `GET`  | `/{customerId}/status` | Get KYC status |
| `GET`  | `/{customerId}/documents` | List all documents |
| `PUT`  | `/documents/{docId}/review` | Approve/Reject document (admin) |

### OTP (`/api/v1/otp`)
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/generate` | Generate OTP |
| `POST` | `/verify` | Verify OTP |

### TransactionService (`/api/v1/transactions`)
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/neft` | NEFT / RTGS / IMPS transfer |
| `POST` | `/upi` | UPI payment |
| `GET`  | `/ref/{referenceNumber}` | Lookup by reference number |
| `GET`  | `/fraud/flagged` | Fraud-flagged transactions (admin) |
| `GET`  | `/account/{no}/statement` | Date-range statement |
| `GET`  | `/account/{no}/summary` | Credit/Debit summary |

---

## Business Rules Quick Reference

| Rule | Value |
|------|-------|
| SAVINGS minimum balance | ₹500 |
| SAVINGS daily limit | ₹1,00,000 |
| CURRENT daily limit | ₹10,00,000 |
| SAVINGS interest rate | 3.5% p.a. |
| FD interest rate | 7.0% p.a. |
| RTGS minimum | ₹2,00,000 |
| RTGS fee | ₹24 flat |
| IMPS fee | 0.5%, max ₹15 |
| NEFT fee | Free |
| UPI fee | Free |
| Fraud velocity window | 5 txns in 10 min |
| Fraud large amount | > ₹5,00,000 |
| Fraud off-hours | 11 PM – 5 AM + amount > ₹10,000 |
| Fraud round amount | Exact multiple of ₹10,000 |
| EMI overdue penalty | 2% per overdue EMI |
| OTP validity | 5 minutes |
| OTP max attempts | 3 |
| Loan min amount | ₹10,000 |
| Loan max amount | ₹1,00,00,000 |
| Personal loan rate | 12% p.a. |
| Home loan rate | 8.5% p.a. |
| Auto loan rate | 9.75% p.a. |
| Education loan rate | 7.5% p.a. |

