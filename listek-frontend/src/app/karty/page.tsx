"use client";

import {
  ArrowRight,
  Check,
  CreditCard,
  Eye,
  EyeOff,
  LockKeyhole,
  Settings,
  ShieldCheck,
  Wifi,
  X,
} from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import BankShell from "../BankShell";
import {
  Account,
  BankCard,
  createCard,
  getAccounts,
  getCards,
  updateCard,
} from "../../lib/api";

export default function CardsPage() {
  const [cards, setCards] = useState<BankCard[]>([]);
  const [account, setAccount] = useState<Account | null>(null);
  const [accountId, setAccountId] = useState("");
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState("");
  const [saving, setSaving] = useState(false);
  const [locked, setLocked] = useState(false);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [modal, setModal] = useState<"limits" | "order" | null>(null);
  const [orderSent, setOrderSent] = useState(false);
  const [onlinePayments, setOnlinePayments] = useState(true);
  const [inStorePayments, setInStorePayments] = useState(true);
  const [cashWithdrawals, setCashWithdrawals] = useState(true);
  const [paymentLimit, setPaymentLimit] = useState(50000);
  const [onlinePaymentLimit, setOnlinePaymentLimit] = useState(30000);
  const [withdrawalLimit, setWithdrawalLimit] = useState(10000);

  const activeCard = cards[0];

  useEffect(() => {
    getAccounts()
      .then((accounts) => {
        const account = accounts[0];
        if (!account) throw new Error("Nebyl nalezen žádný účet.");
        setAccount(account);
        setAccountId(account.id);
        return getCards(account.id);
      })
      .then((loadedCards) => {
        setCards(loadedCards);
        const card = loadedCards[0];
        if (card) {
          setLocked(card.locked);
          setPaymentLimit(card.paymentLimit);
          setOnlinePaymentLimit(card.onlinePaymentLimit);
          setWithdrawalLimit(card.withdrawalLimit);
          setOnlinePayments(card.onlinePayments);
          setInStorePayments(card.inStorePayments);
          setCashWithdrawals(card.cashWithdrawals);
        }
      })
      .catch((error) =>
        setApiError(
          error instanceof Error
            ? error.message
            : "Karty se nepodařilo načíst.",
        ),
      )
      .finally(() => setLoading(false));
  }, []);

  async function saveSettings(
    overrides: Partial<
      Pick<
        BankCard,
        | "locked"
        | "paymentLimit"
        | "onlinePaymentLimit"
        | "withdrawalLimit"
        | "onlinePayments"
        | "inStorePayments"
        | "cashWithdrawals"
      >
    > = {},
  ) {
    if (!activeCard && !accountId) return false;
    setSaving(true);
    try {
      const card = activeCard ?? (await createCard(accountId));
      const savedCard = await updateCard(card.id, {
        locked: overrides.locked ?? locked,
        paymentLimit: overrides.paymentLimit ?? paymentLimit,
        onlinePaymentLimit: overrides.onlinePaymentLimit ?? onlinePaymentLimit,
        withdrawalLimit: overrides.withdrawalLimit ?? withdrawalLimit,
        onlinePayments: overrides.onlinePayments ?? onlinePayments,
        inStorePayments: overrides.inStorePayments ?? inStorePayments,
        cashWithdrawals: overrides.cashWithdrawals ?? cashWithdrawals,
      });
      setCards((currentCards) =>
        currentCards.some((currentCard) => currentCard.id === savedCard.id)
          ? currentCards.map((currentCard) =>
              currentCard.id === savedCard.id ? savedCard : currentCard,
            )
          : [savedCard, ...currentCards],
      );
      setLocked(savedCard.locked);
      setPaymentLimit(savedCard.paymentLimit);
      setOnlinePaymentLimit(savedCard.onlinePaymentLimit);
      setWithdrawalLimit(savedCard.withdrawalLimit);
      setOnlinePayments(savedCard.onlinePayments);
      setInStorePayments(savedCard.inStorePayments);
      setCashWithdrawals(savedCard.cashWithdrawals);
      setApiError("");
      return true;
    } catch (error) {
      setApiError(
        error instanceof Error
          ? error.message
          : "Nastavení karty se nepodařilo uložit.",
      );
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function submitOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accountId) return;
    setSaving(true);
    try {
      const newCard = await createCard(accountId);
      setCards((currentCards) => [newCard, ...currentCards]);
      setOrderSent(true);
      setApiError("");
    } catch (error) {
      setApiError(
        error instanceof Error
          ? error.message
          : "Kartu se nepodařilo objednat.",
      );
    } finally {
      setSaving(false);
    }
  }

  function closeModal() {
    setModal(null);
    setOrderSent(false);
  }

  async function submitCardSettings(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (await saveSettings()) closeModal();
  }

  return (
    <BankShell>
      <div className="bank-content section-page">
        {loading && <p className="api-notice">Načítám karty z databáze...</p>}
        {apiError && <p className="api-notice">{apiError}</p>}
        <div className="page-hero">
          <div>
            <p className="date-label">Plaťte podle sebe</p>
            <h1>Moje karty</h1>
            <p className="page-lead">
              Karty máte pod kontrolou. Včetně limitů, bezpečnosti a nastavení.
            </p>
          </div>
          <button className="pay-button" onClick={() => setModal("order")}>
            <CreditCard size={18} /> Objednat kartu
          </button>
        </div>
        <div className="cards-page-grid">
          <article className={`card-large ${locked ? "is-locked" : ""}`}>
            <div className="card-chip" />
            <Wifi className="card-contactless" size={19} />
            <span className="card-brand">Lístek</span>
            <p>••••&nbsp; ••••&nbsp; ••••&nbsp; 2841</p>
            <div className="card-owner">
              <span>{account?.ownerName ?? "Načítám..."}</span>
              <b>VISA</b>
            </div>
            {locked && (
              <div className="card-lock-label">
                <LockKeyhole size={15} /> Zamknuto
              </div>
            )}
          </article>
          <section className="card-info">
            <p className={`card-status ${locked ? "locked" : ""}`}>
              <i /> {locked ? "Dočasně zamknutá" : "Aktivní karta"}
            </p>
            <h2>Visa Classic</h2>
            <p>Platná do 08/29</p>
            <div className="card-number-row">
              <span>Číslo karty</span>
              <b>•••• 2841</b>
            </div>
            <button
              className="text-button"
              onClick={() => setDetailsVisible((visible) => !visible)}
            >
              {detailsVisible ? "Skrýt detaily" : "Zobrazit detaily"}{" "}
              {detailsVisible ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            {detailsVisible && (
              <div className="card-details">
                <span>
                  Držitel<b>{account?.ownerName ?? "Načítám..."}</b>
                </span>
                <span>
                  Účet<b>{account?.accountNumber ?? "Načítám..."}</b>
                </span>
                <span>
                  Typ<b>Debetní karta</b>
                </span>
              </div>
            )}
          </section>
        </div>
        <div className="card-tools">
          <button
            onClick={() => {
              const nextLocked = !locked;
              setLocked(nextLocked);
              void saveSettings({ locked: nextLocked });
            }}
            disabled={saving}
          >
            <span>
              <LockKeyhole size={20} />
            </span>
            <strong>{locked ? "Odemknout kartu" : "Dočasně zamknout"}</strong>
            <small>
              {locked
                ? "Kartu znovu aktivujete jedním kliknutím."
                : "Kartu můžete kdykoliv odemknout."}
            </small>
          </button>
          <button onClick={() => setModal("limits")} disabled={saving}>
            <span>
              <Settings size={20} />
            </span>
            <strong>Limity a nastavení</strong>
            <small>Upravte platby na internetu i výběry.</small>
          </button>
        </div>
        {modal && (
          <div className="modal-backdrop" role="presentation">
            <section
              className="payment-modal card-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="card-modal-title"
            >
              <button
                className="modal-close"
                type="button"
                onClick={closeModal}
                aria-label="Zavřít"
              >
                <X size={21} />
              </button>
              {modal === "limits" ? (
                <form onSubmit={submitCardSettings}>
                  <p className="modal-kicker">Nastavení karty</p>
                  <h2 id="card-modal-title">Limity a bezpečnost</h2>
                  <div className="card-limit-list">
                    <label>
                      <span>
                        <strong>Platby kartou</strong>
                        <small>Maximálně za den</small>
                      </span>
                      <input
                        type="number"
                        value={paymentLimit}
                        onChange={(event) =>
                          setPaymentLimit(Number(event.target.value))
                        }
                        min="0"
                        step="1"
                      />{" "}
                      Kč
                    </label>
                    <label>
                      <span>
                        <strong>Platby na internetu</strong>
                        <small>Maximálně za den</small>
                      </span>
                      <input
                        type="number"
                        value={onlinePaymentLimit}
                        onChange={(event) =>
                          setOnlinePaymentLimit(Number(event.target.value))
                        }
                        min="0"
                        step="1"
                      />{" "}
                      Kč
                    </label>
                    <label>
                      <span>
                        <strong>Výběry z bankomatu</strong>
                        <small>Maximálně za den</small>
                      </span>
                      <input
                        type="number"
                        value={withdrawalLimit}
                        onChange={(event) =>
                          setWithdrawalLimit(Number(event.target.value))
                        }
                        min="0"
                        step="1"
                      />{" "}
                      Kč
                    </label>
                  </div>
                  <div className="card-switches">
                    <button
                      className={inStorePayments ? "is-on" : ""}
                      type="button"
                      onClick={() => setInStorePayments((value) => !value)}
                    >
                      <span>{inStorePayments ? <Check size={14} /> : null}</span>
                      <b>Placení v obchodech</b>
                    </button>
                    <button
                      className={onlinePayments ? "is-on" : ""}
                      type="button"
                      onClick={() => setOnlinePayments((value) => !value)}
                    >
                      <span>{onlinePayments ? <Check size={14} /> : null}</span>
                      <b>Platby na internetu</b>
                    </button>
                    <button
                      className={cashWithdrawals ? "is-on" : ""}
                      type="button"
                      onClick={() => setCashWithdrawals((value) => !value)}
                    >
                      <span>
                        {cashWithdrawals ? <Check size={14} /> : null}
                      </span>
                      <b>Výběry z bankomatů</b>
                    </button>
                  </div>
                  <button
                    className="pay-button payment-submit"
                    type="submit"
                    disabled={saving}
                  >
                    Uložit nastavení
                  </button>
                  <p className="secure-note">
                    <ShieldCheck size={16} /> Změny se projeví okamžitě.
                  </p>
                </form>
              ) : orderSent ? (
                <div className="payment-success">
                  <span>
                    <Check size={30} />
                  </span>
                  <h2 id="card-modal-title">Karta je na cestě</h2>
                  <p>Novou kartu vám doručíme do 5 pracovních dnů.</p>
                  <button className="pay-button" onClick={closeModal}>
                    Hotovo
                  </button>
                </div>
              ) : (
                <>
                  <p className="modal-kicker">Nová karta</p>
                  <h2 id="card-modal-title">Objednat kartu</h2>
                  <p className="card-order-copy">
                    Visa Classic k běžnému účtu bez měsíčního poplatku.
                  </p>
                  <form onSubmit={submitOrder}>
                    <label>
                      Adresa doručení
                      <input required defaultValue="Dlouhá 12, Praha 1" />
                    </label>
                    <label>
                      Typ doručení
                      <select defaultValue="standard">
                        <option value="standard">Standardní (zdarma)</option>
                        <option value="express">Expresní (99 Kč)</option>
                      </select>
                    </label>
                    <button
                      className="pay-button payment-submit"
                      type="submit"
                      disabled={saving}
                    >
                      Objednat kartu <ArrowRight size={18} />
                    </button>
                  </form>
                </>
              )}
            </section>
          </div>
        )}
      </div>
    </BankShell>
  );
}
