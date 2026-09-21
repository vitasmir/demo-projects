"use client";

import { ArrowRight, Download, FileText, Search } from "lucide-react";
import { useDeferredValue, useState } from "react";
import BankShell from "../BankShell";

const documents = [
  { id: "statement-08-2026", name: "Výpis z běžného účtu", date: "Srpen 2026", content: "Výpis z běžného účtu za srpen 2026\nStav dokumentu: uzavřený" },
  { id: "statement-07-2026", name: "Výpis z běžného účtu", date: "Červenec 2026", content: "Výpis z běžného účtu za červenec 2026\nStav dokumentu: uzavřený" },
  { id: "confirmation-2026", name: "Potvrzení o vedení účtu", date: "12. června 2026", content: "Banka Lístek potvrzuje vedení běžného účtu klienta." },
  { id: "contract-2025", name: "Smlouva o běžném účtu", date: "3. ledna 2025", content: "Smlouva o vedení běžného účtu u banky Lístek." },
  { id: "fees-2026", name: "Přehled poplatků", date: "1. ledna 2026", content: "Přehled poplatků za bankovní služby pro rok 2026." },
  { id: "tax-2025", name: "Potvrzení pro daňové účely", date: "31. prosince 2025", content: "Potvrzení o příjmech a výdajích na účtu za rok 2025." },
];

export default function DocumentsPage() {
  const [search, setSearch] = useState("");
  const [showAll, setShowAll] = useState(false);
  const deferredSearch = useDeferredValue(search);
  const filtered = documents.filter((document) => `${document.name} ${document.date}`.toLocaleLowerCase("cs").includes(deferredSearch.toLocaleLowerCase("cs")));
  const displayed = showAll || deferredSearch ? filtered : filtered.slice(0, 4);

  function download(document: typeof documents[number]) {
    const blob = new Blob([`${document.name}\n${document.date}\n\n${document.content}\n`], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = window.document.createElement("a");
    link.href = url;
    link.download = `${document.id}.txt`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return <BankShell><div className="bank-content section-page">
    <div className="page-hero"><div><p className="date-label">Vše důležité po ruce</p><h1>Dokumenty</h1><p className="page-lead">Výpisy, smlouvy a potvrzení bezpečně na jednom místě.</p></div></div>
    <section className="documents-panel"><div className="section-heading"><div><h2>Moje dokumenty</h2><p>{filtered.length} {filtered.length === 1 ? "dokument" : "dokumentů"}</p></div><label className="transaction-search"><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Hledat" aria-label="Hledat dokumenty" /></label></div><div className="document-list">{displayed.map((document) => <button className="document-row" key={document.id} onClick={() => download(document)} title={`Stáhnout dokument: ${document.name}`} aria-label={`Stáhnout dokument: ${document.name}`}><span className="document-icon"><FileText size={19} /></span><span><strong>{document.name}</strong><small>{document.date}</small></span><b>TXT</b><Download size={17} /></button>)}{displayed.length === 0 && <p className="bank-empty">Hledání neodpovídá žádnému dokumentu.</p>}</div>{!deferredSearch && filtered.length > 4 && <button className="all-transactions" onClick={() => setShowAll((current) => !current)} title={showAll ? "Zobrazit méně dokumentů" : "Zobrazit všechny dokumenty"}>{showAll ? "Zobrazit méně" : "Zobrazit všechny dokumenty"} <ArrowRight size={16} /></button>}</section>
  </div></BankShell>;
}
