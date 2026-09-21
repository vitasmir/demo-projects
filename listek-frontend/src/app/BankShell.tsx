"use client";

import {
  Bell, ChevronDown, CreditCard, FileText, HelpCircle, Home, Landmark,
  HandCoins, LogOut, Menu, Send, Settings, TrendingUp, WalletCards, X,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { FormEvent, ReactNode, useEffect, useRef, useState } from "react";
import { Account, getAccounts, updateAccount } from "../lib/api";
import { clearSession, getSession, setSession } from "../lib/session";

const navigation = [
  { label: "Přehled", href: "/", icon: Home },
  { label: "Účty", href: "/ucty", icon: Landmark },
  { label: "Platby", href: "/payments", icon: Send },
  { label: "Karty", href: "/karty", icon: CreditCard },
  { label: "Spoření", href: "/sporeni", icon: TrendingUp },
  { label: "Půjčky", href: "/pujcky", icon: HandCoins },
  { label: "Hypotéky", href: "/hypoteky", icon: Home },
  { label: "Kontokorent", href: "/kontokorent", icon: WalletCards },
  { label: "Dokumenty", href: "/dokumenty", icon: FileText },
];

export default function BankShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [account, setAccount] = useState<Account | null>(null);
  const [profileOpen, setProfileOpen] = useState(false);
  const [profileName, setProfileName] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profileAddress, setProfileAddress] = useState("");
  const [profilePassword, setProfilePassword] = useState("");
  const [profileError, setProfileError] = useState("");
  const [profileSaving, setProfileSaving] = useState(false);
  const profileInputRef = useRef<HTMLInputElement>(null);
  const profileModalRef = useRef<HTMLElement>(null);
  const [noticeOpen, setNoticeOpen] = useState<"notifications" | "logout" | null>(null);
  const [sessionReady, setSessionReady] = useState(false);

  useEffect(() => {
    const session = getSession();
    if (!session) {
      router.replace("/login");
      return;
    }
    getAccounts().then((accounts) => {
      const currentAccount = accounts.find((candidate) => candidate.id === session.id);
      if (!currentAccount) {
        clearSession();
        router.replace("/login");
        return;
      }
      setAccount(currentAccount);
      setProfileName(currentAccount.ownerName);
      setProfileEmail(currentAccount.email);
      setProfileAddress(currentAccount.address);
    }).catch(() => {
      setAccount(session);
      setProfileError("Profil se nepodařilo obnovit.");
    }).finally(() => setSessionReady(true));
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

  function openProfile() {
    setProfileName(account?.ownerName ?? "");
    setProfileEmail(account?.email ?? "");
    setProfileAddress(account?.address ?? "");
    setProfilePassword("");
    setProfileError("");
    setProfileOpen(true);
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!account || !profileName.trim() || !profileEmail.trim() || !profileAddress.trim()) return;
    setProfileSaving(true);
    try {
      const updatedAccount = await updateAccount(account.id, { ownerName: profileName.trim(), email: profileEmail.trim(), address: profileAddress.trim(), password: profilePassword || undefined });
      setAccount(updatedAccount);
      setSession(updatedAccount);
      setProfileName(updatedAccount.ownerName);
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

  const displayName = account?.ownerName ?? "Načítám...";
  const initials = displayName.split(" ").filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase() || "JK";

  if (!sessionReady) return <div className="session-loading">Načítám bankovnictví...</div>;

  return (
    <div className="bank-app">
      <aside className={`bank-sidebar ${menuOpen ? "is-open" : ""}`} inert={profileOpen ? true : undefined}>
        <div className="bank-logo" aria-label="Lístek banka"><span className="logo-mark"><span /></span><span>Lístek</span></div>
        <button className="sidebar-close" onClick={() => setMenuOpen(false)} aria-label="Zavřít nabídku"><X size={22} /></button>
        <nav className="bank-nav" aria-label="Hlavní navigace">
          {navigation.map(({ label, href, icon: Icon }) => (
            <Link className={pathname === href ? "active" : ""} href={href} key={label} onClick={() => setMenuOpen(false)}><Icon size={20} strokeWidth={1.8} /><span>{label}</span></Link>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Link href="/help" onClick={() => setMenuOpen(false)}><HelpCircle size={20} /><span>Pomoc a kontakt</span></Link>
          <Link href="/settings" onClick={() => setMenuOpen(false)}><Settings size={20} /><span>Nastavení</span></Link>
        </div>
      </aside>
      {menuOpen && <button className="menu-scrim" onClick={() => setMenuOpen(false)} aria-label="Zavřít nabídku" />}
      <main className="bank-main" inert={profileOpen ? true : undefined}>
        <header className="bank-header">
          <button className="mobile-menu" onClick={() => setMenuOpen(true)} aria-label="Otevřít nabídku"><Menu /></button>
          <div className="mobile-logo">Lístek</div>
          <div className="header-actions">
            <button className="icon-button notification" onClick={() => setNoticeOpen("notifications")} aria-label="Oznámení"><Bell size={20} /><span /></button>
            <button className="profile-button" onClick={openProfile} title="Otevřít profil"><span className="avatar">{initials}</span><span className="profile-name">{displayName}</span><ChevronDown size={16} /></button>
            <button className="header-logout" onClick={() => setNoticeOpen("logout")}><LogOut size={17} /> Odhlásit se</button>
          </div>
        </header>
        {children}
      </main>
      {profileOpen && <div className="modal-backdrop" role="presentation"><section ref={profileModalRef} className="payment-modal profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-title"><button className="modal-close" type="button" onClick={() => setProfileOpen(false)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">Váš profil</p><h2 id="profile-title">Osobní údaje a přihlášení</h2>{profileError && <p className="api-notice">{profileError}</p>}<form onSubmit={saveProfile}><label>Jméno a příjmení<input ref={profileInputRef} required minLength={2} maxLength={120} value={profileName} onChange={(event) => setProfileName(event.target.value)} /></label><label>E-mail<input required type="email" value={profileEmail} onChange={(event) => setProfileEmail(event.target.value)} /></label><label>Adresa<input required maxLength={240} value={profileAddress} onChange={(event) => setProfileAddress(event.target.value)} /></label><label>Nové heslo<input type="password" minLength={8} placeholder="Ponechte prázdné, pokud ho neměníte" value={profilePassword} onChange={(event) => setProfilePassword(event.target.value)} /></label><button className="pay-button payment-submit" type="submit" disabled={profileSaving}>{profileSaving ? "Ukládám..." : "Uložit profil"}</button></form></section></div>}
      {noticeOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setNoticeOpen(null)}><section className="payment-modal compact-modal" role="dialog" aria-modal="true" aria-labelledby="notice-title"><button className="modal-close" onClick={() => setNoticeOpen(null)} aria-label="Zavřít"><X size={21} /></button><p className="modal-kicker">{noticeOpen === "notifications" ? "Oznámení" : "Přihlášení"}</p><h2 id="notice-title">{noticeOpen === "notifications" ? "Vše je v pořádku" : "Odhlásit se?"}</h2><p className="modal-copy">{noticeOpen === "notifications" ? "Nemáte žádná nová oznámení." : "Pro další práci s účtem se budete muset znovu přihlásit."}</p><button className="pay-button payment-submit" onClick={() => { if (noticeOpen === "logout") { clearSession(); router.replace("/login"); } else setNoticeOpen(null); }}>{noticeOpen === "logout" ? "Odhlásit se" : "Rozumím"}</button></section></div>}
    </div>
  );
}
