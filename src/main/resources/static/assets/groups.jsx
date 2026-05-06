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

function ThesisGroupsPage() {
  const [groups, setGroups] = useState([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [groupsError, setGroupsError] = useState("");

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    loadGroups();
  }, []);

  function loadGroups(preferredGroupId) {
    setGroupsLoading(true);
    setGroupsError("");

    fetch("/api/groups", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load thesis groups"))
      .then((data) => {
        const joinedGroups = data.filter((group) => group.currentUserMember);
        setGroups(joinedGroups);
      })
      .catch((err) => {
        setGroupsError(err.message);
        setGroups([]);
      })
      .finally(() => setGroupsLoading(false));
  }

  function openGroup(groupId) {
    window.location.href = `/group-details.html?groupId=${encodeURIComponent(groupId)}`;
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
          <a className="button-secondary" href="/home">Homepage</a>
          <a className="button-secondary" href="/messages.html">Messages</a>
          <a className="button-secondary" href="/create-group.html">Create thesis group</a>
          <a className="button-secondary" href="/notifications.html">Notifications</a>
          <a className="button-secondary" href="/available-groups.html">Available groups</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>My thesis groups</h1>
          <p className="helper">
            This page only shows the thesis groups you have already joined. Open a group to review members,
            see full profile details, and manage requests if you are one of the group admins.
          </p>
        </div>

        {groupsError && <div className="error">{groupsError}</div>}

        <div className="results-column">
          <div className="results-header">
            <div className="section-title" style={{marginBottom: 0}}>Joined groups</div>
            <div className="footer-note">
              {groupsLoading ? "Loading..." : `${groups.length} group${groups.length === 1 ? "" : "s"} joined`}
            </div>
          </div>

          {groupsLoading ? (
            <div className="notice">Loading your thesis groups...</div>
          ) : groups.length === 0 ? (
            <div className="notice">
              You have not joined any thesis groups yet. Use Available groups to browse teams or Create thesis group
              to start one.
            </div>
          ) : (
            <div className="student-list">
              {groups.map((group) => (
                <button
                  type="button"
                  key={group.groupId}
                  className="student-card"
                  onClick={() => openGroup(group.groupId)}
                >
                  <div className="group-card-head">
                    <div>
                      <div className="student-name">{group.topic}</div>
                      <div className="muted compact-text">Created by {group.adminName}</div>
                    </div>
                    <span className={`status-badge ${group.currentUserAdmin ? "status-open" : "status-closed"}`}>
                      {group.currentUserAdmin ? "Admin" : "Member"}
                    </span>
                  </div>
                  <div className="compact-text muted">{group.description}</div>
                  <div className="chip-row compact-chips">
                    <div className="chip">{group.memberCount} member{group.memberCount === 1 ? "" : "s"}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<ThesisGroupsPage />);
