"use client";

import {
  ArrowDownLeft, ArrowRight, ArrowUpRight, Bell, ChevronDown,
  CreditCard, Eye, EyeOff, FileText, HandCoins, HelpCircle, Home, Landmark,
  LogOut, Menu, Plus, Search, Send, Settings, Smartphone, WalletCards,
  Sparkles, TrendingUp, X,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getAccounts, getTransactions, Account, BankTransaction, updateAccount } from "../lib/api";
import { clearSession, getSession, setSession } from "../lib/session";
import { FormEvent, useDeferredValue, useEffect, useRef, useState } from "react";

type Transaction = {
  id: number;
  title: string;
  detail: string;
  date: string;
  amount: number;
  icon: "card" | "income" | "payment" | "mobile";
  source?: BankTransaction;
};

const transactions: Transaction[] = [
];

const navigation = [
  { label: "Přehled", href: "/", icon: Home }, { label: "Účty", href: "/ucty", icon: Landmark },
  { label: "Platby", href: "/payments", icon: Send }, { label: "Karty", href: "/karty", icon: CreditCard },
  { label: "Spoření", href: "/sporeni", icon: TrendingUp }, { label: "Půjčky", href: "/pujcky", icon: HandCoins },
  { label: "Hypotéky", href: "/hypoteky", icon: Home },
  { label: "Kontokorent", href: "/kontokorent", icon: WalletCards },
  { label: "Dokumenty", href: "/dokumenty", icon: FileText },
];

const currency = new Intl.NumberFormat("cs-CZ", {
  style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol", minimumFractionDigits: 2,
});

function TransactionIcon({ type }: { type: Transaction["icon"] }) {
  const Icon = type === "income" ? ArrowDownLeft : type === "mobile" ? Smartphone : type === "payment" ? ArrowUpRight : CreditCard;
  return <Icon size={19} strokeWidth={1.8} />;
}

export default function BankDashboard() {
  const router = useRouter();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [apiTransactions, setApiTransactions] = useState<BankTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState("");
  const [balanceVisible, setBalanceVisible] = useState(true);
  const [search, setSearch] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileName, setProfileName] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profileAddress, setProfileAddress] = useState("");
  const [profilePassword, setProfilePassword] = useState("");
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileError, setProfileError] = useState("");
  const profileInputRef = useRef<HTMLInputElement>(null);
  const profileModalRef = useRef<HTMLElement>(null);
  const [noticeOpen, setNoticeOpen] = useState<"notifications" | "logout" | "spending" | null>(null);
  const [sessionReady, setSessionReady] = useState(false);
  const [currentDate] = useState<Date>(() => new Date());
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const deferredSearch = useDeferredValue(search);
  const displayedTransactions: Transaction[] = apiTransactions.length > 0 ? apiTransactions.map((transaction, index) => ({
    id: index,
    title: transaction.description,
    detail: transaction.type === "CREDIT" ? "Příchozí platba" : "Odchozí platba",
    date: new Intl.DateTimeFormat("cs-CZ", { day: "numeric", month: "long" }).format(new Date(transaction.createdAt)),
    amount: transaction.amount,
    icon: transaction.type === "CREDIT" ? "income" : "payment",
    source: transaction,
  })) : transactions;
  const filteredTransactions = displayedTransactions.filter((transaction) =>
    `${transaction.title} ${transaction.detail}`.toLocaleLowerCase("cs").includes(deferredSearch.toLocaleLowerCase("cs")),
  );
  const spendingDate = currentDate ?? new Date();
  const spendingTransactions = apiTransactions.filter((transaction) => {
    const transactionDate = new Date(transaction.createdAt);
    return transaction.amount < 0
      && transactionDate.getFullYear() === spendingDate.getFullYear()
      && transactionDate.getMonth() === spendingDate.getMonth();
  });
  const spendingTotal = spendingTransactions.reduce((total, transaction) => total + Math.abs(transaction.amount), 0);
  const weeklySpending = Array.from({ length: 12 }, (_, index) => spendingTransactions
    .filter((transaction) => Math.ceil(new Date(transaction.createdAt).getDate() / 3) - 1 === index)
    .reduce((total, transaction) => total + Math.abs(transaction.amount), 0));
  const highestWeeklySpending = Math.max(...weeklySpending, 1);
  const spendingMonthLabel = new Intl.DateTimeFormat("cs-CZ", { month: "long" }).format(spendingDate);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
      return;
    }
    getAccounts()
      .then(async (loadedAccounts) => {
        const currentAccounts = loadedAccounts.filter((account) => account.id === session.id);
        if (currentAccounts.length === 0) {
          clearSession();
          router.replace("/login");
          return;
        }
        setAccounts(loadedAccounts);
        const accountTransactions = await Promise.all(currentAccounts.map((account) => getTransactions(account.id)));
        setApiTransactions(accountTransactions.flat().sort((first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime()));
      })
      .catch(() => {
        setAccounts([]);
        setApiTransactions([]);
        setApiError("Účty se nepodařilo načíst.");
      })
      .finally(() => { setLoading(false); setSessionReady(true); });
  }, [router]);

  useEffect(() => {
    if (!profileOpen) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setProfileOpen(false);
      if (event.key !== "Tab") return;
      const focusable = profileModalRef.current?.querySelectorAll<HTMLElement>("button, input, select, textarea, a[href]");
      if (!focusable?.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", closeOnEscape);
    profileInputRef.current?.focus();
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [profileOpen]);

  const session = getSession();
  const currentAccount = accounts.find((account) => account.id === session?.id) ?? accounts[0];
  const savingsAccount = accounts.find((account) => account.type === "SAVINGS");
  const firstName = currentAccount?.ownerName.trim().split(/\s+/)[0];
  const hour = currentDate?.getHours() ?? 12;
  const greeting = hour < 12 ? "Dobré ráno" : hour < 18 ? "Dobré odpoledne" : "Dobrý večer";
  function counterpartyFor(transaction: Transaction) {
    if (!transaction.source) return undefined;
    return apiTransactions.find((candidate) => candidate.id !== transaction.source?.id
      && candidate.accountId !== transaction.source?.accountId
      && candidate.description === transaction.source?.description
      && candidate.amount === -transaction.source?.amount);
  }
  const selectedAccount = selectedTransaction?.source ? accounts.find((account) => account.id === selectedTransaction.source?.accountId) : currentAccount;
  const selectedCounterpartyAccount = selectedTransaction ? accounts.find((account) => account.id === counterpartyFor(selectedTransaction)?.accountId) : undefined;

  function openProfile() {
    setProfileName(currentAccount?.ownerName ?? "");
    setProfileEmail(currentAccount?.email ?? "");
    setProfileAddress(currentAccount?.address ?? "");
    setProfilePassword("");
    setProfileError("");
    setProfileOpen(true);
  }

  async function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!currentAccount || !profileName.trim() || !profileEmail.trim() || !profileAddress.trim()) return;
    setProfileSaving(true);
    try {
      const updatedAccount = await updateAccount(currentAccount.id, { ownerName: profileName.trim(), email: profileEmail.trim(), address: profileAddress.trim(), password: profilePassword || undefined });
      setSession(updatedAccount);
      setAccounts((currentAccounts) => currentAccounts.map((account) => account.id === updatedAccount.id ? updatedAccount : account));
      setProfileEmail(updatedAccount.email);
      setProfileAddress(updatedAccount.address);
      setProfilePassword("");
      setProfileOpen(false);
      setProfileError("");
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : "Profil se nepodařilo uložit.");
    } finally {
      setProfileSaving(false);
    }
  }

  if (!sessionReady) return <div className="session-loading">Načítám bankovnictví...</div>;

  return (
    <div className="bank-app">
      <aside className={`bank-sidebar ${menuOpen ? "is-open" : ""}`} inert={profileOpen ? true : undefined}>
        <div className="bank-logo" aria-label="Lístek banka"><span className="logo-mark"><span /></span><span>Lístek</span></div>
        <button className="sidebar-close" onClick={() => setMenuOpen(false)} aria-label="Zavřít nabídku"><X size={22} /></button>
        <nav className="bank-nav" aria-label="Hlavní navigace">
          {navigation.map(({ label, href, icon: Icon }, index) => (
            <Link className={index === 0 ? "active" : ""} href={href} key={label} onClick={() => setMenuOpen(false)}><Icon size={20} strokeWidth={1.8} /><span>{label}</span></Link>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Link href="/help"><HelpCircle size={20} /><span>Pomoc a kontakt</span></Link>
          <Link href="/settings"><Settings size={20} /><span>Nastavení</span></Link>
        </div>
      </aside>
      {menuOpen && <button className="menu-scrim" onClick={() => setMenuOpen(false)} aria-label="Zavřít nabídku" />}

      <main className="bank-main" inert={profileOpen ? true : undefined}>
        <header className="bank-header">
          <button className="mobile-menu" onClick={() => setMenuOpen(true)} aria-label="Otevřít nabídku"><Menu /></button>
          <div className="mobile-logo">Lístek</div>
          <div className="header-actions">
            <button className="icon-button notification" onClick={() => setNoticeOpen("notifications")} aria-label="Oznámení"><Bell size={20} /><span /></button>
            <button className="profile-button" onClick={openProfile} title="Otevřít profil"><span className="avatar">{(currentAccount?.ownerName ?? "?").split(" ").map((part) => part[0]).slice(0, 2).join("").toUpperCase()}</span><span className="profile-name">{currentAccount?.ownerName ?? "Načítám..."}</span><ChevronDown size={16} /></button>
            <button className="header-logout" onClick={() => setNoticeOpen("logout")}><LogOut size={17} /> Odhlásit se</button>
          </div>
        </header>

        <div className="bank-content">
          {loading && <p className="api-notice">Načítám data z bankovního backendu...</p>}
          {apiError && <p className="api-notice">{apiError}</p>}
          <section className="welcome-row">
            <div><p className="date-label">{currentDate ? new Intl.DateTimeFormat("cs-CZ", { weekday: "long", day: "numeric", month: "long" }).format(currentDate) : "Načítám datum..."}</p><h1>{greeting}{firstName ? `, ${firstName}` : ""}.</h1></div>
            <Link className="pay-button" href="/payments"><Plus size={19} /> Nová platba</Link>
          </section>

          <section className="account-section" aria-labelledby="accounts-title">
            <div className="section-heading"><div><h2 id="accounts-title">Moje účty</h2><button className="text-button" onClick={() => setBalanceVisible((visible) => !visible)} aria-label={balanceVisible ? "Skrýt zůstatky" : "Zobrazit zůstatky"}>{balanceVisible ? <Eye size={17} /> : <EyeOff size={17} />} {balanceVisible ? "Skrýt zůstatky" : "Zobrazit zůstatky"}</button></div><Link className="text-button" href="/ucty" aria-label="Otevřít správu účtů" title="Otevřít správu účtů">Spravovat účty <ArrowRight size={16} /></Link></div>
            <div className="account-grid">
              <article className="account-primary">
                <div className="account-topline"><Link className="account-type account-link" href={currentAccount ? `/payments?accountId=${currentAccount.id}` : "/payments"} title="Zobrazit pohyby běžného účtu">Běžný účet</Link></div>
                <Link className="account-number account-link" href={currentAccount ? `/payments?accountId=${currentAccount.id}` : "/payments"} title="Zobrazit pohyby běžného účtu">{currentAccount?.accountNumber ?? "-"}</Link><p className="balance-label">Disponibilní zůstatek</p>
                <strong className="main-balance">{balanceVisible ? currentAccount ? currency.format(currentAccount.balance) : "-" : "••••••••"}</strong>
                <div className="account-footer"><span><i /> Aktivní účet</span><Link href="/ucty" aria-label="Otevřít detail běžného účtu" title="Otevřít detail běžného účtu">Detail účtu <ArrowRight size={15} /></Link></div>
              </article>
              {savingsAccount && <article className="savings-account">
                <div className="savings-head"><Link className="account-link" href={`/payments?accountId=${savingsAccount.id}`} title="Zobrazit pohyby spořicího účtu"><span><TrendingUp size={19} /> Spořicí účet</span></Link><Link href="/sporeni" aria-label="Otevřít detail spořicího účtu" title="Otevřít detail spořicího účtu"><ArrowRight size={17} /></Link></div>
                <Link className="account-number account-link" href={`/payments?accountId=${savingsAccount.id}`} title="Zobrazit pohyby spořicího účtu">{savingsAccount.accountNumber}</Link><strong>{balanceVisible ? savingsAccount ? currency.format(savingsAccount.balance) : "-" : "••••••••"}</strong><p>Úrok 4,2 % p. a.</p>
                <div className="saving-progress"><span style={{ width: "0%" }} /></div><small>Spořicí cíl zatím není nastaven.</small><div className="account-footer"><span><i /> Aktivní účet</span><Link href="/ucty" aria-label="Otevřít detail spořicího účtu" title="Otevřít detail spořicího účtu">Detail účtu <ArrowRight size={15} /></Link></div>
              </article>}
              <article className="card-preview">
                <div className="card-chip" /><span className="card-brand">Lístek</span><p>••••&nbsp; ••••&nbsp; ••••&nbsp; 2841</p><div><span>{currentAccount?.ownerName ?? "Načítám..."}</span><b>VISA</b></div>
              </article>
            </div>
          </section>

          <div className="dashboard-grid">
            <section className="transactions-section" aria-labelledby="transactions-title">
              <div className="section-heading transaction-heading">
                <div><h2 id="transactions-title">Poslední pohyby</h2><p>Srpen 2026</p></div>
                <label className="transaction-search"><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Hledat" aria-label="Hledat v transakcích" /></label>
              </div>
              <div className="transaction-list">
                {filteredTransactions.map((transaction) => (
                  <button className="transaction-row" key={transaction.id} onClick={() => setSelectedTransaction(transaction)} title="Otevřít detail pohybu">
                    <span className={`transaction-icon ${transaction.amount > 0 ? "incoming" : ""}`}><TransactionIcon type={transaction.icon} /></span>
                    <span className="transaction-copy"><strong>{transaction.title}</strong><small>{transaction.detail}</small></span>
                    <span className="transaction-date">{transaction.date}</span>
                    <strong className={transaction.amount > 0 ? "amount incoming-amount" : "amount"}>{transaction.amount > 0 ? "+" : ""}{currency.format(transaction.amount)}</strong>
                    <ArrowRight className="row-arrow" size={16} />
                  </button>
                ))}
                {filteredTransactions.length === 0 && <p className="bank-empty">Žádný pohyb neodpovídá hledání.</p>}
              </div>
              <Link className="all-transactions" href="/payments">Všechny pohyby <ArrowRight size={16} /></Link>
            </section>

            <aside className="insights-column">
              <section className="spending-panel">
                <div className="section-heading"><h2>Výdaje v {spendingMonthLabel}</h2><button onClick={() => setNoticeOpen("spending")} aria-label="Otevřít detail výdajů" title="Otevřít detail výdajů"><ArrowRight size={17} /></button></div>
                <strong>{currency.format(spendingTotal)}</strong><p>Součet odchozích plateb za tento měsíc</p>
                <div className="spending-bars" aria-label="Výdaje po týdnech">{weeklySpending.map((amount, index) => <span key={index} style={{ height: `${amount === 0 ? 2 : Math.max((amount / highestWeeklySpending) * 100, 8)}%` }} className={index === Math.ceil(spendingDate.getDate() / 3) - 1 ? "current" : ""} />)}</div>
                <div className="bar-labels"><span>1. 8.</span><span>Dnes</span><span>31. 8.</span></div>
              </section>
              <section className="tip-panel"><Sparkles size={21} /><div><strong>Tip pro vaše peníze</strong><p>Na běžném účtu máte víc, než obvykle. Přesuňte část na spoření.</p><Link href="/sporeni" title="Otevřít převod peněz na spoření">Přesunout peníze <ArrowRight size={15} /></Link></div></section>
            </aside>
          </div>
        </div>
      </main>

      {selectedTransaction && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setSelectedTransaction(null)}><section className="payment-modal transaction-detail-modal" role="dialog" aria-modal="true" aria-labelledby="dashboard-transaction-detail-title"><button className="modal-close" onClick={() => setSelectedTransaction(null)} aria-label="Zavřít detail"><X size={21} /></button><p className="modal-kicker">Detail pohybu</p><h2 id="dashboard-transaction-detail-title">{selectedTransaction.title}</h2><div className="transaction-detail-amount"><span className={selectedTransaction.amount > 0 ? "incoming-amount" : ""}>{selectedTransaction.amount > 0 ? "+" : "−"}{currency.format(Math.abs(selectedTransaction.amount))}</span><small>{selectedTransaction.detail}</small></div><dl className="transaction-detail-list"><div><dt>Datum</dt><dd>{selectedTransaction.source ? new Intl.DateTimeFormat("cs-CZ", { dateStyle: "long", timeStyle: "short" }).format(new Date(selectedTransaction.source.createdAt)) : selectedTransaction.date}</dd></div><div><dt>Typ pohybu</dt><dd>{selectedTransaction.amount > 0 ? "Příchozí platba" : "Odchozí platba"}</dd></div><div><dt>Odchozí účet</dt><dd>{selectedTransaction.amount < 0 ? selectedAccount?.accountNumber ?? "Neuveden" : selectedCounterpartyAccount?.accountNumber ?? "Neuveden"}</dd></div><div><dt>Cílový účet</dt><dd>{selectedTransaction.amount > 0 ? selectedAccount?.accountNumber ?? "Neuveden" : selectedCounterpartyAccount?.accountNumber ?? "Neuveden"}</dd></div><div><dt>Variabilní symbol</dt><dd>{selectedTransaction.source?.variableSymbol || "Neuveden"}</dd></div><div><dt>Specifický symbol</dt><dd>{selectedTransaction.source?.specificSymbol || "Neuveden"}</dd></div><div><dt>Zpráva</dt><dd>{selectedTransaction.source?.description ?? selectedTransaction.title}</dd></div>{selectedTransaction.source && <div><dt>ID transakce</dt><dd>{selectedTransaction.source.id}</dd></div>}</dl></section></div>}
      {profileOpen && <div className="modal-backdrop" role="presentation"><section ref={profileModalRef} className="payment-modal profile-modal" role="dialog" aria-modal="true" aria-labelledby="dashboard-profile-title"><button className="modal-close" type="button" onClick={() => setProfileOpen(false)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">Váš profil</p><h2 id="dashboard-profile-title">Osobní údaje a přihlášení</h2>{profileError && <p className="api-notice">{profileError}</p>}<form onSubmit={submitProfile}><label>Jméno a příjmení<input ref={profileInputRef} required minLength={2} maxLength={120} value={profileName} onChange={(event) => setProfileName(event.target.value)} /></label><label>E-mail<input required type="email" value={profileEmail} onChange={(event) => setProfileEmail(event.target.value)} /></label><label>Adresa<input required maxLength={240} value={profileAddress} onChange={(event) => setProfileAddress(event.target.value)} /></label><label>Nové heslo<input type="password" minLength={8} placeholder="Ponechte prázdné, pokud ho neměníte" value={profilePassword} onChange={(event) => setProfilePassword(event.target.value)} /></label><button className="pay-button payment-submit" type="submit" disabled={profileSaving}>{profileSaving ? "Ukládám..." : "Uložit profil"}</button></form></section></div>}
      {noticeOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setNoticeOpen(null)}><section className="payment-modal compact-modal" role="dialog" aria-modal="true" aria-labelledby="dashboard-notice-title"><button className="modal-close" onClick={() => setNoticeOpen(null)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">{noticeOpen === "spending" ? "Výdaje" : noticeOpen === "notifications" ? "Oznámení" : "Přihlášení"}</p><h2 id="dashboard-notice-title">{noticeOpen === "spending" ? "Odchozí platby" : noticeOpen === "notifications" ? "Vše je v pořádku" : "Odhlásit se?"}</h2>{noticeOpen === "spending" ? <div className="spending-detail-list">{spendingTransactions.map((transaction) => <div key={transaction.id}><span>{transaction.description}</span><strong>{currency.format(transaction.amount)}</strong></div>)}</div> : <><p className="modal-copy">{noticeOpen === "notifications" ? "Nemáte žádná nová oznámení." : "Pro další práci s účtem se budete muset znovu přihlásit."}</p><button className="pay-button payment-submit" onClick={() => { if (noticeOpen === "logout") { clearSession(); router.replace("/login"); } else setNoticeOpen(null); }}>{noticeOpen === "logout" ? "Odhlásit se" : "Rozumím"}</button></>}</section></div>}
    </div>
  );
}