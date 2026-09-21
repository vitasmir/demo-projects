"use client";

import { ArrowLeft, ArrowRight, ClipboardCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { LoanReport as LoanReportData, getLoanReport } from "@/lib/api";

const money = new Intl.NumberFormat("cs-CZ", { style: "currency", currency: "CZK", currencyDisplay: "narrowSymbol", maximumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("cs-CZ", { dateStyle: "medium", timeStyle: "short" });

export default function Reports() {
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const [reports, setReports] = useState<LoanReportData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!selectedReport) return;
    getLoanReport()
      .then(setReports)
      .catch(() => setError("Report se nepodařilo načíst."))
      .finally(() => setLoading(false));
  }, [selectedReport]);

  if (!selectedReport) return <section className="report-catalog">
    <button className="report-card" type="button" onClick={() => setSelectedReport("loans")}>
      <span className="report-card-icon"><ClipboardCheck size={24} /></span>
      <span className="report-card-copy"><strong>Report půjček</strong><small>Schválení, splácení, nedoplatky a údaje o žadateli</small></span>
      <ArrowRight size={20} />
    </button>
  </section>;

  if (loading) return <div className="loading">Načítám report...</div>;
  if (error) return <div className="notice">{error}</div>;

  return <section className="clients-panel loan-records-panel report-panel">
    <div className="panel-heading"><div><button className="report-back-button" type="button" onClick={() => setSelectedReport(null)}><ArrowLeft size={16} /> Všechny reporty</button><p>AUDIT A SPLÁCENÍ</p><h2>Report půjček</h2></div><span>{reports.length} žádostí</span></div>
    <div className="loan-records-table report-table"><div className="table-head"><span>Žadatel</span><span>Požadováno</span><span>Schválení</span><span>Splacení</span><span>Zůstatek</span><span>Stav</span></div>{reports.map((loan) => <article key={loan.id}><span><strong>{loan.clientName}</strong><small>{loan.clientEmail}</small><small>{loan.accountNumber}</small></span><span><strong>{money.format(loan.amount)}</strong><small>{loan.purpose}</small><small>{date.format(new Date(loan.requestedAt))}</small></span><span><strong>{loan.approvedBy ?? (loan.status === "APPROVED" ? "Neuveden" : "-")}</strong><small>{loan.approvedAt ? date.format(new Date(loan.approvedAt)) : loan.status === "APPROVED" ? "Schváleno před zavedením evidence" : "Neschváleno"}</small></span><span><strong>{loan.repaidAt ? date.format(new Date(loan.repaidAt)) : "-"}</strong><small>{money.format(loan.repaidAmount ?? 0)} splaceno</small></span><span><strong>{money.format(loan.remainingAmount ?? 0)}</strong><small>{loan.overdue ? "Nedoplatek po splatnosti" : loan.hasOutstandingBalance ? `${loan.remainingInstallments ?? 0} splátek zbývá` : "Bez nedoplatku"}</small></span><span className={`status ${loan.status.toLowerCase()}`}>{loan.status === "APPROVED" ? "Schváleno" : loan.status === "REJECTED" ? "Zamítnuto" : "Čeká"}</span></article>)}</div>
    {reports.length === 0 && <div className="empty-state"><ClipboardCheck size={25} /><strong>Report neobsahuje žádná data</strong><span>Po založení žádostí se zde zobrazí jejich audit.</span></div>}
  </section>;
}
