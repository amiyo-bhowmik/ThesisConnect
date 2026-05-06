const features = [
  {
    title: "Build your research identity",
    text: "Present your academic background, research interests, skills, and thesis goals so other students can understand how you would contribute to a team."
  },
  {
    title: "Find the right collaborators",
    text: "Discover students with matching interests, explore their profiles, and connect with people whose strengths fit your research direction."
  },
  {
    title: "Organize thesis groups",
    text: "Create groups, manage members, assign admins when needed, and keep your collaboration space structured as your work progresses."
  },
  {
    title: "Keep collaboration moving",
    text: "Use direct messages, notifications, and shared group activity to stay aligned on discussions, documents, and next steps."
  }
];

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

function HomePage() {
  const [profileName, setProfileName] = React.useState("");
  const [hasUnreadNotifications, setHasUnreadNotifications] = React.useState(false);

  React.useEffect(() => {
    const token = getToken();
    if (!token) {
      window.location.href = "/login.html";
      return;
    }

    fetch("/api/profile/me", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load profile"))
      .then((data) => setProfileName(data.name || ""))
      .catch(() => setProfileName(""));

    fetch("/api/groups/notifications/unread", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load unread notification status"))
      .then((data) => setHasUnreadNotifications(!!data))
      .catch(() => setHasUnreadNotifications(false));
  }, []);

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
          <a className="button-secondary" href="/profile.html">Edit my profile</a>
          <a className="button-secondary" href="/discover.html">Discover thesis partners</a>
          <a className="button-secondary" href="/messages.html">Inbox</a>
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary nav-notification-link" href="/notifications.html">
            Notifications
            {hasUnreadNotifications && <span className="nav-notification-dot" aria-label="Unread notifications" />}
          </a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="hero">
        <div>
          <h1>Welcome{profileName ? ` ${profileName}` : ""} to ThesisConnect.</h1>
          <p className="helper">
            Manage your thesis journey in one place by finding research partners, joining or creating groups,
            and staying connected with the students you collaborate with.
          </p>
          <div className="nav-links" style={{marginTop: "24px"}}>
            <a className="button" href="/profile.html">Edit My Profile</a>
            <a className="button-secondary" href="/discover.html">Discover Thesis Partners</a>
            <a className="button-secondary" href="/messages.html">Open Inbox</a>
            <a className="button-secondary" href="/groups.html">Open Thesis Groups</a>
            <a className="button-secondary" href="/notifications.html">Open Notifications</a>
          </div>
          <div className="hero-stats">
            <div className="mini-card">
              <h3>Profile</h3>
              <p className="muted">Keep your academic background and research interests ready for potential collaborators.</p>
            </div>
            <div className="mini-card">
              <h3>Groups</h3>
              <p className="muted">Create or join thesis groups and coordinate your work with the right teammates.</p>
            </div>
            <div className="mini-card">
              <h3>Inbox</h3>
              <p className="muted">Stay in touch through direct messages and notifications as your thesis work evolves.</p>
            </div>
          </div>
        </div>

        <div className="panel stack">
          <div>
            <div className="section-title">Work Together</div>
            <p className="muted">
              ThesisConnect is built for research students who need a shared place to discover collaborators,
              discuss ideas, organize groups, and manage thesis-related communication.
            </p>
          </div>
          <div>
            <div className="section-title">Plan Better</div>
            <p className="muted">
              Keep your team aligned by using profiles, group roles, shared documents, and ongoing discussion
              to move your research from idea to final submission.
            </p>
          </div>
        </div>
      </section>

      <section id="feature-grid" className="card-grid">
        {features.map((feature) => (
          <article className="card" key={feature.title}>
            <h3>{feature.title}</h3>
            <p className="muted">{feature.text}</p>
          </article>
        ))}
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<HomePage />);
