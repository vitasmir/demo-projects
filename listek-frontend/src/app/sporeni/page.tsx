"use client";

import {
  ArrowRight,
  Pencil,
  Plus,
  TrendingUp,
  Trash2,
  X,
} from "lucide-react";
import { FormEvent, useEffect, useRef, useState } from "react";
import BankShell from "../BankShell";
import {
  Account,
  InterestSettings,
  createSavingsAccount,
  createStandingOrder,
  getAccounts,
  getInterestSettings,
  transferMoney,
} from "../../lib/api";
import { getSession } from "../../lib/session";

type Goal = { id: string; name: string; saved: number; target: number };
const demoGoals: Goal[] = [];

export default function SavingsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [goals, setGoals] = useState<Goal[]>(() => {
    if (typeof window === "undefined") return demoGoals;
    const stored = localStorage.getItem("listek-goals");
    return stored
      ? (JSON.parse(stored) as Goal[]).filter((goal) => goal.id !== "reserve")
      : demoGoals;
  });
  const [modal, setModal] = useState<
    "goal" | "transfer" | "order" | "manage" | null
  >(null);
  const [editingGoal, setEditingGoal] = useState<Goal | null>(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const [rates, setRates] = useState<InterestSettings | null>(null);
  const modalRef = useRef<HTMLElement>(null);

  useEffect(() => {
    Promise.all([getAccounts(), getInterestSettings()])
      .then(([loadedAccounts, loadedRates]) => {
        setAccounts(loadedAccounts);
        setRates(loadedRates);
      })
      .catch(() => setMessage("Účty se nepodařilo načíst."));
  }, []);

  useEffect(() => {
    if (!modal) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setModal(null);
      if (event.key !== "Tab") return;
      const focusable = modalRef.current?.querySelectorAll<HTMLElement>("button, input, select, textarea, a[href]");
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
    modalRef.current?.querySelector<HTMLElement>("input:not([disabled]), button")?.focus();
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [modal]);

  function persistGoals(nextGoals: Goal[]) {
    setGoals(nextGoals);
    localStorage.setItem("listek-goals", JSON.stringify(nextGoals));
  }

  function addGoal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const goal = {
      id: editingGoal?.id ?? crypto.randomUUID(),
      name: String(form.get("name")).trim(),
      saved: Number(form.get("saved") || 0),
      target: Number(form.get("target")),
    };
    persistGoals(
      editingGoal
        ? goals.map((item) => (item.id === editingGoal.id ? goal : item))
        : [...goals, goal],
    );
    setEditingGoal(null);
    setModal(null);
    setMessage(
      editingGoal ? "Spořicí cíl byl upraven." : "Spořicí cíl byl přidán.",
    );
  }

  async function submitStandingOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sourceAccount || !savingsAccount) return;
    setSaving(true);
    try {
      const form = new FormData(event.currentTarget);
      await createStandingOrder(sourceAccount.id, {
        targetAccountNumber: savingsAccount.accountNumber,
        amount: Number(form.get("amount")),
        description: String(form.get("description")).trim(),
        dayOfMonth: Number(form.get("dayOfMonth")),
      });
      setModal(null);
      setMessage("Trvalý příkaz pro spoření byl nastaven.");
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Trvalý příkaz se nepodařilo nastavit.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function openSavingsAccount() {
    if (!sourceAccount) return;
    setSaving(true);
    try {
      const createdAccount = await createSavingsAccount(sourceAccount.id);
      setAccounts((currentAccounts) => [...currentAccounts, createdAccount]);
      setMessage("Spořicí účet byl založen.");
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Spořicí účet se nepodařilo založit.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function submitTransfer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    if (!sourceAccount || !savingsAccount) {
      setMessage("Pro převod potřebujete druhý účet ve stejné měně.");
      return;
    }
    setSaving(true);
    try {
      const form = new FormData(event.currentTarget);
      await transferMoney({
        fromAccountId: sourceAccount.id,
        toAccountNumber: savingsAccount.accountNumber,
        amount: Number(form.get("amount")),
        description: "Převod na spoření",
      });
      const refreshed = await getAccounts();
      setAccounts(refreshed);
      setModal(null);
      setMessage("Peníze byly převedeny na spoření.");
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Převod se nepodařilo dokončit.",
      );
    } finally {
      setSaving(false);
    }
  }

  const session = getSession();
  const sourceAccount = accounts.find((account) => account.id === session?.id);
  const savingsAccount = accounts.find(
    (account) =>
      account.type === "SAVINGS" &&
      account.currency === sourceAccount?.currency,
  );

  return (
    <BankShell>
      <div className="bank-content section-page">
        {message && <p className="api-notice">{message}</p>}
        <div className="page-hero">
          <div>
            <p className="date-label">Nechte peníze růst</p>
            <h1>Spoření</h1>
            <p className="page-lead">
              Vaše cíle, rezerva i úroky přehledně na jednom místě.
            </p>
          </div>
        </div>
        <div className="savings-hero">
          <div className="savings-copy">
            <span className="savings-icon">
              <TrendingUp size={23} />
            </span>
            <p className="modal-kicker">Spořicí účet</p>
            {savingsAccount ? (
              <>
                <p className="account-number">
                  Číslo účtu: {savingsAccount.accountNumber}
                </p>
                <h2>
                  {savingsAccount.balance.toLocaleString("cs-CZ", {
                    style: "currency",
                    currency: savingsAccount.currency,
                  })}
                </h2>
                <p>
                  Úroková sazba{" "}
                  <b>
                    {(rates?.savingsRate ?? 4.2).toLocaleString("cs-CZ")} % p.
                    a.
                  </b>
                </p>
                <p className="account-status">
                  <i /> Aktivní účet
                </p>
                <div className="savings-actions">
                  <button
                    className="text-button"
                    onClick={() => setModal("transfer")}
                  >
                    Převést peníze <ArrowRight size={16} />
                  </button>
                  <button
                    className="text-button"
                    onClick={() => setModal("order")}
                  >
                    Nastavit pravidelné spoření <Plus size={16} />
                  </button>
                </div>
              </>
            ) : (
              <>
                <h2>Spořicí účet zatím nemáte</h2>
                <p>Založte si ho a začněte si odkládat peníze.</p>
                <button
                  className="pay-button"
                  onClick={() => void openSavingsAccount()}
                  disabled={saving || !sourceAccount}
                >
                  {saving ? "Zakládám..." : "Založit spořicí účet"}
                </button>
              </>
            )}
          </div>
        </div>
        {modal && (
          <div
            className="modal-backdrop"
            role="presentation"
          >
            <section
              ref={modalRef}
              className="payment-modal account-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="savings-dialog-title"
            >
              <button
                className="modal-close"
                onClick={() => setModal(null)}
                aria-label="Zavřít"
              >
                <X size={21} />
              </button>
              <p className="modal-kicker">Spoření</p>
              <h2 id="savings-dialog-title">
                {modal === "goal"
                  ? editingGoal
                    ? "Upravit cíl"
                    : "Nový cíl"
                  : modal === "transfer"
                    ? "Převést peníze"
                    : modal === "order"
                      ? "Pravidelné spoření"
                      : "Spravovat cíle"}
              </h2>
              {modal === "goal" && (
                <form onSubmit={addGoal}>
                  <label>
                    Název
                    <input
                      name="name"
                      required
                      maxLength={80}
                      defaultValue={editingGoal?.name}
                    />
                  </label>
                  <div className="payment-fields">
                    <label>
                      Naspořeno
                      <input
                        name="saved"
                        type="number"
                        min="0"
                        defaultValue={editingGoal?.saved ?? 0}
                      />
                    </label>
                    <label>
                      Cílová částka
                      <input
                        name="target"
                        type="number"
                        min="1"
                        required
                        defaultValue={editingGoal?.target}
                      />
                    </label>
                  </div>
                  <button className="pay-button payment-submit" type="submit">
                    {editingGoal ? "Uložit změny" : "Přidat cíl"}
                  </button>
                </form>
              )}
              {modal === "transfer" && (
                <form onSubmit={submitTransfer}>
                  <label>
                    Z účtu
                    <input
                      value={
                        sourceAccount?.accountNumber ?? "Účet není dostupný"
                      }
                      disabled
                    />
                  </label>
                  <label>
                    Na účet
                    <input
                      value={
                        savingsAccount?.accountNumber ??
                        "Druhý účet není dostupný"
                      }
                      disabled
                    />
                  </label>
                  <label>
                    Částka
                    <input
                      name="amount"
                      type="number"
                      min="0.01"
                      step="0.01"
                      required
                    />
                  </label>
                  <button
                    className="pay-button payment-submit"
                    type="submit"
                    disabled={saving || !savingsAccount}
                  >
                    {saving ? "Převádím..." : "Převést"}
                  </button>
                </form>
              )}
              {modal === "order" && (
                <form onSubmit={submitStandingOrder}>
                  <label>
                    Z účtu
                    <input
                      value={
                        sourceAccount?.accountNumber ?? "Účet není dostupný"
                      }
                      disabled
                    />
                  </label>
                  <label>
                    Na spořicí účet
                    <input
                      value={
                        savingsAccount?.accountNumber ?? "Účet není dostupný"
                      }
                      disabled
                    />
                  </label>
                  <div className="payment-fields">
                    <label>
                      Částka
                      <input
                        name="amount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        required
                      />
                    </label>
                    <label>
                      Den v měsíci
                      <input
                        name="dayOfMonth"
                        type="number"
                        min="1"
                        max="28"
                        defaultValue="1"
                        required
                      />
                    </label>
                  </div>
                  <label>
                    Popis
                    <input
                      name="description"
                      defaultValue="Pravidelné spoření"
                      required
                      maxLength={120}
                    />
                  </label>
                  <button
                    className="pay-button payment-submit"
                    type="submit"
                    disabled={saving || !sourceAccount || !savingsAccount}
                  >
                    {saving ? "Ukládám..." : "Nastavit příkaz"}
                  </button>
                </form>
              )}
              {modal === "manage" && (
                <div className="goal-management-list">
                  {goals.map((goal) => (
                    <div key={goal.id}>
                      <span>
                        <strong>{goal.name}</strong>
                        <small>
                          Cíl {goal.target.toLocaleString("cs-CZ")} Kč
                        </small>
                      </span>
                      <button
                        onClick={() => {
                          setEditingGoal(goal);
                          setModal("goal");
                        }}
                        aria-label={`Upravit ${goal.name}`}
                      >
                        <Pencil size={17} />
                      </button>
                      <button
                        onClick={() =>
                          persistGoals(
                            goals.filter(
                              (candidate) => candidate.id !== goal.id,
                            ),
                          )
                        }
                        aria-label={`Odstranit ${goal.name}`}
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>
                  ))}
                  {goals.length === 0 && (
                    <p className="modal-copy">Nemáte žádné cíle.</p>
                  )}
                </div>
              )}
            </section>
          </div>
        )}
      </div>
    </BankShell>
  );
}
