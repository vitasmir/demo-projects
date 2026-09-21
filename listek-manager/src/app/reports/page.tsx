"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Reports from "./Reports";
import Header from "../Header";
import { Dashboard, getDashboard } from "@/lib/api";

import { CircleDollarSign, ClipboardCheck, LayoutDashboard, ShieldCheck, Users, WalletCards } from "lucide-react";

export default function ReportsPage() {
  const router = useRouter();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [adminUser] = useState(() => typeof window === "undefined" ? "" : localStorage.getItem("listek-admin-user") ?? "");

  useEffect(() => {
    if (!localStorage.getItem("listek-admin-session")) router.replace("/");
    else {
      getDashboard().then(setDashboard).catch(() => undefined);
    }
  }, [router]);

  if (typeof window !== "undefined" && !localStorage.getItem("listek-admin-session")) return null;
  function signOut() {
    localStorage.removeItem("listek-admin-session");
    localStorage.removeItem("listek-admin-user");
    localStorage.removeItem("listek-admin-must-change");
    router.replace("/");
  }

  const navigation = [
    { href: "/overview", label: "Přehled", icon: LayoutDashboard },
    { href: "/loans", label: "Půjčky", icon: CircleDollarSign },
    { href: "/reports", label: "Reporty", icon: ClipboardCheck },
    { href: "/overdrafts", label: "Kontokorenty", icon: WalletCards },
    { href: "/clients", label: "Klienti", icon: Users },
    { href: "/settings", label: "Nastavení sazeb", icon: ClipboardCheck },
    { href: "/admins", label: "Administrátoři", icon: ShieldCheck },
  ];

  return (
    <div className="admin-app">
      <aside className="admin-sidebar">
        <div className="brand"><span className="brand-mark"><i /></span><span>Lístek<small>Manager</small></span></div>
        <p className="nav-label">Pracovní prostor</p>
        <nav>{navigation.map(({ href, label, icon: Icon }) => <button className={href === "/reports" ? "active" : ""} key={href} onClick={() => router.push(href)}><Icon size={19} /><span>{label}</span>{(href === "/overview" || href === "/loans" || href === "/overdrafts") && <b>{href === "/loans" ? dashboard?.pendingLoans ?? 0 : href === "/overdrafts" ? dashboard?.pendingOverdrafts ?? 0 : (dashboard?.pendingLoans ?? 0) + (dashboard?.pendingOverdrafts ?? 0)}</b>}</button>)}</nav>
        <div className="sidebar-foot"><ShieldCheck size={18} /><span>Zabezpečená administrace<small>Produkční prostředí</small></span></div>
      </aside>
      <main className="admin-main">
        <Header adminUser={adminUser} onSignOut={signOut} />
        <div className="admin-content">
          <section className="page-heading"><div><p>27. SRPNA 2026</p><h1>Reporty</h1><span>Sledujte schválení, splácení a zůstatky půjček.</span></div></section>
          <Reports />
        </div>
      </main>
    </div>
  );
}
