"use client";

import { FormEvent, useEffect, useState } from "react";
import { Building2, CheckCircle2 } from "lucide-react";
import BankShell from "../BankShell";
import {
  Account,
  InterestSettings,
  createLoanApplication,
  getAccounts,
  getInterestSettings,
} from "../../lib/api";
import { getSession } from "../../lib/session";

const currency = new Intl.NumberFormat("cs-CZ", {
  style: "currency",
  currency: "CZK",
  currencyDisplay: "narrowSymbol",
  maximumFractionDigits: 0,
});

function monthlyPayment(amount: number, months: number, annualRate: number) {
  const monthlyRate = annualRate / 1200;
  return (
    Math.round(
      ((amount * monthlyRate) / (1 - (1 + monthlyRate) ** -months)) * 100,
    ) / 100
  );
}

export default function MortgagesPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [settings, setSettings] = useState<InterestSettings | null>(null);
  const [amount, setAmount] = useState(2_000_000);
  const [months, setMonths] = useState(300);
  const [purpose, setPurpose] = useState("");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const session = getSession();
  const savingsAccount = accounts.find(
    (account) => account.type === "SAVINGS" && account.currency === "CZK",
  );
  const rate = settings?.mortgageRate ?? 4.2;
  const equityPercent = settings?.mortgageMinimumEquityPercent ?? 20;
  const requiredEquity = (amount * equityPercent) / 100;
  const payment = monthlyPayment(amount, months, rate);
  const hasEquity = (savingsAccount?.balance ?? 0) >= requiredEquity;

  useEffect(() => {
    Promise.all([getAccounts(), getInterestSettings()])
      .then(([loadedAccounts, loadedSettings]) => {
        setAccounts(loadedAccounts);
        setSettings(loadedSettings);
      })
      .catch(() => setMessage("Podmínky hypotéky se nepodařilo načíst."));
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !hasEquity) return;
    setSaving(true);
    try {
      await createLoanApplication(session.id, {
        type: "MORTGAGE",
        amount,
        repaymentMonths: months,
        purpose: purpose.trim(),
      });
      setMessage(
        "Žádost o hypotéku byla odeslána. U hypoték nad 1 000 000 Kč jsou potřeba dvě schválení administrátorů.",
      );
      setPurpose("");
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Žádost o hypotéku se nepodařilo odeslat.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <BankShell>
      <div className="bank-content section-page loans-page">
        <div className="page-hero">
          <div>
            <p className="date-label">Vlastní bydlení</p>
            <h1>Hypotéky</h1>
            <p className="page-lead">
              Financování bydlení s delší dobou splácení a nižší měsíční
              splátkou.
            </p>
          </div>
        </div>
        {message && <p className="api-notice">{message}</p>}
        <div className="loan-layout">
          <section className="loan-calculator">
            <span className="savings-icon">
              <Building2 size={23} />
            </span>
            <p className="modal-kicker">Hypotéka</p>
            <h2>Nastavte si financování bydlení</h2>
            <form className="mortgage-form" onSubmit={submit}>
              <label className="mortgage-range-field">
                <span>
                  <strong>Výše hypotéky</strong>
                  <b>{currency.format(amount)}</b>
                </span>
                <input
                  type="range"
                  min="500000"
                  max="20000000"
                  step="50000"
                  value={amount}
                  onChange={(event) => setAmount(Number(event.target.value))}
                />
                <small>
                  <span>500 000 Kč</span>
                  <span>20 000 000 Kč</span>
                </small>
              </label>
              <label className="mortgage-range-field">
                <span>
                  <strong>Doba splácení</strong>
                  <b>{months / 12} let</b>
                </span>
                <input
                  type="range"
                  min="120"
                  max="360"
                  step="12"
                  value={months}
                  onChange={(event) => setMonths(Number(event.target.value))}
                />
                <small>
                  <span>10 let</span>
                  <span>30 let</span>
                </small>
              </label>
              <label className="mortgage-purpose-field">
                Účel hypotéky
                <input
                  value={purpose}
                  onChange={(event) => setPurpose(event.target.value)}
                  required
                  maxLength={80}
                  placeholder="Koupě bytu, stavba domu..."
                />
              </label>
              <button
                className="pay-button payment-submit"
                type="submit"
                disabled={saving || !hasEquity}
              >
                {saving ? "Odesílám..." : "Požádat o hypotéku"}
              </button>
            </form>
          </section>
          <aside className="loan-summary">
            <p>Orientační měsíční splátka</p>
            <strong>{currency.format(payment)}</strong>
            <dl>
              <div>
                <dt>Úroková sazba</dt>
                <dd>{rate.toLocaleString("cs-CZ")} % p. a.</dd>
              </div>
              <div>
                <dt>Vlastní prostředky</dt>
                <dd>{equityPercent.toLocaleString("cs-CZ")} %</dd>
              </div>
              <div>
                <dt>Požadováno na spoření</dt>
                <dd>{currency.format(requiredEquity)}</dd>
              </div>
              <div>
                <dt>Na spořicím účtu</dt>
                <dd>{currency.format(savingsAccount?.balance ?? 0)}</dd>
              </div>
            </dl>
            <p
              className={
                hasEquity ? "mortgage-eligible" : "mortgage-ineligible"
              }
            >
              {hasEquity ? (
                <>
                  <CheckCircle2 size={16} /> Podmínku vlastních prostředků
                  splňujete.
                </>
              ) : (
                "Pro žádost potřebujete navýšit zůstatek spořicího účtu."
              )}
            </p>
          </aside>
        </div>
      </div>
    </BankShell>
  );
}
