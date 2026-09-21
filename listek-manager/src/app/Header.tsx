"use client";

import { FormEvent, useState } from "react";
import { Bell, LogOut, Menu, X } from "lucide-react";
import { AdminProfile, changeAdminPassword, getAdminProfile, updateAdminProfile } from "@/lib/api";

type HeaderProps = {
  adminUser: string;
  onSignOut: () => void;
  onMenuOpen?: () => void;
};

async function hashPassword(password: string, username: string) {
  const encoder = new TextEncoder();
  const salt = encoder.encode(`listek-password-salt:${username.trim().toLocaleLowerCase("cs-CZ")}`);
  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt, iterations: 120000, hash: "SHA-256" }, key, 256);
  return Array.from(new Uint8Array(bits), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export default function Header({ adminUser, onSignOut, onMenuOpen }: HeaderProps) {
  const [profile, setProfile] = useState<AdminProfile | null>(null);
  const [profileOpen, setProfileOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [profileMessage, setProfileMessage] = useState("");
  const [passwordMessage, setPasswordMessage] = useState("");
  const [error, setError] = useState("");

  async function openProfile() {
    setProfileOpen(true); setLoading(true); setProfileMessage(""); setPasswordMessage(""); setError("");
    try { setProfile(await getAdminProfile()); }
    catch (profileError) { setError(profileError instanceof Error ? profileError.message : "Profil se nepodařilo načíst."); }
    finally { setLoading(false); }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true); setProfileMessage(""); setError("");
    try {
      const updated = await updateAdminProfile({
        firstName: String(form.get("firstName")).trim(), lastName: String(form.get("lastName")).trim(),
        birthNumber: String(form.get("birthNumber")).trim(), email: String(form.get("email")).trim(),
        street: String(form.get("street")).trim(), city: String(form.get("city")).trim(),
        postalCode: String(form.get("postalCode")).trim(),
      });
      setProfile(updated); setProfileMessage("Osobní údaje byly úspěšně aktualizovány.");
    } catch (profileError) { setError(profileError instanceof Error ? profileError.message : "Údaje se nepodařilo uložit."); }
    finally { setSaving(false); }
  }

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const password = String(form.get("newPassword"));
    if (password !== String(form.get("passwordConfirmation"))) { setError("Hesla se neshodují."); return; }
    setSaving(true); setPasswordMessage(""); setError("");
    try {
      await changeAdminPassword(await hashPassword(password, adminUser));
      formElement.reset(); setPasswordMessage("Přihlašovací heslo bylo změněno.");
    } catch (passwordError) { setError(passwordError instanceof Error ? passwordError.message : "Heslo se nepodařilo změnit."); }
    finally { setSaving(false); }
  }

  return <>
    <header className="topbar">
      {onMenuOpen && <button className="menu-button" onClick={onMenuOpen} aria-label="Otevřít nabídku"><Menu /></button>}
      <div className="environment"><i /> Systémy v pořádku</div>
      <button className="current-user" type="button" onClick={openProfile}>{adminUser}</button>
      <button className="icon-button" aria-label="Oznámení"><Bell size={19} /><span /></button>
      <button className="logout-button" type="button" onClick={() => setLogoutOpen(true)}><LogOut size={17} /> Odhlášení</button>
    </header>
    {profileOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setProfileOpen(false); }}>
      <section className="admin-modal profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-title">
        <button className="modal-close" type="button" onClick={() => setProfileOpen(false)} aria-label="Zavřít profil"><X size={18} /></button>
        <p>PROFIL ADMINISTRÁTORA</p><h2 id="profile-title">{profile?.firstName || adminUser}</h2><span>Přihlašovací jméno: {profile?.username || adminUser}</span>
        {loading && <div className="loading">Načítám profil...</div>}
        {!loading && profile && <>
          <form className="profile-form" onSubmit={saveProfile}>
            <label>Jméno<input name="firstName" defaultValue={profile.firstName} required /></label>
            <label>Příjmení<input name="lastName" defaultValue={profile.lastName} required /></label>
            <label>Rodné číslo<input name="birthNumber" defaultValue={profile.birthNumber} required /></label>
            <label>E-mail<input name="email" type="email" defaultValue={profile.email} required /></label>
            <label>Ulice a číslo<input name="street" defaultValue={profile.street} required /></label>
            <label>Město<input name="city" defaultValue={profile.city} required /></label>
            <label>PSČ<input name="postalCode" defaultValue={profile.postalCode} required /></label>
            <button className="primary-button submit-button" disabled={saving}>Uložit osobní údaje</button>
          </form>
          {profileMessage && <div className="profile-message">{profileMessage}</div>}
          <form className="profile-password-form" onSubmit={savePassword}>
            <h3>Změna hesla</h3>
            <label>Nové heslo<input name="newPassword" type="password" minLength={12} required /></label>
            <label>Nové heslo znovu<input name="passwordConfirmation" type="password" minLength={12} required /></label>
            <button className="primary-button submit-button" disabled={saving}>Změnit heslo</button>
          </form>
          {passwordMessage && <div className="profile-message">{passwordMessage}</div>}
        </>}
        {error && <div className="notice">{error}</div>}
      </section>
    </div>}
    {logoutOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setLogoutOpen(false); }}><section className="admin-modal logout-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="logout-confirm-title"><button className="modal-close" type="button" onClick={() => setLogoutOpen(false)} aria-label="Zavřít potvrzení"><X size={18} /></button><p>ODHLÁŠENÍ</p><h2 id="logout-confirm-title">Opravdu se chcete odhlásit?</h2><span>Vaše administrátorská relace bude ukončena.</span><div className="logout-confirm-actions"><button className="reject" type="button" onClick={() => setLogoutOpen(false)}>Zrušit</button><button className="primary-button" type="button" onClick={onSignOut}><LogOut size={16} /> Odhlásit se</button></div></section></div>}
  </>;
}