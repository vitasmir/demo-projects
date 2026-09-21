"use client";

import {
  Check, ChevronRight, CircleDollarSign, ClipboardCheck,
  Eye, EyeOff, Landmark, LayoutDashboard, Search, ShieldCheck, Users, WalletCards, X,
} from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import Reports from "./reports/Reports";
import Header from "./Header";
import {
  Account, AdminUser, BankApplication, Dashboard, InterestSettings, decideApplication, getAccounts,
  getDashboard, getInterestSettings, getLoans, getOverdrafts, updateInterestSettings,
  adminLogin, changeAdminPassword, decideRegistration, getAdmins,
  createAdmin,
} from "@/lib/api";

type View = "overview" | "loans" | "reports" | "overdrafts" | "clients" | "settings" | "admins";

const viewPaths: Record<View, string> = {
  overview: "/overview",
  loans: "/loans",
  reports: "/reports",
  overdrafts: "/overdrafts",
  clients: "/clients",
  settings: "/settings",
  admins: "/admins",
};

function viewForPath(pathname: string): View {
  const entry = (Object.entries(viewPaths) as [View, string][]).find(([, path]) => path === pathname);
  return entry?.[0] ?? "overview";
}

const money = new Intl.NumberFormat("cs-CZ", { style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol", maximumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("cs-CZ", { dateStyle: "medium", timeStyle: "short" });
const navigation = [
  { id: "overview" as const, label: "Přehled", icon: LayoutDashboard },
  { id: "loans" as const, label: "Půjčky", icon: CircleDollarSign },
  { id: "reports" as const, label: "Reporty", icon: ClipboardCheck },
  { id: "overdrafts" as const, label: "Kontokorenty", icon: WalletCards },
  { id: "clients" as const, label: "Klienti", icon: Users },
  { id: "settings" as const, label: "Nastavení sazeb", icon: ClipboardCheck },
  { id: "admins" as const, label: "Administrátoři", icon: ShieldCheck },
];

async function hashPassword(password: string, username: string) {
  const encoder = new TextEncoder();
  const salt = encoder.encode(`listek-password-salt:${username.trim().toLocaleLowerCase("cs-CZ")}`);
  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt, iterations: 120000, hash: "SHA-256" }, key, 256);
  return Array.from(new Uint8Array(bits), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export default function Home() {
  const pathname = usePathname();
  const router = useRouter();
  const view = viewForPath(pathname);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [loans, setLoans] = useState<BankApplication[]>([]);
  const [overdrafts, setOverdrafts] = useState<BankApplication[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [selected, setSelected] = useState<BankApplication | null>(null);
  const [search, setSearch] = useState("");
  const [loanSearch, setLoanSearch] = useState("");
  const [pendingOnly, setPendingOnly] = useState(true);
  const [onlyUnprocessedLoans, setOnlyUnprocessedLoans] = useState(true);
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [interestSettings, setInterestSettings] = useState<InterestSettings | null>(null);
  const [currentDate] = useState<Date>(() => new Date());
  const [adminReady, setAdminReady] = useState(false);
  const [adminUser, setAdminUser] = useState("");
  const [mustChangePassword, setMustChangePassword] = useState(false);
  const [pendingRegistrations, setPendingRegistrations] = useState<Account[]>([]);
  const [authError, setAuthError] = useState("");
  const [authSaving, setAuthSaving] = useState(false);
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showPasswordConfirmation, setShowPasswordConfirmation] = useState(false);
  const [adminFormError, setAdminFormError] = useState("");
  const [adminFormSaving, setAdminFormSaving] = useState(false);

  useEffect(() => {
    if (pathname === "/") {
      return;
    }
    const token = localStorage.getItem("listek-admin-session");
    const username = localStorage.getItem("listek-admin-user");
    if (!token || !username) {
      router.replace("/");
      return;
    }
    queueMicrotask(() => {
      setAdminUser(username);
      setMustChangePassword(localStorage.getItem("listek-admin-must-change") === "true");
      setAdminReady(true);
    });
    async function refreshData() {
      const [dashboardData, loanData, overdraftData, accountData, settingsData, adminData] = await Promise.all([getDashboard(), getLoans(), getOverdrafts(), getAccounts(), getInterestSettings(), getAdmins()]);
      setDashboard(dashboardData);
      setLoans(loanData);
      setOverdrafts(overdraftData);
      setAccounts(accountData);
      setInterestSettings(settingsData);
      setAdmins(adminData);
      setPendingRegistrations([]);
      setError("");
    }
    refreshData()
      .catch(() => setError("Administraci se nepodařilo spojit s backendem."))
      .finally(() => setLoading(false));
    const interval = window.setInterval(() => {
      void refreshData().catch(() => setError("Administraci se nepodařilo spojit s backendem."));
    }, 10000);
    return () => window.clearInterval(interval);
  }, [adminReady, pathname, router]);

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setAuthSaving(true); setAuthError("");
    const form = new FormData(event.currentTarget);
    try {
      const username = String(form.get("username")).trim();
      const result = await adminLogin({ username, password: await hashPassword(String(form.get("password")), username) });
      localStorage.setItem("listek-admin-session", result.token); localStorage.setItem("listek-admin-user", result.username); localStorage.setItem("listek-admin-must-change", String(result.mustChangePassword));
      setAdminUser(result.username); setMustChangePassword(result.mustChangePassword); setAdminReady(true); setLoading(true); router.replace(viewPaths.overview);
      const [dashboardData, loanData, overdraftData, accountData, settingsData, adminData] = await Promise.all([getDashboard(), getLoans(), getOverdrafts(), getAccounts(), getInterestSettings(), getAdmins()]);
      setDashboard(dashboardData); setLoans(loanData); setOverdrafts(overdraftData); setAccounts(accountData); setInterestSettings(settingsData); setAdmins(adminData); setPendingRegistrations([]); setLoading(false);
    } catch { setAuthError("Uživatelské jméno nebo heslo nesedí. Zkontrolujte zadané údaje."); }
    finally { setAuthSaving(false); }
  }

  async function saveAdminPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget); const password = String(form.get("password"));
    if (password !== String(form.get("confirmation"))) { setAuthError("Hesla se neshodují."); return; }
    setAuthSaving(true); setAuthError("");
    try { await changeAdminPassword(await hashPassword(password, adminUser)); localStorage.setItem("listek-admin-must-change", "false"); setMustChangePassword(false); }
    catch (passwordError) { setAuthError(passwordError instanceof Error ? passwordError.message : "Heslo se nepodařilo změnit."); }
    finally { setAuthSaving(false); }
  }

  async function decideRegistrationRequest(id: string, status: "APPROVED" | "REJECTED") {
    try {
      const updated = await decideRegistration(id, status);
      setPendingRegistrations((items) => items.filter((item) => item.id !== updated.id));
      setAccounts((items) => [...items, updated]);
    } catch (decisionError) { setError(decisionError instanceof Error ? decisionError.message : "Registraci se nepodařilo vyřídit."); }
  }

  const allApplications = [...loans, ...overdrafts].sort((left, right) => right.createdAt.localeCompare(left.createdAt));
  const sourceApplications = view === "loans" ? loans : view === "overdrafts" ? overdrafts : allApplications;
  const visibleApplications = sourceApplications.filter((application) =>
    (!pendingOnly || application.status === "PENDING")
      && (application.clientName.toLocaleLowerCase("cs").includes(search.toLocaleLowerCase("cs"))
        || application.accountNumber.includes(search)));
  const visibleLoans = loans.filter((loan) =>
    loan.status === "APPROVED" && (!onlyUnprocessedLoans || (loan.remainingAmount ?? 0) > 0)
      && (loan.clientName.toLocaleLowerCase("cs").includes(loanSearch.toLocaleLowerCase("cs"))
        || loan.accountNumber.includes(loanSearch)));

  async function decide(status: "APPROVED" | "REJECTED") {
    if (!selected) return;
    setSaving(true);
    try {
      const updated = await decideApplication(selected, status, note.trim());
      const updateList = (items: BankApplication[]) => items.map((item) => item.id === updated.id ? updated : item);
      if (updated.category === "LOAN") setLoans(updateList);
      else setOverdrafts(updateList);
      setSelected(updated);
      setNote("");
      setDashboard(await getDashboard());
      setError("");
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : "Rozhodnutí se nepodařilo uložit.");
    } finally {
      setSaving(false);
    }
  }

  const titles: Record<View, [string, string]> = {
    overview: ["Operační přehled", "Dnes máte pod kontrolou vše důležité."],
    loans: ["Žádosti o půjčku", "Posuďte žádosti, riziko a schopnost klienta splácet."],
    reports: ["Reporty", "Sledujte schválení, splácení a zůstatky půjček."],
    overdrafts: ["Žádosti o kontokorent", "Rozhodujte o krátkodobých úvěrových rámcích."],
    clients: ["Klienti a účty", "Rychlý dohled nad klientským portfoliem banky."],
    settings: ["Nastavení sazeb", "Spravujte úrokové sazby produktů dostupných klientům."],
    admins: ["Administrátoři", "Vytvářejte přístupy pro další členy administrace."],
  };

  async function saveNewAdmin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const username = String(form.get("newAdminUsername")).trim();
    const firstName = String(form.get("newAdminFirstName")).trim();
    const lastName = String(form.get("newAdminLastName")).trim();
    const birthNumber = String(form.get("newAdminBirthNumber")).trim();
    const email = String(form.get("newAdminEmail")).trim();
    const street = String(form.get("newAdminStreet")).trim();
    const city = String(form.get("newAdminCity")).trim();
    const postalCode = String(form.get("newAdminPostalCode")).trim();
    const password = String(form.get("newAdminPassword"));
    if (password !== String(form.get("newAdminConfirmation"))) { setAdminFormError("Hesla se neshodují."); return; }
    setAdminFormSaving(true); setAdminFormError("");
    try {
      const createdAdmin = await createAdmin({ username, firstName, lastName, birthNumber, email, street, city, postalCode, password: await hashPassword(password, username) });
      setAdmins((items) => [...items, createdAdmin]);
      formElement.reset();
      setAdminFormError("Administrátor byl vytvořen.");
    } catch (createError) { setAdminFormError(createError instanceof Error ? createError.message : "Administrátora se nepodařilo vytvořit."); }
    finally { setAdminFormSaving(false); }
  }

  async function saveInterestSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    try {
      const updated = await updateInterestSettings({
        savingsRate: Number(form.get("savingsRate")), overdraftRate: Number(form.get("overdraftRate")),
        personalLoanRate: Number(form.get("personalLoanRate")), homeLoanRate: Number(form.get("homeLoanRate")),
        mortgageRate: Number(form.get("mortgageRate")), mortgageMinimumEquityPercent: Number(form.get("mortgageMinimumEquityPercent")),
      });
      setInterestSettings(updated);
      setError("");
    } catch (saveError) { setError(saveError instanceof Error ? saveError.message : "Sazby se nepodařilo uložit."); }
    finally { setSaving(false); }
  }

  if (!adminReady) return <main className="admin-auth"><form className="admin-modal" onSubmit={signIn}><p>ADMINISTRACE BANKY LÍSTEK</p><h2>Přihlášení administrátora</h2><span>Přihlaste se pro správu registrací a bankovních žádostí.</span><label>Uživatelské jméno<input name="username" required autoComplete="username" /></label><label>Heslo<div className="password-field"><input name="password" required type={showLoginPassword ? "text" : "password"} autoComplete="current-password" /><button type="button" className="password-toggle" onClick={() => setShowLoginPassword((visible) => !visible)} aria-label={showLoginPassword ? "Skrýt heslo" : "Zobrazit heslo"} title={showLoginPassword ? "Skrýt heslo" : "Zobrazit heslo"}>{showLoginPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>{authError && <div className="notice">{authError}</div>}<button className="primary-button submit-button" disabled={authSaving}>{authSaving ? "Přihlašuji..." : "Přihlásit se"}</button></form></main>;
  if (mustChangePassword) return <main className="admin-auth"><form className="admin-modal" onSubmit={saveAdminPassword}><p>PRVNÍ PŘIHLÁŠENÍ</p><h2>Změňte heslo administrátora</h2><span>Výchozí heslo musí být před pokračováním změněno.</span><label>Nové heslo<div className="password-field"><input name="password" required minLength={12} type={showNewPassword ? "text" : "password"} autoComplete="new-password" /><button type="button" className="password-toggle" onClick={() => setShowNewPassword((visible) => !visible)} aria-label={showNewPassword ? "Skrýt heslo" : "Zobrazit heslo"} title={showNewPassword ? "Skrýt heslo" : "Zobrazit heslo"}>{showNewPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label><label>Potvrzení hesla<div className="password-field"><input name="confirmation" required minLength={12} type={showPasswordConfirmation ? "text" : "password"} autoComplete="new-password" /><button type="button" className="password-toggle" onClick={() => setShowPasswordConfirmation((visible) => !visible)} aria-label={showPasswordConfirmation ? "Skrýt heslo" : "Zobrazit heslo"} title={showPasswordConfirmation ? "Skrýt heslo" : "Zobrazit heslo"}>{showPasswordConfirmation ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>{authError && <div className="notice">{authError}</div>}<button className="primary-button submit-button" disabled={authSaving}>{authSaving ? "Ukládám..." : "Změnit heslo"}</button></form></main>;

  return (
    <div className="admin-app">
      <aside className={`admin-sidebar ${menuOpen ? "is-open" : ""}`}>
        <div className="brand"><span className="brand-mark"><i /></span><span>Lístek<small>Manager</small></span></div>
        <button className="sidebar-close" onClick={() => setMenuOpen(false)} aria-label="Zavřít nabídku"><X size={20} /></button>
        <p className="nav-label">Pracovní prostor</p>
        <nav>
          {navigation.map(({ id, label, icon: Icon }) => (
            <button className={view === id ? "active" : ""} key={id} onClick={() => { router.push(viewPaths[id]); setSelected(null); setMenuOpen(false); }}>
              <Icon size={19} /><span>{label}</span>
              {(id === "overview" || id === "loans" || id === "overdrafts") && <b>{id === "loans" ? dashboard?.pendingLoans ?? 0 : id === "overdrafts" ? dashboard?.pendingOverdrafts ?? 0 : (dashboard?.pendingLoans ?? 0) + (dashboard?.pendingOverdrafts ?? 0)}</b>}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot"><ShieldCheck size={18} /><span>Zabezpečená administrace<small>Produkční prostředí</small></span></div>
      </aside>
      {menuOpen && <button className="menu-scrim" aria-label="Zavřít nabídku" onClick={() => setMenuOpen(false)} />}

      <main className="admin-main">
        <Header adminUser={adminUser} onMenuOpen={() => setMenuOpen(true)} onSignOut={() => window.location.reload()} />

        <div className="admin-content">
          <section className="page-heading">
            <div><p>{currentDate ? new Intl.DateTimeFormat("cs-CZ", { day: "numeric", month: "long", year: "numeric" }).format(currentDate).toUpperCase() : "Načítám datum..."}</p><h1>{titles[view][0]}</h1><span>{titles[view][1]}</span></div>
            {view !== "clients" && view !== "settings" && view !== "admins" && view !== "reports" && <div className="heading-actions"><div className="search"><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Hledat klienta nebo účet" /></div><label className="pending-filter"><input type="checkbox" checked={pendingOnly} onChange={(event) => setPendingOnly(event.target.checked)} /> Jen čekající</label></div>}
          </section>

          {error && <div className="notice">{error}</div>}
          {loading && <div className="loading">Načítám data banky...</div>}

          {!loading && view === "overview" && dashboard && <section className="metrics">
            <article><span className="metric-icon green"><Users size={21} /></span><div><small>Aktivní klienti</small><strong>{accounts.filter((account) => account.type === "CURRENT").length}</strong><p>Podle počtu běžných účtů</p></div></article>
            <article><span className="metric-icon blue"><Landmark size={21} /></span><div><small>Vklady klientů</small><strong>{money.format(dashboard.deposits)}</strong><p>Napříč všemi účty</p></div></article>
            <article><span className="metric-icon dark"><ClipboardCheck size={21} /></span><div><small>Dnes rozhodnuto</small><strong>{dashboard.decidedToday}</strong><p>Vyřízených žádostí</p></div></article>
          </section>}

          {view !== "clients" && view !== "settings" && view !== "admins" && view !== "reports" && !loading && <section className="work-layout">
            <div className="queue-panel">
              <div className="panel-heading"><div><p>{view === "overview" ? "PRIORITNÍ FRONTA" : "VŠECHNY ŽÁDOSTI"}</p><h2>{view === "loans" ? "Půjčky" : view === "overdrafts" ? "Kontokorenty" : "Žádosti k posouzení"}</h2></div><span>{visibleApplications.filter((item) => item.status === "PENDING").length} čeká</span></div>
              <div className="application-list">
                {visibleApplications.length === 0 && <div className="empty-state"><Check size={25} /><strong>Fronta je prázdná</strong><span>Žádné žádosti neodpovídají filtru.</span></div>}
                {visibleApplications.map((application) => (
                  <button key={application.id} className={selected?.id === application.id ? "selected" : ""} onClick={() => { setSelected(application); setNote(application.decisionNote ?? ""); }}>
                    <span className={`application-type ${application.category.toLowerCase()}`}>{application.category === "LOAN" ? <CircleDollarSign size={20} /> : <WalletCards size={20} />}</span>
                    <div><strong>{application.clientName}</strong><small>{application.category === "LOAN" ? (application.product === "PERSONAL" ? "Půjčka na cokoliv" : "Půjčka na bydlení") : "Kontokorent"} · {application.accountNumber}</small></div>
                    <div className="application-amount"><strong>{money.format(application.amount)}</strong><small>{application.category === "OVERDRAFT" ? "Limit k čerpání" : "Požadovaná částka"}</small><span className={`status ${application.status.toLowerCase()}`}>{application.status === "PENDING" ? "Čeká" : application.status === "APPROVED" ? "Schváleno" : "Zamítnuto"}</span></div>
                    <ChevronRight size={18} />
                  </button>
                ))}
              </div>
            </div>

            <aside className="detail-panel">
              {!selected ? <div className="detail-empty"><ClipboardCheck size={34} /><h2>Vyberte žádost</h2><p>V detailu uvidíte finanční údaje a provedete rozhodnutí.</p></div> : <>
                <div className="detail-head"><span className={`application-type ${selected.category.toLowerCase()}`}>{selected.category === "LOAN" ? <CircleDollarSign size={22} /> : <WalletCards size={22} />}</span><div><small>{selected.category === "LOAN" ? "ŽÁDOST O PŮJČKU" : "ŽÁDOST O KONTOKORENT"}</small><h2>{selected.clientName}</h2></div></div>
                <dl><div><dt>{selected.category === "OVERDRAFT" ? "Limit k čerpání" : "Požadovaná částka"}</dt><dd>{money.format(selected.amount)}</dd></div><div><dt>Účet</dt><dd>{selected.accountNumber}</dd></div>{selected.repaymentMonths && <div><dt>Splatnost</dt><dd>{selected.repaymentMonths} měsíců</dd></div>}{selected.monthlyPayment && <div><dt>Měsíční splátka</dt><dd>{money.format(selected.monthlyPayment)}</dd></div>}{selected.monthlyIncome && <div><dt>Měsíční příjem</dt><dd>{money.format(selected.monthlyIncome)}</dd></div>}<div><dt>Účel</dt><dd>{selected.purpose}</dd></div><div><dt>Podáno</dt><dd>{date.format(new Date(selected.createdAt))}</dd></div></dl>
                {selected.status === "PENDING" ? <div className="decision-box"><label>Poznámka k rozhodnutí<textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Volitelná poznámka k rozhodnutí" maxLength={500} /></label><div><button className="reject" disabled={saving} onClick={() => decide("REJECTED")}><X size={17} /> Zamítnout</button><button className="approve" disabled={saving} onClick={() => decide("APPROVED")}><Check size={17} /> Schválit</button></div></div> : <div className={`decision-result ${selected.status.toLowerCase()}`}><strong>{selected.status === "APPROVED" ? "Žádost byla schválena" : "Žádost byla zamítnuta"}</strong><p>{selected.decisionNote || "Bez doplňující poznámky."}</p></div>}
              </>}
            </aside>
          </section>}

          {view === "reports" && !loading && <Reports />}

          {view === "clients" && !loading && <section className="clients-panel">
            <div className="panel-heading"><div><p>KLIENTSKÝ KMEN</p><h2>Účty a zůstatky</h2></div><span>{accounts.length} klientů</span></div>
            <div className="client-table"><div className="table-head"><span>Klient</span><span>Číslo účtu</span><span>Kontakt</span><span>Zůstatek</span></div>{accounts.map((account) => <article key={account.id}><span className="client-name"><i>{account.ownerName.split(" ").map((part) => part[0]).slice(0, 2).join("")}</i><strong>{account.ownerName}</strong></span><span>{account.accountNumber}</span><span>{account.email}</span><strong>{money.format(account.balance)}</strong></article>)}</div>
            {pendingRegistrations.length > 0 && <div className="registration-queue"><div className="panel-heading"><div><p>ČEKÁ NA SCHVÁLENÍ</p><h2>Nové registrace</h2></div><span>{pendingRegistrations.length} čeká</span></div>{pendingRegistrations.map((registration) => <article key={registration.id}><div><strong>{registration.ownerName}</strong><small>{registration.email} · {registration.accountNumber}</small></div><div><button className="reject" onClick={() => decideRegistrationRequest(registration.id, "REJECTED")}>Zamítnout</button><button className="approve" onClick={() => decideRegistrationRequest(registration.id, "APPROVED")}>Schválit</button></div></article>)}</div>}
          </section>}

          {view === "loans" && !loading && <section className="clients-panel loan-records-panel">
            <div className="panel-heading"><div><p>EVIDENCE ÚVĚRŮ</p><h2>Přehled půjček</h2></div><div className="loan-records-controls"><div className="search"><Search size={18} /><input value={loanSearch} onChange={(event) => setLoanSearch(event.target.value)} placeholder="Hledej klienta nebo účet" /></div><label className="pending-filter"><input type="checkbox" checked={onlyUnprocessedLoans} onChange={(event) => setOnlyUnprocessedLoans(event.target.checked)} /> Jen nesplacené</label><span>{visibleLoans.length} aktivních</span></div></div>
            <div className="loan-records-table"><div className="table-head"><span>Klient a účet</span><span>Částka</span><span>Splaceno</span><span>Zbývá</span><span>Úrok</span><span>Splátkový účet</span></div>{visibleLoans.map((loan) => <article key={loan.id}><span><strong>{loan.clientName}</strong><small>{loan.accountNumber}</small></span><strong>{money.format(loan.amount)}</strong><strong>{money.format(loan.repaidAmount ?? 0)}</strong><strong>{money.format(loan.remainingAmount ?? 0)}</strong><strong>{loan.annualRate?.toLocaleString("cs-CZ") ?? "-"} % p. a.</strong><span><strong>{loan.repaymentAccountNumber ?? "-"}</strong><small>{loan.repaymentDayOfMonth ? `${loan.repaymentDayOfMonth}. den v měsíci` : "-"}</small><small>VS {loan.variableSymbol ?? "-"} · SS {loan.specificSymbol ?? "-"}</small></span></article>)}</div>
            {visibleLoans.length === 0 && <div className="empty-state"><Check size={25} /><strong>{onlyUnprocessedLoans ? "Žádné nezpracované půjčky" : "Žádné schválené půjčky"}</strong><span>{onlyUnprocessedLoans ? "Všechny schválené půjčky jsou již uzavřené." : "Po schválení se zde zobrazí evidence splácení."}</span></div>}
          </section>}

          {view === "settings" && !loading && interestSettings && <section className="settings-card"><div className="panel-heading"><div><p>PRODUKTOVÉ PODMÍNKY</p><h2>Úrokové sazby</h2></div><span>% p. a.</span></div><form onSubmit={saveInterestSettings} className="rate-form"><label>Spořicí účet<input name="savingsRate" type="number" min="0" step="0.001" defaultValue={interestSettings.savingsRate} /></label><label>Kontokorent<input name="overdraftRate" type="number" min="0" step="0.001" defaultValue={interestSettings.overdraftRate} /></label><label>Půjčka na cokoliv<input name="personalLoanRate" type="number" min="0" step="0.001" defaultValue={interestSettings.personalLoanRate} /></label><label>Půjčka na bydlení<input name="homeLoanRate" type="number" min="0" step="0.001" defaultValue={interestSettings.homeLoanRate} /></label><label>Hypotéka<input name="mortgageRate" type="number" min="0" step="0.001" defaultValue={interestSettings.mortgageRate} /></label><label>Vlastní prostředky pro hypotéku (%)<input name="mortgageMinimumEquityPercent" type="number" min="0" max="100" step="0.01" defaultValue={interestSettings.mortgageMinimumEquityPercent} /></label><button className="primary-button submit-button" type="submit" disabled={saving}>{saving ? "Ukládám..." : "Uložit sazby"}</button></form></section>}

          {view === "admins" && <section className="settings-card admin-users-card"><div className="panel-heading"><div><p>SPRÁVA PŘÍSTUPŮ</p><h2>Administrátoři</h2></div><ShieldCheck size={22} color="var(--green)" /></div><div className="admin-list"><div className="admin-list-head"><span>Login</span><span>Jméno a příjmení</span><span>E-mail</span></div>{admins.map((admin) => <div className="admin-list-row" key={admin.username}><strong>{admin.username}</strong><span>{admin.firstName} {admin.lastName}</span><span>{admin.email}</span></div>)}</div><div className="admin-create-heading"><p>SPRÁVA PŘÍSTUPŮ</p><h2>Nový administrátor</h2></div><form onSubmit={saveNewAdmin} className="rate-form"><label>Uživatelské jméno<input name="newAdminUsername" required maxLength={80} autoComplete="username" /></label><label>Jméno<input name="newAdminFirstName" required maxLength={100} autoComplete="given-name" /></label><label>Příjmení<input name="newAdminLastName" required maxLength={100} autoComplete="family-name" /></label><label>Rodné číslo<input name="newAdminBirthNumber" required pattern="[0-9]{6}/?[0-9]{3,4}" placeholder="123456/7890" /></label><label>E-mail<input name="newAdminEmail" required type="email" maxLength={160} autoComplete="email" /></label><label>Ulice a číslo<input name="newAdminStreet" required maxLength={160} autoComplete="street-address" /></label><label>Město<input name="newAdminCity" required maxLength={100} autoComplete="address-level2" /></label><label>PSČ<input name="newAdminPostalCode" required pattern="[0-9]{3} ?[0-9]{2}" placeholder="110 00" autoComplete="postal-code" /></label><label>Heslo<input name="newAdminPassword" required minLength={12} type="password" autoComplete="new-password" /></label><label>Heslo znovu<input name="newAdminConfirmation" required minLength={12} type="password" autoComplete="new-password" /></label>{adminFormError && <div className="notice admin-form-notice">{adminFormError}</div>}<button className="primary-button submit-button" type="submit" disabled={adminFormSaving}>{adminFormSaving ? "Vytvářím..." : "Vytvořit administrátora"}</button></form></section>}
        </div>
      </main>
    </div>
  );
}
