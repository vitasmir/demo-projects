import { Account } from "./api";

const SESSION_KEY = "listek-session";

export function getSession(): Account | null {
  if (typeof window === "undefined") return null;
  const value = localStorage.getItem(SESSION_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value) as Account;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function setSession(account: Account) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(account));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}