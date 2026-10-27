import { useEffect, useMemo, useState } from "react";

const DESKS = [
  {
    key: "marlow_demo_elm_oak_7f3a",
    name: "Elm & Oak Bakery",
    city: "Chandigarh",
    note: "Counter iPad key. Croissants, cakes, and the Saturday bun.",
  },
  {
    key: "marlow_demo_brightline_2c91",
    name: "Brightline Dental",
    city: "Patiala",
    note: "Front desk key. Cleanings, crowns, and the appointment book.",
  },
];

const TONES = ["warm", "brief", "accountable"];
const storageKey = "marlow.apiKey";

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const key = sessionStorage.getItem(storageKey);
  if (key) headers.set("X-Api-Key", key);
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(payload?.error || "The desk could not complete that.");
  }
  return payload;
}

function isoDay(value) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(value);
}

function formatDay(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    timeZone: "Asia/Kolkata",
  }).format(new Date(value));
}

function mood(score) {
  if (score >= 0.35) return ["warm", "Customers are warm"];
  if (score >= 0.1) return ["mixed", "Leaning fair"];
  if (score > -0.1) return ["mixed", "A mixed window"];
  return ["cold", "Needs a careful reply"];
}

export default function App() {
  const [key, setKey] = useState(() => sessionStorage.getItem(storageKey) || "");
  const [shop, setShop] = useState(null);
  const [booting, setBooting] = useState(Boolean(key));
  const [view, setView] = useState("overview");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!key) {
      setBooting(false);
      return;
    }
    let cancelled = false;
    api("/api/tenant")
      .then((tenant) => {
        if (!cancelled) setShop(tenant);
      })
      .catch((reason) => {
        sessionStorage.removeItem(storageKey);
        if (!cancelled) {
          setKey("");
          setError(reason.message);
        }
      })
      .finally(() => {
        if (!cancelled) setBooting(false);
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function openDesk(nextKey) {
    setError("");
    sessionStorage.setItem(storageKey, nextKey.trim());
    setKey(nextKey.trim());
    setBooting(true);
    setView("overview");
  }

  function leave() {
    sessionStorage.removeItem(storageKey);
    setKey("");
    setShop(null);
  }

  if (booting) {
    return <main className="main"><p>Opening the desk…</p></main>;
  }
  if (!shop) {
    return <Gate onOpen={openDesk} error={error} />;
  }
  return (
    <div className="shell">
      <aside className="rail">
        <div>
          <p className="eyebrow">Reply desk</p>
          <p className="word">Marlow</p>
        </div>
        <nav>
          {[
            ["overview", "Overview"],
            ["reviews", "Reviews"],
            ["upload", "CSV upload"],
          ].map(([id, label]) => (
            <button key={id} className={view === id ? "active" : ""} onClick={() => setView(id)}>
              {label}
            </button>
          ))}
        </nav>
        <div className="rail-foot">
          <strong>{shop.name}</strong>
          <div><small>{shop.city} · {shop.industry}</small></div>
          <span className="mode">{shop.aiMode === "openai" ? "Spring AI" : "On-desk reader"}</span>
          <div style={{ marginTop: 12 }}>
            <button className="ghost" onClick={leave}>Switch shop</button>
          </div>
        </div>
      </aside>
      <main className="main">
        {view === "overview" && <Overview shop={shop} />}
        {view === "reviews" && <Reviews />}
        {view === "upload" && <Uploads />}
      </main>
    </div>
  );
}

function Gate({ onOpen, error }) {
  const [custom, setCustom] = useState("");
  return (
    <div className="gate">
      <section className="gate-copy">
        <div>
          <p className="eyebrow">For neighborhood shops</p>
          <h1>Read the room. Answer like a person.</h1>
          <p className="lede">
            Marlow gathers customer reviews, sorts the mood and the subject, and leaves a polite draft on the desk. Each shop key sees only its own book.
          </p>
        </div>
        <div className="stamp">Two demo desks are already open</div>
      </section>
      <section className="gate-shops">
        <div className="shop-grid">
          {error && <div className="error">{error}</div>}
          {DESKS.map((desk) => (
            <button key={desk.key} className="shop-card" onClick={() => onOpen(desk.key)}>
              <p className="eyebrow">{desk.city}</p>
              <h2>{desk.name}</h2>
              <p>{desk.note}</p>
            </button>
          ))}
          <form className="manual" onSubmit={(event) => { event.preventDefault(); onOpen(custom); }}>
            <input aria-label="API key" placeholder="Or paste a shop API key" value={custom} onChange={(event) => setCustom(event.target.value)} />
            <button className="primary" type="submit">Open</button>
          </form>
        </div>
      </section>
    </div>
  );
}

function Overview({ shop }) {
  const [days, setDays] = useState(90);
  const [summary, setSummary] = useState(null);
  const [categories, setCategories] = useState([]);
  const [sources, setSources] = useState([]);
  const [trend, setTrend] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const to = new Date();
    const from = new Date(to);
    from.setDate(to.getDate() - (days - 1));
    const query = `?from=${isoDay(from)}&to=${isoDay(to)}`;
    setLoading(true);
    setError("");
    Promise.all([
      api(`/api/analytics/summary${query}`),
      api(`/api/analytics/categories${query}`),
      api(`/api/analytics/trends${query}`),
      api(`/api/analytics/sources${query}`),
    ])
      .then(([nextSummary, nextCategories, nextTrend, nextSources]) => {
        setSummary(nextSummary);
        setCategories(nextCategories);
        setTrend(nextTrend);
        setSources(nextSources);
      })
      .catch((reason) => setError(reason.message))
      .finally(() => setLoading(false));
  }, [days]);

  const maxCategory = Math.max(1, ...categories.map((item) => item.count));
  const [tone, headline] = mood(summary?.averageSentiment || 0);

  return (
    <>
      <header className="topbar">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>{headline}</h1>
        </div>
        <div className="window">
          {[30, 90, 180].map((window) => (
            <button key={window} className={days === window ? "primary" : "ghost"} onClick={() => setDays(window)}>
              {window} days
            </button>
          ))}
        </div>
      </header>
      {error && <div className="error">{error}</div>}
      {loading && <p>Counting the book…</p>}
      {summary && !loading && (
        <>
          <section className="stats">
            <article className="stat">
              <span className={`score ${tone}`}>{summary.averageSentiment.toFixed(2)}</span>
              <span>average sentiment, from -1 cold to 1 warm</span>
            </article>
            <article className="stat">
              <strong>{summary.reviewCount}</strong>
              <span>reviews</span>
            </article>
            <article className="stat">
              <strong>{summary.averageRating.toFixed(1)}</strong>
              <span>stars</span>
            </article>
            <article className="stat">
              <strong>{summary.drafts}</strong>
              <span>drafts waiting</span>
            </article>
          </section>
          <section className="split">
            <article className="panel">
              <h2>Mood by day</h2>
              {trend.length === 0 ? <p className="muted">No reviews in this window yet.</p> : <Trend points={trend} />}
            </article>
            <article className="panel">
              <h2>Mix</h2>
              <Mix summary={summary} />
            </article>
          </section>
          <section className="split">
            <article className="panel">
              <h2>What they wrote about</h2>
              {categories.length === 0 ? <p className="muted">Categories show up once reviews are in.</p> : (
                <div className="bars">
                  {categories.map((item) => (
                    <div className="bar-row" key={item.category}>
                      <span>{label(item.category)}</span>
                      <div className="track"><span style={{ width: `${(item.count / maxCategory) * 100}%` }} /></div>
                      <small>{item.count}</small>
                    </div>
                  ))}
                </div>
              )}
            </article>
            <article className="panel">
              <h2>On the desk</h2>
              <ul className="members">
                {shop.members.map((member) => (
                  <li key={member.email}><span>{member.name}</span><small>{member.role}</small></li>
                ))}
              </ul>
              <p className="muted">Sources: {sources.map((source) => `${source.source} ${source.count}`).join(" · ") || "none yet"}</p>
            </article>
          </section>
        </>
      )}
    </>
  );
}

function Mix({ summary }) {
  const rows = [
    ["Positive", summary.positiveShare, "POSITIVE"],
    ["Neutral", summary.neutralShare, "NEUTRAL"],
    ["Negative", summary.negativeShare, "NEGATIVE"],
  ];
  return (
    <div className="bars">
      {rows.map(([name, share, kind]) => (
        <div className="bar-row" key={name}>
          <span className={`pill ${kind}`}>{name}</span>
          <div className="track"><span style={{ width: `${share * 100}%` }} /></div>
          <small>{Math.round(share * 100)}%</small>
        </div>
      ))}
    </div>
  );
}

function Trend({ points }) {
  const width = 520;
  const height = 140;
  const scores = points.map((point) => point.averageSentiment);
  const min = Math.min(-1, ...scores);
  const max = Math.max(1, ...scores);
  const coords = points.map((point, index) => {
    const x = points.length === 1 ? width / 2 : (index / (points.length - 1)) * (width - 16) + 8;
    const y = 12 + ((max - point.averageSentiment) / (max - min || 1)) * (height - 24);
    return [x, y];
  });
  const line = coords.map(([x, y]) => `${x},${y}`).join(" ");
  return (
    <svg className="trend" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Sentiment trend">
      <polyline points={line} />
      <g className="dots">
        {coords.map(([x, y], index) => <circle key={points[index].day} cx={x} cy={y} r="3.5" />)}
      </g>
    </svg>
  );
}

function Reviews() {
  const [page, setPage] = useState(0);
  const [sentiment, setSentiment] = useState("");
  const [category, setCategory] = useState("");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState({ text: "", author: "", rating: "5", source: "Desk" });
  const [saving, setSaving] = useState(false);

  const query = useMemo(() => {
    const params = new URLSearchParams({ page: String(page), size: "8" });
    if (sentiment) params.set("sentiment", sentiment);
    if (category) params.set("category", category);
    return params.toString();
  }, [page, sentiment, category]);

  function load() {
    setLoading(true);
    setError("");
    return api(`/api/reviews?${query}`)
      .then(setData)
      .catch((reason) => setError(reason.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, [query]);

  async function pasteReview(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await api("/api/reviews", {
        method: "POST",
        body: JSON.stringify({
          text: draft.text,
          author: draft.author,
          rating: Number(draft.rating),
          source: draft.source,
          tone: "warm",
        }),
      });
      setDraft({ ...draft, text: "", author: "" });
      setPage(0);
      await load();
    } catch (reason) {
      setError(reason.message);
    } finally {
      setSaving(false);
    }
  }

  function replace(updated) {
    setData((current) => ({
      ...current,
      items: current.items.map((item) => item.id === updated.id ? updated : item),
    }));
  }

  return (
    <>
      <header className="topbar">
        <div>
          <p className="eyebrow">Reviews</p>
          <h1>The book</h1>
        </div>
      </header>
      {error && <div className="error">{error}</div>}
      <form className="composer" onSubmit={pasteReview}>
        <input aria-label="Review text" placeholder="Paste one review" value={draft.text} onChange={(event) => setDraft({ ...draft, text: event.target.value })} required />
        <input aria-label="Author" placeholder="Author" value={draft.author} onChange={(event) => setDraft({ ...draft, author: event.target.value })} />
        <select aria-label="Stars" value={draft.rating} onChange={(event) => setDraft({ ...draft, rating: event.target.value })}>
          {[5, 4, 3, 2, 1].map((star) => <option key={star} value={star}>{star} stars</option>)}
        </select>
        <input aria-label="Source" value={draft.source} onChange={(event) => setDraft({ ...draft, source: event.target.value })} />
        <button className="primary" disabled={saving}>{saving ? "Reading…" : "Read it"}</button>
      </form>
      <div className="filters">
        <select aria-label="Sentiment" value={sentiment} onChange={(event) => { setPage(0); setSentiment(event.target.value); }}>
          <option value="">All moods</option>
          <option value="POSITIVE">Positive</option>
          <option value="NEUTRAL">Neutral</option>
          <option value="NEGATIVE">Negative</option>
        </select>
        <select aria-label="Category" value={category} onChange={(event) => { setPage(0); setCategory(event.target.value); }}>
          <option value="">All subjects</option>
          {["PRODUCT", "SERVICE", "DELIVERY", "PRICING", "SUPPORT", "AMBIENCE", "OTHER"].map((item) => (
            <option key={item} value={item}>{label(item)}</option>
          ))}
        </select>
        <span className="muted">{data ? `${data.total} in the book` : ""}</span>
      </div>
      {loading && <p>Pulling reviews…</p>}
      {!loading && data?.items.length === 0 && <div className="panel empty">Nothing in this filter. Paste a review or widen the mood.</div>}
      <div className="stack">
        {data?.items.map((review) => <ReviewCard key={review.id} review={review} onChange={replace} />)}
      </div>
      {data && data.total > data.size && (
        <div className="pager">
          <button className="ghost" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Newer</button>
          <span>Page {page + 1}</span>
          <button className="ghost" disabled={(page + 1) * data.size >= data.total} onClick={() => setPage((value) => value + 1)}>Older</button>
        </div>
      )}
    </>
  );
}

function ReviewCard({ review, onChange }) {
  const [text, setText] = useState(review.draftReply);
  const [busy, setBusy] = useState(false);
  const [tone, setTone] = useState(0);
  const [error, setError] = useState("");

  useEffect(() => setText(review.draftReply), [review.draftReply]);

  async function redraft() {
    const next = TONES[(tone + 1) % TONES.length];
    setTone((value) => value + 1);
    setBusy(true);
    setError("");
    try {
      onChange(await api(`/api/reviews/${review.id}/redraft?tone=${next}`, { method: "POST" }));
    } catch (reason) {
      setError(reason.message);
    } finally {
      setBusy(false);
    }
  }

  async function save(status) {
    setBusy(true);
    setError("");
    try {
      onChange(await api(`/api/reviews/${review.id}`, {
        method: "PATCH",
        body: JSON.stringify({ draftReply: text, replyStatus: status }),
      }));
    } catch (reason) {
      setError(reason.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="review">
      <header>
        <div>
          <strong>{review.author}</strong>
          <div className="muted">{review.source} · {formatDay(review.reviewedAt)} · {review.rating ? `${review.rating} stars` : "unrated"}</div>
        </div>
        <div className="pills">
          <span className={`pill ${review.sentiment}`}>{review.sentiment.toLowerCase()}</span>
          <span className="pill">{label(review.category)}</span>
          <span className={`pill ${review.replyStatus}`}>{review.replyStatus === "SENT" ? "sent" : "draft"}</span>
        </div>
      </header>
      <p>{review.text}</p>
      <textarea aria-label={`Draft reply to ${review.author}`} value={text} onChange={(event) => setText(event.target.value)} />
      {error && <div className="error">{error}</div>}
      <footer>
        <small className="muted">{review.model}</small>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="ghost" disabled={busy} onClick={redraft}>Another tone</button>
          <button className="ghost" disabled={busy} onClick={() => save(review.replyStatus)}>Save</button>
          <button className="primary" disabled={busy} onClick={() => save("SENT")}>Mark sent</button>
        </div>
      </footer>
    </article>
  );
}

function Uploads() {
  const [jobs, setJobs] = useState([]);
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);

  async function refresh() {
    setJobs(await api("/api/uploads"));
  }

  useEffect(() => {
    refresh().catch((reason) => setError(reason.message));
    const timer = setInterval(() => {
      refresh().catch(() => {});
    }, 2000);
    return () => clearInterval(timer);
  }, []);

  async function sendFile(file) {
    setSending(true);
    setError("");
    try {
      const body = new FormData();
      body.append("file", file);
      await api("/api/uploads", { method: "POST", body });
      await refresh();
    } catch (reason) {
      setError(reason.message);
    } finally {
      setSending(false);
    }
  }

  async function sendSample() {
    const response = await fetch("/sample-reviews.csv");
    const blob = await response.blob();
    await sendFile(new File([blob], "sample-reviews.csv", { type: "text/csv" }));
  }

  const active = jobs.some((job) => job.status === "QUEUED" || job.status === "RUNNING");

  return (
    <>
      <header className="topbar">
        <div>
          <p className="eyebrow">Batch</p>
          <h1>Bring in the old book</h1>
        </div>
      </header>
      <p className="lede">A CSV drops into RabbitMQ and Spring Batch reads it off the queue, so the desk stays free while the file is scored.</p>
      {error && <div className="error">{error}</div>}
      <label className="drop">
        <strong>{sending ? "Sending to the queue…" : "Drop a CSV here, or click to choose one"}</strong>
        <span className="muted">Header row: author, rating, source, reviewed_at, text. Aliases like reviewer and stars are fine.</span>
        <input type="file" accept=".csv,text/csv" hidden onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) sendFile(file);
          event.target.value = "";
        }} />
      </label>
      <div>
        <button type="button" className="ghost" disabled={sending} onClick={sendSample}>
          Import the sample file
        </button>
      </div>
      <div className="stack">
        {jobs.length === 0 && <div className="panel empty">No uploads yet. The sample file is four reviews and takes a moment.</div>}
        {jobs.map((job) => (
          <article className="review" key={job.id}>
            <header>
              <strong>{job.filename}</strong>
              <span className={`pill ${job.status === "FAILED" ? "NEGATIVE" : job.status === "COMPLETED" ? "POSITIVE" : "NEUTRAL"}`}>
                {job.status.toLowerCase()}
              </span>
            </header>
            <p className="muted">
              {job.processedRows} written{job.totalRows ? ` of ${job.totalRows}` : ""}{job.failedRows ? ` · ${job.failedRows} skipped` : ""}
              {active && (job.status === "QUEUED" || job.status === "RUNNING") ? " · still working" : ""}
            </p>
            {job.errorMessage && <div className="error">{job.errorMessage}</div>}
          </article>
        ))}
      </div>
    </>
  );
}

function label(value) {
  return value.charAt(0) + value.slice(1).toLowerCase();
}
