export type CurrencyCode = "CZK" | "EUR";

export type Account = {
  id: string;
  ownerName: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  birthNumber?: string;
  email: string;
  address: string;
  accountNumber: string;
  balance: number;
  currency: CurrencyCode;
  type: "CURRENT" | "SAVINGS";
};

export type BankTransaction = {
  id: string;
  accountId: string;
  amount: number;
  type: "CREDIT" | "DEBIT";
  description: string;
  counterpartyAccountNumber?: string;
  variableSymbol?: string;
  specificSymbol?: string;
  createdAt: string;
};

export type StandingOrder = {
  id: string;
  accountId: string;
  targetAccountNumber: string;
  amount: number;
  description: string;
  variableSymbol?: string;
  specificSymbol?: string;
  dayOfMonth: number;
  active: boolean;
  createdAt: string;
};

export type PaymentTemplate = {
  id: string;
  accountId: string;
  name: string;
  targetAccountNumber: string;
  amount: number;
  description: string;
  variableSymbol?: string;
  specificSymbol?: string;
  createdAt: string;
};

export type BankCard = {
  id: string;
  accountId: string;
  holderName: string;
  cardType: string;
  lastFour: string;
  expirationDate: string;
  locked: boolean;
  paymentLimit: number;
  onlinePaymentLimit: number;
  withdrawalLimit: number;
  onlinePayments: boolean;
  inStorePayments: boolean;
  cashWithdrawals: boolean;
};

export type LoanApplication = {
  id: string;
  type: "PERSONAL" | "HOME" | "MORTGAGE";
  amount: number;
  repaymentMonths: number;
  annualRate: number;
  monthlyPayment: number;
  purpose: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
  repaymentAccountNumber?: string;
  variableSymbol?: string;
  specificSymbol?: string;
  repaymentDayOfMonth?: number;
  repaidAmount?: number;
  remainingAmount?: number;
  remainingInstallments?: number;
  earlyRepaymentAmount?: number;
  dueDate?: string;
};

export type InterestSettings = { savingsRate: number; overdraftRate: number; personalLoanRate: number; homeLoanRate: number; mortgageRate: number; mortgageMinimumEquityPercent: number };
export type OverdraftApplication = { id: string; category: "OVERDRAFT"; product: string; accountId: string; clientName: string; accountNumber: string; amount: number; monthlyIncome: number; annualRate?: number; status: "PENDING" | "APPROVED" | "REJECTED"; createdAt: string; decidedAt: string | null; decisionNote: string | null };

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/backend";
const managerBase = "/api/manager";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  return requestFrom<T>(apiBase, path, options);
}

async function requestFrom<T>(base: string, path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...options?.headers },
  });

  if (!response.ok) {
    const responseText = await response.text();
    let responseBody: { message?: string; detail?: string; error?: string } | undefined;
    try {
      responseBody = JSON.parse(responseText) as typeof responseBody;
    } catch {
      responseBody = undefined;
    }
    const message = responseBody?.message ?? responseBody?.detail
      ?? (response.status === 422 ? "Na účtu není dostatečný zůstatek." : undefined)
      ?? responseBody?.error
      ?? responseText
      ?? `Backend odpověděl stavem ${response.status}`;
    throw new Error(message);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function getAccounts() {
  return request<Account[]>("/accounts", { cache: "no-store" });
}

export function getInterestSettings() { return requestFrom<InterestSettings>(managerBase, "/settings/interest", { cache: "no-store" }); }
export function getOverdraftApplications() { return requestFrom<OverdraftApplication[]>(managerBase, "/overdrafts", { cache: "no-store" }); }
export function createOverdraftApplication(input: { accountId: string; requestedLimit: number; monthlyIncome: number }) {
  return requestFrom<OverdraftApplication>(managerBase, "/overdrafts", { method: "POST", body: JSON.stringify(input) });
}

export function terminateOverdraft(id: string) {
  return requestFrom<void>(managerBase, `/overdrafts/${id}`, { method: "DELETE" });
}

export function createAccount(input: {
  ownerName: string;
  email: string;
  address: string;
  password: string;
  accountNumber: string;
  initialBalance?: number;
  currency?: CurrencyCode;
}) {
  return request<Account>("/accounts", { method: "POST", body: JSON.stringify(input) });
}

export function createSavingsAccount(accountId: string) {
  return request<Account>(`/accounts/${accountId}/savings`, { method: "POST" });
}

export function login(input: { username: string; password: string }) {
  return request<Account>("/auth/login", { method: "POST", body: JSON.stringify(input) });
}

export function register(input: {
  username: string;
  firstName: string;
  lastName: string;
  birthNumber: string;
  email: string;
  street: string;
  city: string;
  postalCode: string;
  password: string;
}) {
  return request<Account>("/auth/register", { method: "POST", body: JSON.stringify(input) });
}

export function getLoanApplications(accountId: string) {
  return request<LoanApplication[]>(`/accounts/${accountId}/loan-applications`, { cache: "no-store" });
}

export function createLoanApplication(accountId: string, input: {
  type: LoanApplication["type"];
  amount: number;
  repaymentMonths: number;
  purpose: string;
}) {
  return request<LoanApplication>(`/accounts/${accountId}/loan-applications`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateAccount(accountId: string, input: { ownerName: string; email: string; address: string; password?: string }) {
  return request<Account>(`/accounts/${accountId}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export function getTransactions(accountId: string) {
  return request<BankTransaction[]>(`/accounts/${accountId}/transactions`, { cache: "no-store" });
}

export function getStandingOrders(accountId: string) {
  return request<StandingOrder[]>(`/accounts/${accountId}/standing-orders`, { cache: "no-store" });
}

export function createStandingOrder(accountId: string, input: Omit<StandingOrder, "id" | "accountId" | "active" | "createdAt">) {
  return request<StandingOrder>(`/accounts/${accountId}/standing-orders`, { method: "POST", body: JSON.stringify(input) });
}

export function updateStandingOrder(orderId: string, input: Omit<StandingOrder, "id" | "accountId" | "active" | "createdAt">) {
  return request<StandingOrder>(`/standing-orders/${orderId}`, { method: "PATCH", body: JSON.stringify(input) });
}

export function deleteStandingOrder(orderId: string) {
  return request<void>(`/standing-orders/${orderId}`, { method: "DELETE" });
}

export function getPaymentTemplates(accountId: string) {
  return request<PaymentTemplate[]>(`/accounts/${accountId}/payment-templates`, { cache: "no-store" });
}

export function createPaymentTemplate(accountId: string, input: Omit<PaymentTemplate, "id" | "accountId" | "createdAt">) {
  return request<PaymentTemplate>(`/accounts/${accountId}/payment-templates`, { method: "POST", body: JSON.stringify(input) });
}

export function updatePaymentTemplate(templateId: string, input: Omit<PaymentTemplate, "id" | "accountId" | "createdAt">) {
  return request<PaymentTemplate>(`/payment-templates/${templateId}`, { method: "PATCH", body: JSON.stringify(input) });
}

export function deletePaymentTemplate(templateId: string) {
  return request<void>(`/payment-templates/${templateId}`, { method: "DELETE" });
}

export function getCards(accountId: string) {
  return request<BankCard[]>(`/accounts/${accountId}/cards`, { cache: "no-store" });
}

export function createCard(accountId: string) {
  return request<BankCard>(`/accounts/${accountId}/cards`, { method: "POST" });
}

export function updateCard(cardId: string, input: Omit<BankCard, "id" | "accountId" | "holderName" | "cardType" | "lastFour" | "expirationDate">) {
  return request<BankCard>(`/cards/${cardId}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export function transferMoney(input: {
  fromAccountId: string;
  toAccountNumber: string;
  amount: number;
  description: string;
  variableSymbol?: string;
  specificSymbol?: string;
}) {
  return request<void>("/transfers", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
