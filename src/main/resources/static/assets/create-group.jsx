const { useState } = React;

function getToken() {
  return window.localStorage.getItem("thesisconnect_token");
}

function buildAuthHeaders(extraHeaders) {
  const token = getToken();
  return {
    ...(extraHeaders || {}),
    Authorization: `Bearer ${token}`
  };
}

async function handleApiResponse(response, fallbackMessage) {
  if (response.status === 401) {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
    throw new Error("Session expired. Please log in again.");
  }

  if (!response.ok) {
    const rawBody = await response.text();
    let message = rawBody;

    try {
      const data = rawBody ? JSON.parse(rawBody) : {};
      message = data.message || data.detail || data.error || rawBody;
    } catch (parseError) {
      message = rawBody;
    }

    throw new Error(message || fallbackMessage);
  }

  return response.json();
}

function CreateGroupPage() {
  const [formData, setFormData] = useState({ topic: "", description: "" });
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  React.useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
    }
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setFormData((current) => ({
      ...current,
      [name]: value
    }));
  }

  function createGroup(event) {
    event.preventDefault();
    setSubmitting(true);
    setStatus("");
    setError("");

    fetch("/api/groups", {
      method: "POST",
      headers: {
        ...buildAuthHeaders({
          "Content-Type": "application/json"
        })
      },
      body: JSON.stringify(formData)
    })
      .then((response) => handleApiResponse(response, "Could not create thesis group"))
      .then(() => {
        setStatus("Thesis group created successfully.");
        setFormData({ topic: "", description: "" });
      })
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  }

  function logout() {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
  }

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">TC</div>
          <div>ThesisConnect</div>
        </div>
        <nav className="nav-links">
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary" href="/available-groups.html">Available groups</a>
          <a className="button-secondary" href="/notifications.html">Notifications</a>
          <a className="button-secondary" href="/home">Homepage</a>
          <a className="button-secondary" href="/discover.html">Discover students</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Create thesis group</h1>
          <p className="helper">
            Start a thesis group with a clear research topic and description so other students know the scope and goals.
          </p>
        </div>

        {status && <div className="success">{status}</div>}
        {error && <div className="error">{error}</div>}

        <form className="auth-form" onSubmit={createGroup}>
          <label className="field">
            <span>Topic</span>
            <input
              name="topic"
              value={formData.topic}
              onChange={updateField}
              placeholder="Human-centered AI in education"
              required
            />
          </label>

          <label className="field">
            <span>Description</span>
            <textarea
              name="description"
              value={formData.description}
              onChange={updateField}
              placeholder="Describe the thesis idea, research direction, tools, and the type of collaborators you want."
              required
            />
          </label>

          <div className="nav-links">
            <button className="button" type="submit" disabled={submitting}>
              {submitting ? "Creating..." : "Create thesis group"}
            </button>
            <a className="button-secondary" href="/groups.html">Back to thesis groups</a>
          </div>
        </form>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<CreateGroupPage />);
