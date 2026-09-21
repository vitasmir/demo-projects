import { Mail, Phone, ShieldCheck } from "lucide-react";
import BankShell from "../BankShell";

export default function HelpPage() {
  return <BankShell><div className="bank-content section-page"><div className="page-hero"><div><p className="date-label">Jsme tu pro vás</p><h1>Pomoc a kontakt</h1><p className="page-lead">Vyberte způsob, jakým nás chcete kontaktovat.</p></div></div><div className="support-grid"><a href="tel:+420800123456"><span><Phone size={21} /></span><strong>Telefonní podpora</strong><small>+420 800 123 456, každý den 8:00–20:00</small></a><a href="mailto:podpora@listek.cz"><span><Mail size={21} /></span><strong>Napsat podpoře</strong><small>podpora@listek.cz</small></a><article><span><ShieldCheck size={21} /></span><strong>Blokace karty</strong><small>Kartu okamžitě zamknete v sekci Karty.</small></article></div></div></BankShell>;
}