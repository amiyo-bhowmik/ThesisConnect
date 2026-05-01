const { useEffect, useMemo, useState } = React;

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

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function getInitials(name) {
  return (name || "?")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

function formatTimestamp(value) {
  if (!value) {
    return "";
  }

  return new Date(value).toLocaleString();
}

function GroupsPage() {
  const [groups, setGroups] = useState([]);
  const [groupsLoading, setGroupsLoading] = useState(true);
  const [groupsError, setGroupsError] = useState("");
  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [selectedGroupLoading, setSelectedGroupLoading] = useState(false);
  const [selectedGroupError, setSelectedGroupError] = useState("");
  const [notifications, setNotifications] = useState([]);
  const [students, setStudents] = useState([]);
  const [studentsLoading, setStudentsLoading] = useState(true);
  const [createForm, setCreateForm] = useState({ topic: "", description: "" });
  const [inviteUserId, setInviteUserId] = useState("");
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [busyAction, setBusyAction] = useState("");

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    loadGroups();
    loadNotifications();
    loadStudents();
  }, []);

  function loadGroups(preferredGroupId) {
    setGroupsLoading(true);
    setGroupsError("");

    fetch("/api/groups", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load thesis groups"))
      .then((data) => {
        setGroups(data);
        if (data.length === 0) {
          setSelectedGroupId(null);
          setSelectedGroup(null);
          return;
        }

        const nextGroupId = preferredGroupId || selectedGroupId || data[0].groupId;
        const matchingGroup = data.find((group) => group.groupId === nextGroupId) || data[0];
        setSelectedGroupId(matchingGroup.groupId);
        loadGroup(matchingGroup.groupId);
      })
      .catch((err) => {
        setGroupsError(err.message);
        setGroups([]);
        setSelectedGroupId(null);
        setSelectedGroup(null);
      })
      .finally(() => setGroupsLoading(false));
  }

  function loadGroup(groupId) {
    setSelectedGroupLoading(true);
    setSelectedGroupError("");

    fetch(`/api/groups/${groupId}`, {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load group details"))
      .then((data) => {
        setSelectedGroup(data);
        setSelectedGroupId(groupId);
      })
      .catch((err) => setSelectedGroupError(err.message))
      .finally(() => setSelectedGroupLoading(false));
  }

  function loadNotifications() {
    fetch("/api/groups/notifications", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load notifications"))
      .then((data) => setNotifications(data))
      .catch(() => setNotifications([]));
  }

  function loadStudents() {
    setStudentsLoading(true);
    fetch("/api/profile/students", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load students"))
      .then((data) => setStudents(data))
      .catch(() => setStudents([]))
      .finally(() => setStudentsLoading(false));
  }

  function updateCreateField(event) {
    const { name, value } = event.target;
    setCreateForm((current) => ({
      ...current,
      [name]: value
    }));
  }

  function runGroupAction(actionKey, requestFactory, successMessage) {
    setBusyAction(actionKey);
    setStatus("");
    setError("");

    requestFactory()
      .then((data) => {
        if (data && data.groupId) {
          setSelectedGroup(data);
          setSelectedGroupId(data.groupId);
          loadGroups(data.groupId);
        } else {
          loadGroups(selectedGroupId);
        }
        loadNotifications();
        loadStudents();
        if (successMessage) {
          setStatus(successMessage);
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  function createGroup(event) {
    event.preventDefault();
    runGroupAction(
      "create-group",
      () =>
        fetch("/api/groups", {
          method: "POST",
          headers: {
            ...buildAuthHeaders({
              "Content-Type": "application/json"
            })
          },
          body: JSON.stringify(createForm)
        }).then((response) => handleApiResponse(response, "Could not create thesis group")),
      "Thesis group created successfully."
    );
  }

  function openGroup(groupId) {
    setSelectedGroupId(groupId);
    loadGroup(groupId);
  }

  function logout() {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
  }

  const availableInviteStudents = useMemo(() => {
    if (!selectedGroup) {
      return [];
    }

    const memberIds = new Set((selectedGroup.members || []).map((member) => member.userId));
    const invitedIds = new Set(
      (selectedGroup.pendingInvitations || [])
        .map((request) => request.recipient && request.recipient.userId)
        .filter(Boolean)
    );

    return students.filter((student) => !memberIds.has(student.userId) && !invitedIds.has(student.userId));
  }, [selectedGroup, students]);

  const currentInvitation = selectedGroup && !selectedGroup.currentUserAdmin
    ? (selectedGroup.pendingInvitations || [])[0]
    : null;

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">TC</div>
          <div>ThesisConnect</div>
        </div>
        <nav className="nav-links">
          <a className="button-secondary" href="/home">Homepage</a>
          <a className="button-secondary" href="/discover.html">Discover students</a>
          <a className="button-secondary" href="/profile.html">My profile</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Manage thesis groups</h1>
          <p className="helper">
            Create thesis groups, review members and their profiles, invite students, approve or reject join requests,
            assign additional admins, and respond to invitations from other groups.
          </p>
        </div>

        {status && <div className="success">{status}</div>}
        {error && <div className="error">{error}</div>}
        {groupsError && <div className="error">{groupsError}</div>}

        <div className="group-layout">
          <div className="stack">
            <section className="panel stack">
              <div className="section-title">Create thesis group</div>
              <form className="auth-form" onSubmit={createGroup}>
                <label className="field">
                  <span>Topic</span>
                  <input
                    name="topic"
                    value={createForm.topic}
                    onChange={updateCreateField}
                    placeholder="Smart healthcare diagnosis"
                    required
                  />
                </label>
                <label className="field">
                  <span>Description</span>
                  <textarea
                    name="description"
                    value={createForm.description}
                    onChange={updateCreateField}
                    placeholder="Describe the thesis scope, methods, and the kind of teammates you want."
                    required
                  />
                </label>
                <button className="button" type="submit" disabled={busyAction === "create-group"}>
                  {busyAction === "create-group" ? "Creating..." : "Create group"}
                </button>
              </form>
            </section>

            <section className="panel stack">
              <div className="results-header">
                <div className="section-title" style={{marginBottom: 0}}>My notifications</div>
                <div className="footer-note">{notifications.length} update{notifications.length === 1 ? "" : "s"}</div>
              </div>
              {notifications.length === 0 ? (
                <div className="notice">No group notifications yet.</div>
              ) : (
                <div className="stack">
                  {notifications.slice(0, 6).map((notification) => (
                    <div className="notification-card" key={notification.notificationId}>
                      <div>{notification.message}</div>
                      <div className="footer-note">{formatTimestamp(notification.timestamp)}</div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="panel stack">
              <div className="results-header">
                <div className="section-title" style={{marginBottom: 0}}>Available groups</div>
                <div className="footer-note">
                  {groupsLoading ? "Loading..." : `${groups.length} group${groups.length === 1 ? "" : "s"}`}
                </div>
              </div>

              {groupsLoading ? (
                <div className="notice">Loading thesis groups...</div>
              ) : groups.length === 0 ? (
                <div className="notice">No thesis groups yet. Create the first one from the form above.</div>
              ) : (
                <div className="student-list">
                  {groups.map((group) => (
                    <button
                      type="button"
                      key={group.groupId}
                      className={`student-card ${selectedGroupId === group.groupId ? "student-card-active" : ""}`}
                      onClick={() => openGroup(group.groupId)}
                    >
                      <div className="group-card-head">
                        <div>
                          <div className="student-name">{group.topic}</div>
                          <div className="muted compact-text">Created by {group.adminName}</div>
                        </div>
                        <span className={`status-badge ${group.currentUserMember ? "status-open" : "status-closed"}`}>
                          {group.currentUserAdmin ? "Admin" : group.currentUserMember ? "Member" : "Explore"}
                        </span>
                      </div>
                      <div className="compact-text muted">{group.description}</div>
                      <div className="chip-row compact-chips">
                        <div className="chip">{group.memberCount} member{group.memberCount === 1 ? "" : "s"}</div>
                        {group.currentUserJoinRequestStatus && <div className="chip">Join request pending</div>}
                        {group.currentUserInvitationStatus && <div className="chip">Invitation pending</div>}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </section>
          </div>

          <section className="panel detail-panel">
            {selectedGroupLoading ? (
              <div className="notice">Loading group details...</div>
            ) : selectedGroupError ? (
              <div className="error">{selectedGroupError}</div>
            ) : !selectedGroup ? (
              <div className="notice">Select a thesis group to view members and actions.</div>
            ) : (
              <div className="stack">
                <div className="group-header">
                  <div>
                    <h2 className="section-title" style={{fontSize: "2rem", marginBottom: "8px"}}>{selectedGroup.topic}</h2>
                    <p className="muted compact-text">{selectedGroup.description}</p>
                  </div>
                  <div className="group-badges">
                    <span className="chip">Admin: {selectedGroup.admin.name}</span>
                    <span className="chip">{selectedGroup.members.length} member{selectedGroup.members.length === 1 ? "" : "s"}</span>
                  </div>
                </div>
