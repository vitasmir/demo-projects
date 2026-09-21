"use client";

import { FormEvent, startTransition, useEffect, useState } from "react";
import Link from "next/link";

type Fruit = { id: number; name: string; color: string; weight: number };
const starterFruits: Fruit[] = [
    { id: 1, name: "Jablko", color: "Červená", weight: 180 },
    { id: 2, name: "Banán", color: "Žlutá", weight: 120 },
    { id: 3, name: "Švestka", color: "Fialová", weight: 65 },
    { id: 4, name: "Pomeranč", color: "Oranžová", weight: 210 },
];
const colorMap: Record<string, string> = {
    Červená: "#ef634b",
    Žlutá: "#f4dd46",
    Fialová: "#b99add",
    Oranžová: "#ff9b4a",
    Zelená: "#86c76b",
};

export default function FruitsPage() {
    const [fruits, setFruits] = useState<Fruit[]>(starterFruits);
    const [query, setQuery] = useState("");
    const [editingId, setEditingId] = useState<number | null>(null);
    const [name, setName] = useState("");
    const [color, setColor] = useState("Zelená");
    const [weight, setWeight] = useState(100);
    async function loadFruits() {
        const response = await fetch("/api/fruits", { cache: "no-store" });
        if (!response.ok) throw new Error("Nepodařilo se načíst ovoce.");
        const nextFruits = (await response.json()) as Fruit[];
        startTransition(() => setFruits(nextFruits));
    }

    useEffect(() => {
        loadFruits().catch(() => undefined);
        const refreshTimer = window.setInterval(() => {
            loadFruits().catch(() => undefined);
        }, 3000);
        return () => window.clearInterval(refreshTimer);
    }, []);
    const visibleFruits = fruits.filter((fruit) =>
        fruit.name.toLowerCase().includes(query.toLowerCase()),
    );
    function resetForm() {
        setEditingId(null);
        setName("");
        setColor("Zelená");
        setWeight(100);
    }
    async function submitFruit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!name.trim() || weight <= 0) return;
        const method = editingId === null ? "POST" : "PUT";
        const body = editingId === null
            ? { name: name.trim(), color, weight }
            : { id: editingId, name: name.trim(), color, weight };
        const response = await fetch("/api/fruits", {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
        if (!response.ok) return;
        await loadFruits();
        resetForm();
    }
    function editFruit(fruit: Fruit) {
        setEditingId(fruit.id);
        setName(fruit.name);
        setColor(fruit.color);
        setWeight(fruit.weight);
    }
    async function deleteFruit(id: number) {
        const response = await fetch(`/api/fruits?id=${id}`, { method: "DELETE" });
        if (!response.ok) return;
        await loadFruits();
        if (editingId === id) resetForm();
    }
    return (
        <main className="fruits-shell">
            <nav className="topbar">
                <Link className="brand" href="/">
                    FRUIT<span>DESK</span>
                </Link>
                <Link className="nav-link" href="/">
                    Hello World <span aria-hidden="true">-&gt;</span>
                </Link>
            </nav>
            <header className="fruits-header">
                <div>
                    <p className="section-kicker">COLLECTION / 001</p>
                    <h1>
                        Seznam
                        <br />
                        <em>ovoce.</em>
                    </h1>
                </div>
                <p className="header-note">
                    Evidence barevného skladu. Každý kus má své jméno, barvu a přesnou
                    hmotnost.
                </p>
            </header>
            <section className="fruit-layout">
                <div className="fruit-list-panel">
                    <div className="panel-heading">
                        <span>Všechny položky</span>
                        <span className="count">{fruits.length} ks</span>
                    </div>
                    <input
                        className="search-input"
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Hledat ovoce..."
                        aria-label="Hledat ovoce"
                    />
                    {visibleFruits.length ? (
                        <table className="fruit-table">
                            <thead>
                                <tr>
                                    <th>Název</th>
                                    <th>Barva</th>
                                    <th>Hmotnost</th>
                                    <th>Akce</th>
                                </tr>
                            </thead>
                            <tbody>
                                {visibleFruits.map((fruit) => (
                                    <tr key={fruit.id}>
                                        <td>
                                            <strong>{fruit.name}</strong>
                                        </td>
                                        <td>
                                            <span className="color-cell">
                                                <span
                                                    className="swatch"
                                                    style={{
                                                        backgroundColor: colorMap[fruit.color] ?? "#ccc",
                                                    }}
                                                />
                                                {fruit.color}
                                            </span>
                                        </td>
                                        <td>{fruit.weight} g</td>
                                        <td>
                                            <button
                                                className="action-link"
                                                onClick={() => editFruit(fruit)}
                                            >
                                                Upravit
                                            </button>
                                            <button
                                                className="action-link delete"
                                                onClick={() => deleteFruit(fruit.id)}
                                            >
                                                Smazat
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <p className="empty-state">Nic tu není. Přidej první ovoce.</p>
                    )}
                </div>
                <div className="fruit-form-panel">
                    <div className="panel-heading">
                        <span>{editingId === null ? "Nové ovoce" : "Upravit ovoce"}</span>
                    </div>
                    <form className="fruit-form" onSubmit={submitFruit}>
                        <label htmlFor="fruit-name">Název</label>
                        <input
                            id="fruit-name"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Např. Hruška"
                            required
                        />
                        <label htmlFor="fruit-color">Barva</label>
                        <select
                            id="fruit-color"
                            value={color}
                            onChange={(event) => setColor(event.target.value)}
                        >
                            {Object.keys(colorMap).map((option) => (
                                <option key={option}>{option}</option>
                            ))}
                        </select>
                        <label htmlFor="fruit-weight">Hmotnost v gramech</label>
                        <input
                            id="fruit-weight"
                            type="number"
                            min="1"
                            value={weight}
                            onChange={(event) => setWeight(Number(event.target.value))}
                            required
                        />
                        <div className="form-actions">
                            <button className="submit-button" type="submit">
                                {editingId === null ? "Přidat ovoce" : "Uložit změny"}
                            </button>
                            {editingId !== null && (
                                <button
                                    className="cancel-button"
                                    type="button"
                                    onClick={resetForm}
                                >
                                    Zrušit
                                </button>
                            )}
                        </div>
                    </form>
                </div>
            </section>
        </main>
    );
}
