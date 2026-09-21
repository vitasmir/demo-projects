"use client";

import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  Plus,
  ShieldCheck,
} from "lucide-react";
import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import BankShell from "../BankShell";
import {
  Account,
  BankTransaction,
  getAccounts,
  getTransactions,
  transferMoney,
} from "../../lib/api";

function formatAccountNumber(accountNumber: string) {
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

async function loadTransactions(accountList: Account[], selectedAccountId: string) {
  const selectedAccounts = selectedAccountId
    ? accountList.filter((account) => account.id === selectedAccountId)
    : accountList;
  const accountTransactions = await Promise.all(
    selectedAccounts.map((account) => getTransactions(account.id)),
  );
  return sortTransactions(accountTransactions.flat(), selectedAccounts);
}

export default function PaymentsPage() {
  const initialParams = typeof window === "undefined" ? null : new URLSearchParams(window.location.search);
  const shouldPrefill = initialParams?.get("prefill") === "1";
  const [sent, setSent] = useState(false);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [error, setError] = useState("");
  const [fromAccountId, setFromAccountId] = useState(() => shouldPrefill ? initialParams?.get("fromAccountId") ?? "" : "");
  const [toAccountNumber, setToAccountNumber] = useState(() => shouldPrefill ? initialParams?.get("toAccountNumber") ?? "" : "");
  const [amount, setAmount] = useState(() => shouldPrefill ? initialParams?.get("amount") ?? "" : "");
  const [description, setDescription] = useState(() => shouldPrefill ? initialParams?.get("description") ?? "" : "");
  const [variableSymbol, setVariableSymbol] = useState(() => shouldPrefill ? initialParams?.get("variableSymbol") ?? "" : "");
  const [specificSymbol, setSpecificSymbol] = useState(() => shouldPrefill ? initialParams?.get("specificSymbol") ?? "" : "");
  const [transactions, setTransactions] = useState<BankTransaction[]>([]);
  const [accountFilter] = useState(() => typeof window === "undefined" ? "" : new URLSearchParams(window.location.search).get("accountId") ?? "");
  const paymentFormRef = useRef<HTMLElement>(null);

  useEffect(() => {
    getAccounts()
      .then(async (loadedAccounts) => {
        setAccounts(loadedAccounts);
        setTransactions(await loadTransactions(loadedAccounts, accountFilter));
      })
      .catch(() =>
        setError("Backend není dostupný. Platby nelze načíst ani odeslat."),
      );
  }, [accountFilter]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const selectedFromAccountId = String(form.get("fromAccountId"));
    const selectedToAccountNumber = String(form.get("toAccountNumber")).trim();
    const formAmount = Number(form.get("amount"));
    const paymentAmount = Number(amount);
    const paymentDescription = String(form.get("description") || "Platba");
    const paymentVariableSymbol = String(form.get("variableSymbol") || "").trim();
    const paymentSpecificSymbol = String(form.get("specificSymbol") || "").trim();
    if (!selectedFromAccountId || !selectedToAccountNumber) {
      setError("Vyplňte odesílající i cílový účet.");
      return;
    }
    if (!Number.isFinite(paymentAmount) || paymentAmount < 0.01 || formAmount !== paymentAmount) {
      setError("Zadejte platnou částku vyšší než 0,01 Kč.");
      return;
    }
    try {
      await transferMoney({
        fromAccountId: selectedFromAccountId,
        toAccountNumber: selectedToAccountNumber,
        amount: paymentAmount,
        description: paymentDescription,
        variableSymbol: paymentVariableSymbol || undefined,
        specificSymbol: paymentSpecificSymbol || undefined,
      });
      setTransactions(await loadTransactions(accounts, accountFilter));
      setSent(true);
      setError("");
    } catch (transferError) {
      setError(
        transferError instanceof Error
          ? transferError.message
          : "Platbu se nepodařilo odeslat.",
      );
    }
  }
  function accountForTransaction(transaction: BankTransaction) {
    return accounts.find((account) => account.id === transaction.accountId);
  }

  const visibleTransactions = accountFilter
    ? transactions.filter((transaction) => transaction.accountId === accountFilter)
    : transactions;
  const selectedAccount = accounts.find((account) => account.id === accountFilter);

  function openNewPayment() {
    setSent(false);
    setError("");
    paymentFormRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
    window.setTimeout(
      () => paymentFormRef.current?.querySelector("select")?.focus(),
      350,
    );
  }

  return (
    <BankShell>
      <div className="bank-content section-page">
        <div className="page-hero">
          <div>
            <p className="date-label">Bezpečně a jednoduše</p>
            <h1>{accountFilter ? "Pohyby účtu" : "Platby"}</h1>
            <p className="page-lead">
              {accountFilter
                ? `Historie pohybů účtu ${selectedAccount?.accountNumber ?? ""}.`
                : "Pošlete peníze, nastavte trvalý příkaz nebo si prohlédněte šablony."}
            </p>
            {accountFilter && selectedAccount && <p className="account-balance-summary">Zůstatek: <strong>{selectedAccount.balance.toLocaleString("cs-CZ", { style: "currency", currency: selectedAccount.currency })}</strong></p>}
          </div>
          <button className="pay-button" type="button" onClick={openNewPayment}>
            <Plus size={19} /> Nová platba
          </button>
        </div>
        <div className="payments-layout">
          <section className="payment-form-card" ref={paymentFormRef}>
            <p className="modal-kicker">Tuzemská platba</p>
            <h2>Komu posíláte?</h2>
            {error && <p className="api-notice">{error}</p>}
            {sent ? (
              <div className="payment-success">
                <span>
                  <Check size={30} />
                </span>
                <h2>Platba byla odeslána</h2>
                <p>Backend převod zpracoval a zapsal odchozí pohyb.</p>
              </div>
            ) : (
              <form onSubmit={submit}>
                <label>
                  Odesílatel
                  <select
                    required
                    name="fromAccountId"
                    value={fromAccountId}
                    onChange={(event) => {
                      setFromAccountId(event.target.value);
                      setError("");
                    }}
                  >
                    <option value="" disabled>
                      Vyberte účet
                    </option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.type === "SAVINGS" ? "Spořicí účet" : "Běžný účet"} · {formatAccountNumber(account.accountNumber)} (
                        {account.balance.toLocaleString("cs-CZ")}{" "}
                        {account.currency})
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Cílový účet
                  <input
                    required
                    name="toAccountNumber"
                    value={toAccountNumber}
                    onChange={(event) => {
                      setToAccountNumber(event.target.value);
                      setError("");
                    }}
                    onBlur={() =>
                      setToAccountNumber(formatAccountNumber(toAccountNumber))
                    }
                    placeholder="Například 123456789 / 0100"
                  />
                </label>
                <div className="payment-fields">
                  <label>
                    Částka
                    <input
                      required
                      name="amount"
                      value={amount}
                      onChange={(event) => setAmount(event.target.value)}
                      type="number"
                      min="0.01"
                      step="0.01"
                      placeholder="0,00"
                    />
                  </label>
                  <label>
                    Měna
                    <select defaultValue="CZK">
                      <option>CZK</option>
                      <option>EUR</option>
                    </select>
                  </label>
                </div>
                <label>
                  Zpráva pro příjemce
                  <input
                    name="description"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Například oběd"
                  />
                </label>
                <div className="payment-fields payment-symbol-fields">
                  <label>
                    Variabilní symbol
                    <input name="variableSymbol" value={variableSymbol} onChange={(event) => setVariableSymbol(event.target.value.replace(/\D/g, "").slice(0, 10))} inputMode="numeric" placeholder="Nepovinné" />
                  </label>
                  <label>
                    Specifický symbol
                    <input name="specificSymbol" value={specificSymbol} onChange={(event) => setSpecificSymbol(event.target.value.replace(/\D/g, "").slice(0, 10))} inputMode="numeric" placeholder="Nepovinné" />
                  </label>
                </div>
                <button className="pay-button payment-submit" type="submit">
                  Odeslat platbu <ArrowRight size={18} />
                </button>
                <p className="secure-note">
                  <ShieldCheck size={16} /> Platba se odešle přes zabezpečené
                  REST API.
                </p>
              </form>
            )}
          </section>
          <aside className="quick-actions">
            <h2>Rychlé volby</h2>
            <Link href="/payments/standing-orders/new">
              <span>
                <Plus size={18} />
              </span>
              Nový trvalý příkaz
              <ArrowRight size={16} />
            </Link>
            <Link href="/payments/standing-orders">
              <span>
                <Plus size={18} />
              </span>
              Trvalé příkazy
              <ArrowRight size={16} />
            </Link>
            <Link href="/payments/repeat">
              <span>
                <Plus size={18} />
              </span>
              Opakovat platbu
              <ArrowRight size={16} />
            </Link>
            <Link href="/payments/templates/new">
              <span>
                <Plus size={18} />
              </span>
              Nová šablona
              <ArrowRight size={16} />
            </Link>
            <Link href="/payments/templates">
              <span>
                <Plus size={18} />
              </span>
              Seznam šablon
              <ArrowRight size={16} />
            </Link>
          </aside>
        </div>
        <section
          className="payment-history"
          aria-labelledby="payment-history-title"
        >
          <div className="section-heading">
            <div>
              <h2 id="payment-history-title">Seznam plateb</h2>
              <p>Historie pohybů na vašich účtech</p>
            </div>
            <span className="payment-count">{visibleTransactions.length} plateb</span>
          </div>
          <div className="payment-history-list">
            {visibleTransactions.map((transaction) => (
              <Link
                className="payment-history-row"
                key={transaction.id}
                href={`/payments/transactions/${transaction.id}`}
                title="Otevřít detail platby"
              >
                <span
                  className={`transaction-icon ${transaction.type === "CREDIT" ? "incoming" : ""}`}
                >
                  {transaction.type === "CREDIT" ? (
                    <ArrowDownLeft size={19} />
                  ) : (
                    <ArrowUpRight size={19} />
                  )}
                </span>
                <span className="transaction-copy">
                  <strong>{transaction.description}</strong>
                  <small>
                    {transaction.type === "CREDIT"
                      ? "Příchozí platba"
                      : "Odchozí platba"}{" "}
                    ·{" "}
                    {accountForTransaction(transaction)
                      ? formatAccountNumber(
                          accountForTransaction(transaction)!.accountNumber,
                        )
                      : "Neznámý účet"}
                  </small>
                </span>
                <span className="transaction-date">
                  {new Intl.DateTimeFormat("cs-CZ", {
                    day: "numeric",
                    month: "long",
                  }).format(new Date(transaction.createdAt))}
                </span>
                <strong
                  className={`amount ${transaction.type === "CREDIT" ? "incoming-amount" : ""}`}
                >
                  {transaction.type === "CREDIT" ? "+" : "−"}
                  {Math.abs(transaction.amount).toLocaleString("cs-CZ", {
                    style: "currency",
                    currency:
                      accountForTransaction(transaction)?.currency ?? "CZK",
                  })}
                </strong>
                <ArrowRight className="row-arrow" size={16} />
              </Link>
            ))}
            {visibleTransactions.length === 0 && (
              <p className="bank-empty">Zatím nemáte žádné platby.</p>
            )}
          </div>
        </section>
      </div>
    </BankShell>
  );
}
