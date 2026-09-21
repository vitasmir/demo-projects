import Link from "next/link";

export default function Home() {
  return (
    <main className="home-shell">
      <nav className="topbar">
        <Link className="brand" href="/">
          FRUIT<span>DESK</span>
        </Link>
        <Link className="nav-link" href="/fruits">
          Ovoce <span aria-hidden="true">-&gt;</span>
        </Link>
      </nav>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">NEXT.JS / APP ROUTER / CRUD</p>
          <h1>
            Hello,
            <br />
            <em>world.</em>
          </h1>
          <p className="hero-text">
            Malý pracovní prostor pro velké chutě. Prohlédni si seznam ovoce a
            spravuj jeho údaje.
          </p>
          <Link className="primary-button" href="/fruits">
            Otevřít sklad ovoce <span aria-hidden="true">-&gt;</span>
          </Link>
        </div>
        <div
          className="hero-orchard"
          aria-label="Barevné ovocné kruhy"
          role="img"
        >
          <div className="fruit-orb orb-lemon">LEMON</div>
          <div className="fruit-orb orb-apple">APPLE</div>
          <div className="fruit-orb orb-plum">PLUM</div>
          <span className="hero-stamp">
            HELLO
            <br />
            WORLD
            <br />
            <b>01</b>
          </span>
        </div>
      </section>
    </main>
  );
}
