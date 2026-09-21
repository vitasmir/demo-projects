"use client";

import { ArrowRight, CheckCircle2, WalletCards, X } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import BankShell from "../BankShell";
import { Account, InterestSettings, OverdraftApplication, createOverdraftApplication, getAccounts, getInterestSettings, getOverdraftApplications, terminateOverdraft } from "../../lib/api";
import { getSession } from "../../lib/session";

const currency = new Intl.NumberFormat("cs-CZ", { style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol", maximumFractionDigits: 0 });

function applicationStatusLabel(status: OverdraftApplication["status"]) {
  return status === "PENDING" ? "Čeká na posouzení" : status === "APPROVED" ? "Schváleno" : "Neschváleno";
}

export default function OverdraftPage() {
  const [account, setAccount] = useState<Account | null>(null);
  const [settings, setSettings] = useState<InterestSettings | null>(null);
  const [applications, setApplications] = useState<OverdraftApplication[]>([]);
  const [limit, setLimit] = useState(50000);
  const [income, setIncome] = useState(45000);
  const [open, setOpen] = useState(false);
  const [terminationItem, setTerminationItem] = useState<OverdraftApplication | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const session = getSession();
    if (!session) return;
    Promise.all([getAccounts(), getInterestSettings(), getOverdraftApplications()]).then(([accounts, rates, items]) => {
      setAccount(accounts.find((item) => item.id === session.id) ?? null);
      setSettings(rates);
      setApplications(items.filter((item) => item.accountId === session.id));
    }).catch(() => setError("Kontokorent se nepodařilo načíst."));
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!account) return;
    setSaving(true);
    try {
      const application = await createOverdraftApplication({ accountId: account.id, requestedLimit: limit, monthlyIncome: income });
      setApplications((items) => [application, ...items]);
      setOpen(false);
      setError("");
    } catch (submissionError) { setError(submissionError instanceof Error ? submissionError.message : "Žádost se nepodařilo odeslat."); }
    finally { setSaving(false); }
  }

  async function terminate(item: OverdraftApplication) {
    if (!account || account.balance <= 0 || item.status !== "APPROVED") return;
    setSaving(true);
    try {
      await terminateOverdraft(item.id);
      const [accounts, items] = await Promise.all([getAccounts(), getOverdraftApplications()]);
      setAccount(accounts.find((candidate) => candidate.id === account.id) ?? null);
      setApplications(items.filter((candidate) => candidate.accountId === account.id));
      setTerminationItem(null);
      setError("");
    } catch (terminationError) {
      setError(terminationError instanceof Error ? terminationError.message : "Kontokorent se nepodařilo ukončit.");
    } finally { setSaving(false); }
  }

  function requestTermination(event: React.MouseEvent<HTMLButtonElement>, item: OverdraftApplication) {
    event.preventDefault();
    event.stopPropagation();
    setTerminationItem(item);
  }

  return <BankShell><div className="bank-content section-page loans-page">
    {error && <p className="api-notice">{error}</p>}
    <div className="page-hero"><div><p className="date-label">Finanční rezerva pro nečekané výdaje</p><h1>Kontokorent</h1><p className="page-lead">Mějte peníze k dispozici i v případě, že vám na účtu dočasně nestačí zůstatek.</p></div></div>
    <div className="loan-layout"><section className="loan-calculator"><span className="savings-icon"><WalletCards size={23} /></span><p className="modal-kicker">Povolené přečerpání</p><h2>Nastavte si svůj limit</h2><label><span><strong>Požadovaný limit</strong><b>{currency.format(limit)}</b></span><input type="range" min="1000" max="250000" step="1000" value={limit} onChange={(event) => setLimit(Number(event.target.value))} /><small><span>1 000 Kč</span><span>250 000 Kč</span></small></label><div className="loan-benefits"><span><CheckCircle2 size={17} /> Peníze kdykoliv k dispozici</span><span><CheckCircle2 size={17} /> Platíte úrok jen z čerpané částky</span></div></section><aside className="loan-summary"><p>Úroková sazba</p><strong>{settings ? `${settings.overdraftRate.toLocaleString("cs-CZ")} %` : "Načítám..."}</strong><dl><div><dt>Účet</dt><dd>{account?.accountNumber ?? "-"}</dd></div><div><dt>Maximální limit</dt><dd>{currency.format(250000)}</dd></div><div><dt>Schválení</dt><dd>Po posouzení žádosti</dd></div></dl><button className="pay-button" onClick={() => setOpen(true)} disabled={!account || applications.length > 0}>Požádat o kontokorent <ArrowRight size={18} /></button><small>Kontokorent je úvěrový produkt. O jeho schválení rozhoduje banka.</small></aside></div>
    <section className="loan-applications"><div className="section-heading"><div><h2>Výpis kontokorentu</h2><p>Stav vašich žádostí a rozhodnutí banky.</p></div><span>{applications.length}</span></div>{applications.length === 0 ? <p className="bank-empty">Zatím nemáte žádnou žádost o kontokorent.</p> : applications.map((item) => <article key={item.id}><span className={`loan-status ${item.status === "REJECTED" ? "loan-status-rejected" : ""}`}><CheckCircle2 size={18} /></span><div><strong>Kontokorent</strong><small>Odesláno {new Intl.DateTimeFormat("cs-CZ", { dateStyle: "medium" }).format(new Date(item.createdAt))}</small>{item.status === "REJECTED" && <small className="loan-rejection-reason">Důvod: {item.decisionNote || "Důvod nebyl uveden."}</small>}{item.status === "APPROVED" && <small className="loan-approved-label">Schváleno</small>}</div><div><strong>{currency.format(item.amount)}</strong><small>{currency.format(item.monthlyIncome)} měsíční příjem</small></div><div><b className={`loan-application-status status-${item.status.toLowerCase()}`}>{applicationStatusLabel(item.status)}</b>{item.status === "APPROVED" && (account?.balance ?? 0) > 0 && <button className="text-button overdraft-terminate-button" type="button" onClick={(event) => requestTermination(event, item)} disabled={saving}>Ukončit kontokorent</button>}</div></article>)}</section>
    {open && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setOpen(false)}><section className="payment-modal" role="dialog" aria-modal="true" aria-labelledby="overdraft-title"><button className="modal-close" onClick={() => setOpen(false)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">Kontokorent</p><h2 id="overdraft-title">Odeslat žádost</h2><form onSubmit={submit}><label>Požadovaný limit<input type="number" min="1000" max="250000" step="1000" value={limit} onChange={(event) => setLimit(Number(event.target.value))} required /></label><label>Měsíční příjem<input type="number" min="0" step="1000" value={income} onChange={(event) => setIncome(Number(event.target.value))} required /></label><button className="pay-button payment-submit" type="submit" disabled={saving}>{saving ? "Odesílám..." : "Odeslat žádost"}</button></form></section></div>}
    {terminationItem && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setTerminationItem(null)}><section className="payment-modal overdraft-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="termination-title"><button className="modal-close" onClick={() => setTerminationItem(null)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">Kontokorent</p><h2 id="termination-title">Ukončit kontokorent?</h2><p>Opravdu chcete ukončit schválený kontokorent?</p><div className="overdraft-confirm-actions"><button className="overdraft-confirm-no" type="button" onClick={() => setTerminationItem(null)} disabled={saving}>NE</button><button className="pay-button" type="button" onClick={() => void terminate(terminationItem)} disabled={saving}>{saving ? "Ukončuji..." : "ANO"}</button></div></section></div>}
  </div></BankShell>;
}
