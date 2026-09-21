"use client";

import { ArrowRight, Eye, EyeOff, Landmark, Plus, RefreshCw, TrendingUp, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import BankShell from "../BankShell";
import { Account, createSavingsAccount, getAccounts } from "../../lib/api";
import { getSession } from "../../lib/session";

export default function AccountsPage() {
  const [visible, setVisible] = useState(true);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [error, setError] = useState("");
  const [detailAccount, setDetailAccount] = useState<Account | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  function loadAccounts() {
    setError("");
    return getAccounts().then(setAccounts).catch(() => setError("Účty se nepodařilo načíst."));
  }

  useEffect(() => {
    getAccounts().then((loadedAccounts) => {
      setAccounts(loadedAccounts);
    }).catch(() => setError("Účty se nepodařilo načíst."));
  }, []);

  async function openSavingsAccount() {
    if (!sourceAccount || savingsAccount) return;
    setSaving(true);
    try {
      const createdAccount = await createSavingsAccount(sourceAccount.id);
      setAccounts((currentAccounts) => [...currentAccounts, createdAccount]);
      setMessage("Spořicí účet byl založen.");
    } catch (submissionError) {
      setMessage(submissionError instanceof Error ? submissionError.message : "Spořicí účet se nepodařilo založit.");
    } finally {
      setSaving(false);
    }
  }

  const session = getSession();
  const sourceAccount = accounts.find((account) => account.id === session?.id);
  const primaryAccount = accounts.find((account) => account.id === session?.id) ?? accounts[0];
  const savingsAccount = accounts.find((account) => account.type === "SAVINGS");
  return <BankShell><div className="bank-content section-page">
    {error && <p className="api-notice">{error}</p>}
    {message && <p className="api-notice">{message}</p>}
    <div className="page-hero"><div><p className="date-label">Vaše peníze na jednom místě</p><h1>Moje účty</h1><p className="page-lead">Přehled účtů, zůstatků a nastavení vašich financí.</p></div><button className="pay-button" onClick={() => void openSavingsAccount()} disabled={saving || !sourceAccount || Boolean(savingsAccount)}><Plus size={19} /> {saving ? "Zakládám..." : "Založit spořicí účet"}</button></div>
    {accounts.length > 0 && <div className="account-management-bar"><span><strong>{accounts.length}</strong> {accounts.length === 1 ? "účet" : "účty"} v přehledu</span><button className="text-button" onClick={() => void loadAccounts()}><RefreshCw size={15} /> Obnovit</button></div>}
    <div className="section-heading"><h2>Účty a zůstatky</h2><button className="text-button" onClick={() => setVisible((state) => !state)}>{visible ? <EyeOff size={16} /> : <Eye size={16} />} {visible ? "Skrýt zůstatky" : "Zobrazit zůstatky"}</button></div>
    <div className="account-page-grid">
      <article className="account-primary account-detail-card"><div className="account-topline"><span className="account-type"><Landmark size={20} /> Běžný účet</span>{primaryAccount && <Link className="row-arrow" href={`/payments?accountId=${primaryAccount.id}`} aria-label="Zobrazit pohyby běžného účtu" title="Zobrazit pohyby běžného účtu"><ArrowRight size={17} /></Link>}</div><p className="account-number">{primaryAccount?.accountNumber ?? "-"}</p><p className="balance-label">Disponibilní zůstatek</p><strong className="main-balance">{visible ? primaryAccount ? `${primaryAccount.balance.toLocaleString("cs-CZ", { minimumFractionDigits: 2 })} ${primaryAccount.currency}` : "-" : "••••••••"}</strong><div className="account-footer"><span><i /> Aktivní účet</span><button onClick={() => primaryAccount && setDetailAccount(primaryAccount)}>Detail účtu <ArrowRight size={15} /></button></div></article>
      {savingsAccount && <article className="savings-account account-detail-card"><div className="savings-head"><span><TrendingUp size={19} /> Spořicí účet</span><Link className="row-arrow" href={`/payments?accountId=${savingsAccount.id}`} aria-label="Zobrazit pohyby spořicího účtu" title="Zobrazit pohyby spořicího účtu"><ArrowRight size={17} /></Link></div><p className="account-number">{savingsAccount.accountNumber}</p><strong>{visible ? `${savingsAccount.balance.toLocaleString("cs-CZ", { minimumFractionDigits: 2 })} ${savingsAccount.currency}` : "••••••••"}</strong><p>Účet je veden v měně {savingsAccount.currency}.</p><div className="saving-progress"><span style={{ width: "0%" }} /></div><small>Spořicí cíl zatím není nastaven.</small><div className="account-footer"><span><i /> Aktivní účet</span><button onClick={() => setDetailAccount(savingsAccount)}>Detail účtu <ArrowRight size={15} /></button></div></article>}
    </div>
    <section className="settings-panel"><div><h2>Výpisy a nastavení</h2><p>Spravujte limity, notifikace a detaily svých účtů.</p></div><Link className="text-button" href="/settings">Otevřít nastavení <ArrowRight size={16} /></Link></section>
    {detailAccount && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setDetailAccount(null)}><section className="payment-modal account-modal" role="dialog" aria-modal="true" aria-labelledby="account-detail-title"><button className="modal-close" onClick={() => setDetailAccount(null)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">Detail účtu</p><h2 id="account-detail-title">{detailAccount.ownerName}</h2><dl className="account-detail-list"><div><dt>Číslo účtu</dt><dd>{detailAccount.accountNumber}</dd></div><div><dt>E-mail</dt><dd>{detailAccount.email}</dd></div><div><dt>Adresa</dt><dd>{detailAccount.address}</dd></div><div><dt>Zůstatek</dt><dd>{detailAccount.balance.toLocaleString("cs-CZ", { style: "currency", currency: detailAccount.currency })}</dd></div></dl></section></div>}
  </div></BankShell>;
}
