const pillars = [
  {
    title: "React Frontend",
    body: "The public UI entry point. This is the first shippable base for Autoforge.",
  },
  {
    title: "Private Backend",
    body: "Domain logic and internal APIs stay behind the gateway on the private OCI node.",
  },
  {
    title: "AI Worker Loop",
    body: "Background automation and self-improvement tasks will live in the worker service.",
  },
];

function App() {
  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">Autoforge</p>
        <h1>React base is online.</h1>
        <p className="lead">
          A clean frontend starting point for the self-evolving application platform.
        </p>
      </section>

      <section className="status-grid" aria-label="Platform pillars">
        {pillars.map((pillar) => (
          <article className="card" key={pillar.title}>
            <p className="card-kicker">ready</p>
            <h2>{pillar.title}</h2>
            <p>{pillar.body}</p>
          </article>
        ))}
      </section>

      <section className="footnote">
        <p>Current scope: frontend base only. Routing, auth, API integration and deployment wiring come next.</p>
      </section>
    </main>
  );
}

export default App;
