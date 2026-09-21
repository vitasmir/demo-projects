"use client";

import { ArrowRight, Landmark, ShieldCheck } from "lucide-react";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { login, register } from "../../lib/api";
import { getSession, setSession } from "../../lib/session";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function hashPassword(password: string, username: string) {
    const encoder = new TextEncoder();
    const salt = encoder.encode(`listek-password-salt:${username.trim().toLocaleLowerCase("cs-CZ")}`);
    const keyMaterial = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
    const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", salt, iterations: 120000, hash: "SHA-256" }, keyMaterial, 256);
    return Array.from(new Uint8Array(bits), (byte) => byte.toString(16).padStart(2, "0")).join("");
  }

  useEffect(() => {
    if (getSession()) router.replace("/");
  }, [router]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      const username = String(form.get("username")).trim();
      const password = String(form.get("password"));
      const passwordConfirmation = String(form.get("passwordConfirmation") ?? "");
      if (mode === "register" && password !== passwordConfirmation) {
        setError("Hesla se neshodují.");
        return;
      }
      const hashedPassword = await hashPassword(password, username);
      if (mode === "register") {
        const account = await register({
          username,
          firstName: String(form.get("firstName")).trim(),
          lastName: String(form.get("lastName")).trim(),
          birthNumber: String(form.get("birthNumber")).trim(),
          email: String(form.get("email")).trim(),
          street: String(form.get("street")).trim(),
          city: String(form.get("city")).trim(),
          postalCode: String(form.get("postalCode")).trim(),
          password: hashedPassword,
        });
        setSession(account);
        router.replace("/");
        return;
      }
      const account = await login({ username, password: hashedPassword });
      setSession(account);
      router.replace("/");
    } catch (submissionError) {
      setError(mode === "login" ? "Uživatelské jméno nebo heslo nesedí. Zkontrolujte zadané údaje." : submissionError instanceof Error ? submissionError.message : "Požadavek se nepodařilo dokončit.");
    } finally {
      setSubmitting(false);
    }
  }

  function changeMode(nextMode: "login" | "register") {
    setMode(nextMode);
    setError("");
  }

  return <main className="auth-page"><section className="auth-brand"><div className="bank-logo"><span className="logo-mark"><span /></span><span>Lístek</span></div><div><p className="date-label">Internetové bankovnictví</p><h1>Vaše peníze.<br />Jednoduše.</h1><p>Bezpečný přístup k účtům, platbám a spoření na jednom místě.</p></div><span className="auth-security"><ShieldCheck size={18} /> Zabezpečené přihlášení</span></section><section className="auth-panel"><div className="auth-card"><span className="auth-icon"><Landmark size={24} /></span><h2>{mode === "login" ? "Přihlášení" : "Založení účtu"}</h2><p>{mode === "login" ? "Vítejte zpět v bance Lístek." : "Účet vám založíme během chvíle."}</p><div className="auth-tabs" role="tablist"><button className={mode === "login" ? "active" : ""} onClick={() => changeMode("login")} type="button">Přihlásit se</button><button className={mode === "register" ? "active" : ""} onClick={() => changeMode("register")} type="button">Registrovat</button></div>{error && <p className="api-notice">{error}</p>}<form onSubmit={submit}><label>Uživatelské jméno<input name="username" required maxLength={80} autoComplete="username" /></label>{mode === "register" && <><div className="auth-fields"><label>Jméno<input name="firstName" required maxLength={80} autoComplete="given-name" /></label><label>Příjmení<input name="lastName" required maxLength={80} autoComplete="family-name" /></label></div><label>Rodné číslo<input name="birthNumber" required pattern="[0-9]{6}/?[0-9]{3,4}" placeholder="123456/7890" autoComplete="off" /></label><label>E-mail<input name="email" required type="email" autoComplete="email" /></label><div className="auth-fields"><label>Ulice a číslo<input name="street" required maxLength={120} autoComplete="street-address" /></label><label>Město<input name="city" required maxLength={100} autoComplete="address-level2" /></label></div><label>PSČ<input name="postalCode" required pattern="[0-9]{3} ?[0-9]{2}" placeholder="110 00" autoComplete="postal-code" /></label></>}<label>Heslo<input name="password" required type="password" minLength={mode === "register" ? 12 : undefined} autoComplete={mode === "login" ? "current-password" : "new-password"} /></label>{mode === "register" && <label>Heslo znovu<input name="passwordConfirmation" required type="password" minLength={12} autoComplete="new-password" /></label>}<button className="pay-button payment-submit" type="submit" disabled={submitting}>{submitting ? "Zpracovávám..." : mode === "login" ? "Přihlásit se" : "Vytvořit účet"}<ArrowRight size={18} /></button></form></div></section></main>;
}