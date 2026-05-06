const { useEffect, useState } = React;

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

function formatTimestamp(value) {
  if (!value) {
    return "";
  }

  return new Date(value).toLocaleString();
}

function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [hadUnreadOnLoad, setHadUnreadOnLoad] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    fetch("/api/groups/notifications", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load notifications"))
      .then((data) => {
        setNotifications(data);
        const hasUnread = data.some((notification) => !notification.read);
        setHadUnreadOnLoad(hasUnread);
        if (hasUnread) {
          fetch("/api/groups/notifications/read", {
            method: "POST",
            headers: buildAuthHeaders()
          }).catch(() => null);
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
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
          <a className="button-secondary" href="/messages.html">Inbox</a>
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary" href="/home">Homepage</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Notifications</h1>
          <p className="helper">
            Review invitations, direct message alerts, document feedback, and group activity from one place.
          </p>
        </div>

        {error && <div className="error">{error}</div>}

        {loading ? (
          <div className="notice">Loading notifications...</div>
        ) : notifications.length === 0 ? (
          <div className="notice">No notifications yet.</div>
        ) : (
          <div className="stack">
            {notifications.map((notification) => (
              <div
                className={`notification-card panel ${hadUnreadOnLoad && !notification.read ? "notification-card-unread" : ""}`}
                key={notification.notificationId}
              >
                <div>{notification.message}</div>
                <div className="footer-note">{formatTimestamp(notification.timestamp)}</div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<NotificationsPage />);
