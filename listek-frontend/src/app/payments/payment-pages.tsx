"use client";

import {
  ArrowRight,
  FileText,
  Trash2,
  X,
} from "lucide-react";
import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import PaymentsPage from "./page";
import {
  Account,
  BankTransaction,
  createPaymentTemplate,
  createStandingOrder,
  deletePaymentTemplate,
  deleteStandingOrder,
  getAccounts,
  getPaymentTemplates,
  getStandingOrders,
  getTransactions,
  PaymentTemplate,
  StandingOrder,
  updatePaymentTemplate,
  updateStandingOrder,
} from "../../lib/api";

export function formatAccountNumber(accountNumber: string) {
  const normalized = accountNumber.trim().replace(/\s*\/\s*/, " / ");
  const parts = normalized.split(" / ");
  return parts.length === 2 ? `${parts[0]} / ${parts[1]}` : normalized;
}

function sortTransactions(transactions: BankTransaction[], accounts: Account[]) {
  const ownAccountNumbers = new Set(accounts.map((account) => account.accountNumber.trim()));
  return [...transactions].sort((first, second) => {
    const sameInternalTransfer =
      first.description === second.description &&
      Math.abs(first.amount) === Math.abs(second.amount) &&
      first.type !== second.type &&
      ownAccountNumbers.has(first.counterpartyAccountNumber?.trim() ?? "") &&
      ownAccountNumbers.has(second.counterpartyAccountNumber?.trim() ?? "");

    if (sameInternalTransfer) return first.type === "DEBIT" ? -1 : 1;
    return new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime();
  });
}

function PageFrame({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  return <><PaymentsPage /><div className="modal-backdrop payment-page-backdrop"><div className="standalone-page"><button className="modal-close standalone-close" type="button" onClick={() => router.push("/payments")} aria-label="Zavřít"><X size={21} /></button>{children}</div></div></>;
}

function useAccounts() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    getAccounts().then(setAccounts).catch(() => setError("Backend není dostupný."));
  }, []);

  return { accounts, error, setError };
}

function useAutomation() {
  const [standingOrders, setStandingOrders] = useState<StandingOrder[]>([]);
  const [templates, setTemplates] = useState<PaymentTemplate[]>([]);
  const [error, setError] = useState("");

  async function refresh() {
    try {
      const accounts = await getAccounts();
      const [orders, loadedTemplates] = await Promise.all([
        Promise.all(accounts.map((account) => getStandingOrders(account.id))),
        Promise.all(accounts.map((account) => getPaymentTemplates(account.id))),
      ]);
      setStandingOrders(orders.flat());
      setTemplates(loadedTemplates.flat());
    } catch {
      setError("Backend není dostupný.");
    }
  }

  useEffect(() => {
    void (async () => {
      try {
        const accounts = await getAccounts();
        const [orders, loadedTemplates] = await Promise.all([
          Promise.all(accounts.map((account) => getStandingOrders(account.id))),
          Promise.all(accounts.map((account) => getPaymentTemplates(account.id))),
        ]);
        setStandingOrders(orders.flat());
        setTemplates(loadedTemplates.flat());
      } catch {
        setError("Backend není dostupný.");
      }
    })();
  }, []);

  return { standingOrders, templates, error, setError, refresh };
}

function FormError({ message }: { message: string }) {
  return message ? <p className="api-notice">{message}</p> : null;
}

export function StandingOrderFormPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get("edit");
  const { accounts, error: accountsError } = useAccounts();
  const { standingOrders, error: automationError } = useAutomation();
  const [error, setError] = useState("");
  const editingOrder = standingOrders.find((order) => order.id === editId);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const accountId = String(form.get("accountId"));
    const input = {
      targetAccountNumber: String(form.get("targetAccountNumber")).trim(),
      amount: Number(form.get("amount")),
      description: String(form.get("description")).trim(),
      variableSymbol: String(form.get("variableSymbol") || "").trim() || undefined,
      specificSymbol: String(form.get("specificSymbol") || "").trim() || undefined,
      dayOfMonth: Number(form.get("dayOfMonth")),
    };
    try {
      if (editingOrder) await updateStandingOrder(editingOrder.id, input);
      else await createStandingOrder(accountId, input);
      router.push("/payments/standing-orders");
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Trvalý příkaz se nepodařilo uložit.");
    }
  }

  return <PageFrame><section className="payment-form-card"><p className="modal-kicker">Pravidelná platba</p><h1>{editingOrder ? "Upravit trvalý příkaz" : "Nový trvalý příkaz"}</h1><FormError message={error || accountsError || automationError} /><form onSubmit={submit}>
    <label>Odesílatel<select required name="accountId" defaultValue={editingOrder?.accountId ?? accounts[0]?.id ?? ""}>{accounts.map((account) => <option key={account.id} value={account.id}>{account.type === "SAVINGS" ? "Spořicí účet" : "Běžný účet"} · {formatAccountNumber(account.accountNumber)}</option>)}</select></label>
    <label>Cílový účet<input required name="targetAccountNumber" placeholder="123456789 / 0100" defaultValue={editingOrder?.targetAccountNumber} /></label>
    <div className="payment-fields"><label>Částka<input required name="amount" type="number" min="0.01" step="0.01" placeholder="0,00" defaultValue={editingOrder?.amount} /></label><label>Den v měsíci<input required name="dayOfMonth" type="number" min="1" max="28" defaultValue={editingOrder?.dayOfMonth ?? 1} /></label></div>
    <label>Zpráva pro příjemce<input required name="description" placeholder="Například nájem" defaultValue={editingOrder?.description} /></label>
    <div className="payment-fields"><label>Variabilní symbol<input name="variableSymbol" inputMode="numeric" pattern="[0-9]{1,10}" maxLength={10} defaultValue={editingOrder?.variableSymbol ?? ""} /></label><label>Specifický symbol<input name="specificSymbol" inputMode="numeric" pattern="[0-9]{1,10}" maxLength={10} defaultValue={editingOrder?.specificSymbol ?? ""} /></label></div>
    <button className="pay-button payment-submit" type="submit">{editingOrder ? "Uložit změny" : "Uložit příkaz"} <ArrowRight size={18} /></button>
  </form></section></PageFrame>;
}

export function StandingOrdersPage() {
  const { standingOrders, error, setError, refresh } = useAutomation();

  async function remove(orderId: string) {
    if (!window.confirm("Opravdu chcete smazat tento trvalý příkaz?")) return;
    try {
      await deleteStandingOrder(orderId);
      await refresh();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Trvalý příkaz se nepodařilo smazat.");
    }
  }

  return <PageFrame><section className="payment-form-card"><div className="section-heading"><div><p className="modal-kicker">Pravidelné platby</p><h1>Trvalé příkazy</h1></div><Link className="pay-button" href="/payments/standing-orders/new">Nový příkaz <ArrowRight size={18} /></Link></div><FormError message={error} /><div className="automation-list">{standingOrders.map((order) => <div key={order.id} className="automation-item"><span>{formatAccountNumber(order.targetAccountNumber)} · {order.amount.toLocaleString("cs-CZ", { style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol" })}</span><small>{order.description}, každý měsíc {order.dayOfMonth}. den<br />Účet: {formatAccountNumber(order.targetAccountNumber)} · VS {order.variableSymbol || "neuveden"} · SS {order.specificSymbol || "neuveden"}</small><div className="automation-actions"><Link href={`/payments/standing-orders/new?edit=${order.id}`}><FileText size={15} /> Upravit</Link><button type="button" onClick={() => remove(order.id)}><Trash2 size={15} /> Smazat</button></div></div>)}{standingOrders.length === 0 && <p className="bank-empty">Zatím nemáte uložené trvalé příkazy.</p>}</div></section></PageFrame>;
}

function useTransactions() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<BankTransaction[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    getAccounts().then(async (loadedAccounts) => {
      setAccounts(loadedAccounts);
      const loadedTransactions = await Promise.all(loadedAccounts.map((account) => getTransactions(account.id)));
      setTransactions(sortTransactions(loadedTransactions.flat(), loadedAccounts));
    }).catch(() => setError("Platby se nepodařilo načíst."));
  }, []);

  return { accounts, transactions, error };
}

export function RepeatPaymentPage() {
  const { accounts, transactions, error } = useTransactions();

  function hrefFor(transaction: BankTransaction) {
    const params = new URLSearchParams({ prefill: "1", fromAccountId: transaction.accountId, toAccountNumber: transaction.counterpartyAccountNumber ?? "", amount: String(Math.abs(transaction.amount)), description: transaction.description, variableSymbol: transaction.variableSymbol ?? "", specificSymbol: transaction.specificSymbol ?? "" });
    return `/payments?${params.toString()}`;
  }

  return <PageFrame><section className="payment-form-card"><p className="modal-kicker">Opakovaná platba</p><h1>Vyberte předchozí platbu</h1><FormError message={error} /><div className="automation-list selectable-list">{transactions.filter((transaction) => transaction.type === "DEBIT").map((transaction) => <Link key={transaction.id} href={hrefFor(transaction)}><span>{transaction.description}</span><small>{transaction.counterpartyAccountNumber ? formatAccountNumber(transaction.counterpartyAccountNumber) : "Doplňte cílový účet"} · {Math.abs(transaction.amount).toLocaleString("cs-CZ", { style: "currency", currency: accounts.find((account) => account.id === transaction.accountId)?.currency ?? "CZK", currencyDisplay: "narrowSymbol" })}</small></Link>)}{transactions.filter((transaction) => transaction.type === "DEBIT").length === 0 && <p className="bank-empty">Zatím nemáte žádnou odchozí platbu k opakování.</p>}</div></section></PageFrame>;
}

export function TemplateFormPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const editId = searchParams.get("edit");
  const { accounts, error: accountsError } = useAccounts();
  const { templates, error: automationError } = useAutomation();
  const [error, setError] = useState("");
  const editingTemplate = templates.find((template) => template.id === editId);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const accountId = String(form.get("accountId"));
    const input = { name: String(form.get("name")).trim(), targetAccountNumber: String(form.get("targetAccountNumber")).trim(), amount: Number(form.get("amount")), description: String(form.get("description")).trim(), variableSymbol: String(form.get("variableSymbol") || "").trim() || undefined, specificSymbol: String(form.get("specificSymbol") || "").trim() || undefined };
    try {
      if (editingTemplate) await updatePaymentTemplate(editingTemplate.id, input);
      else await createPaymentTemplate(accountId, input);
      router.push("/payments/templates");
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Šablonu se nepodařilo uložit.");
    }
  }

  return <PageFrame><section className="payment-form-card"><p className="modal-kicker">Platební šablony</p><h1>{editingTemplate ? "Upravit šablonu" : "Nová šablona"}</h1><FormError message={error || accountsError || automationError} /><form onSubmit={submit}>
    <label>Název šablony<input required name="name" placeholder="Například nájem" defaultValue={editingTemplate?.name} /></label>
    <label>Odesílatel<select required name="accountId" defaultValue={editingTemplate?.accountId ?? accounts[0]?.id ?? ""}>{accounts.map((account) => <option key={account.id} value={account.id}>{account.type === "SAVINGS" ? "Spořicí účet" : "Běžný účet"} · {formatAccountNumber(account.accountNumber)}</option>)}</select></label>
    <label>Cílový účet<input required name="targetAccountNumber" placeholder="123456789 / 0100" defaultValue={editingTemplate?.targetAccountNumber} /></label>
    <div className="payment-fields payment-template-fields"><label>Částka<input required name="amount" type="number" min="0.01" step="0.01" placeholder="0,00" defaultValue={editingTemplate?.amount} /></label><label>Zpráva<input required name="description" placeholder="Popis" defaultValue={editingTemplate?.description} /></label></div><div className="payment-fields"><label>Variabilní symbol<input name="variableSymbol" inputMode="numeric" pattern="[0-9]{1,10}" maxLength={10} defaultValue={editingTemplate?.variableSymbol ?? ""} /></label><label>Specifický symbol<input name="specificSymbol" inputMode="numeric" pattern="[0-9]{1,10}" maxLength={10} defaultValue={editingTemplate?.specificSymbol ?? ""} /></label></div>
    <button className="pay-button payment-submit" type="submit">{editingTemplate ? "Uložit změny" : "Uložit šablonu"} <ArrowRight size={18} /></button>
  </form></section></PageFrame>;
}

export function TemplateListPage() {
  const { templates, error, setError, refresh } = useAutomation();

  async function remove(templateId: string) {
    if (!window.confirm("Opravdu chcete smazat tuto šablonu?")) return;
    try {
      await deletePaymentTemplate(templateId);
      await refresh();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "Šablonu se nepodařilo smazat.");
    }
  }

  function applyTemplate(template: PaymentTemplate) {
    const params = new URLSearchParams({ prefill: "1", fromAccountId: template.accountId, toAccountNumber: template.targetAccountNumber, amount: String(template.amount), description: template.description, variableSymbol: template.variableSymbol ?? "", specificSymbol: template.specificSymbol ?? "" });
    return `/payments?${params.toString()}`;
  }

  return <PageFrame><section className="payment-form-card"><div className="section-heading"><div><p className="modal-kicker">Platební šablony</p><h1>Seznam šablon</h1></div><Link className="pay-button" href="/payments/templates/new">Nová šablona <ArrowRight size={18} /></Link></div><FormError message={error} /><div className="automation-list selectable-list">{templates.map((template) => <div key={template.id}><Link href={applyTemplate(template)}><span>{template.name}</span><small>{formatAccountNumber(template.targetAccountNumber)} · {template.amount.toLocaleString("cs-CZ", { style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol" })}</small></Link><div className="automation-actions"><Link href={`/payments/templates/new?edit=${template.id}`}><FileText size={15} /> Upravit</Link><button type="button" onClick={() => remove(template.id)}><Trash2 size={15} /> Smazat</button></div></div>)}{templates.length === 0 && <p className="bank-empty">Zatím nemáte uložené šablony.</p>}</div></section></PageFrame>;
}

export function TransactionDetailPage() {
  const params = useParams<{ id: string }>();
  const { accounts, transactions, error } = useTransactions();
  const selectedTransaction = transactions.find((transaction) => transaction.id === params.id);
  const transactionAccount = selectedTransaction ? accounts.find((account) => account.id === selectedTransaction.accountId) : undefined;
  const counterpartyTransaction = selectedTransaction ? transactions.find((transaction) => transaction.id !== selectedTransaction.id && transaction.accountId !== selectedTransaction.accountId && transaction.description === selectedTransaction.description && transaction.amount === -selectedTransaction.amount) : undefined;
  const counterpartyAccount = counterpartyTransaction ? accounts.find((account) => account.id === counterpartyTransaction.accountId) : undefined;
  const counterpartyNumber = selectedTransaction?.counterpartyAccountNumber ?? counterpartyAccount?.accountNumber;

  if (!selectedTransaction) return <PageFrame><section className="payment-form-card"><FormError message={error || "Platbu se nepodařilo najít."} /></section></PageFrame>;

  return <PageFrame><section className="payment-form-card"><p className="modal-kicker">Detail platby</p><h1>{selectedTransaction.description}</h1><div className="transaction-detail-amount"><span className={selectedTransaction.type === "CREDIT" ? "incoming-amount" : ""}>{selectedTransaction.type === "CREDIT" ? "+" : "−"}{Math.abs(selectedTransaction.amount).toLocaleString("cs-CZ", { style: "currency", currency: transactionAccount?.currency ?? "CZK", currencyDisplay: "narrowSymbol" })}</span><small>{selectedTransaction.type === "CREDIT" ? "Příchozí platba" : "Odchozí platba"}</small></div><dl className="transaction-detail-list"><div><dt>Datum a čas</dt><dd>{new Intl.DateTimeFormat("cs-CZ", { dateStyle: "long", timeStyle: "short" }).format(new Date(selectedTransaction.createdAt))}</dd></div><div><dt>Odchozí účet</dt><dd>{selectedTransaction.type === "DEBIT" ? transactionAccount ? formatAccountNumber(transactionAccount.accountNumber) : "Neznámý účet" : counterpartyNumber ? formatAccountNumber(counterpartyNumber) : "Neuveden"}</dd></div><div><dt>Cílový účet</dt><dd>{selectedTransaction.type === "CREDIT" ? transactionAccount ? formatAccountNumber(transactionAccount.accountNumber) : "Neznámý účet" : counterpartyNumber ? formatAccountNumber(counterpartyNumber) : "Neuveden"}</dd></div><div><dt>Majitel účtu</dt><dd>{transactionAccount?.ownerName ?? "Neznámý"}</dd></div><div><dt>Variabilní symbol</dt><dd>{selectedTransaction.variableSymbol || "Neuveden"}</dd></div><div><dt>Specifický symbol</dt><dd>{selectedTransaction.specificSymbol || "Neuveden"}</dd></div><div><dt>Zpráva</dt><dd>{selectedTransaction.description}</dd></div><div><dt>ID transakce</dt><dd>{selectedTransaction.id}</dd></div></dl></section></PageFrame>;
}
