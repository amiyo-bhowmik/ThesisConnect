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

function GroupDetailsPage() {
  const [group, setGroup] = useState(null);
  const [groupLoading, setGroupLoading] = useState(true);
  const [groupError, setGroupError] = useState("");
  const [students, setStudents] = useState([]);
  const [studentsLoading, setStudentsLoading] = useState(true);
  const [inviteUserId, setInviteUserId] = useState("");
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [busyAction, setBusyAction] = useState("");

  const params = new URLSearchParams(window.location.search);
  const groupId = params.get("groupId");

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    if (!groupId) {
      setGroupError("No thesis group was selected.");
      setGroupLoading(false);
      return;
    }

    loadGroup();
    loadStudents();
  }, [groupId]);

  function loadGroup() {
    setGroupLoading(true);
    setGroupError("");

    fetch(`/api/groups/${groupId}`, {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load group details"))
      .then((data) => setGroup(data))
      .catch((err) => setGroupError(err.message))
      .finally(() => setGroupLoading(false));
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

  function runGroupAction(actionKey, requestFactory, successMessage) {
    setBusyAction(actionKey);
    setStatus("");
    setError("");

    requestFactory()
      .then((data) => {
        if (data) {
          setGroup(data);
        } else {
          loadGroup();
        }
        loadStudents();
        if (successMessage) {
          setStatus(successMessage);
        }
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  function sendJoinRequest() {
    if (!group) {
      return;
    }

    runGroupAction(
      `join-${group.groupId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/join-requests`, {
          method: "POST",
          headers: buildAuthHeaders()
        }).then((response) => handleApiResponse(response, "Could not send join request")),
      "Join request sent."
    );
  }

  function handleRequestAction(requestId, action) {
    if (!group) {
      return;
    }

    runGroupAction(
      `${action}-${requestId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/requests/${requestId}/${action}`, {
          method: "POST",
          headers: buildAuthHeaders()
        }).then((response) => handleApiResponse(response, `Could not ${action} request`)),
      action === "approve" ? "Request updated successfully." : "Request rejected successfully."
    );
  }

  function inviteMember(event) {
    event.preventDefault();
    if (!group || !inviteUserId) {
      return;
    }

    runGroupAction(
      `invite-${group.groupId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/invitations`, {
          method: "POST",
          headers: {
            ...buildAuthHeaders({
              "Content-Type": "application/json"
            })
          },
          body: JSON.stringify({ userId: Number(inviteUserId) })
        }).then((response) => handleApiResponse(response, "Could not send invitation")),
      "Invitation sent."
    );
  }

  function assignAdmin(userId) {
    if (!group) {
      return;
    }

    runGroupAction(
      `assign-admin-${userId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/members/${userId}/admins`, {
          method: "POST",
          headers: buildAuthHeaders()
        }).then((response) => handleApiResponse(response, "Could not assign admin")),
      "Group admin updated."
    );
  }

  function logout() {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
  }

  const availableInviteStudents = useMemo(() => {
    if (!group) {
      return [];
    }

    const memberIds = new Set((group.members || []).map((member) => member.userId));
    const invitedIds = new Set(
      (group.pendingInvitations || [])
        .map((request) => request.recipient && request.recipient.userId)
        .filter(Boolean)
    );

    return students.filter((student) => !memberIds.has(student.userId) && !invitedIds.has(student.userId));
  }, [group, students]);

  const currentInvitation = group && !group.currentUserAdmin
    ? (group.pendingInvitations || [])[0]
    : null;

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
          <a className="button-secondary" href="/create-group.html">Create thesis group</a>
          <a className="button-secondary" href="/notifications.html">Notifications</a>
          <a className="button-secondary" href="/home">Homepage</a>
          <a className="button-secondary" href="/discover.html">Discover students</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        {status && <div className="success">{status}</div>}
        {error && <div className="error">{error}</div>}

        {groupLoading ? (
          <div className="notice">Loading group details...</div>
        ) : groupError ? (
          <div className="error">{groupError}</div>
        ) : !group ? (
          <div className="notice">No thesis group details are available.</div>
        ) : (
          <div className="stack">
            <div>
              <h1 className="page-title" style={{fontSize: "2.8rem"}}>{group.topic}</h1>
              <p className="helper">{group.description}</p>
            </div>

            <div className="group-badges">
              <span className="chip">Group Creator: {group.admin.name}</span>
              <span className="chip">{group.members.length} member{group.members.length === 1 ? "" : "s"}</span>
            </div>

            {group.currentUserMember && (
              <div className="notice">
                You are already a member of this thesis group.
              </div>
            )}

            {!group.currentUserMember && !group.currentUserJoinRequestStatus && !currentInvitation && (
              <button
                className="button"
                type="button"
                onClick={sendJoinRequest}
                disabled={busyAction === `join-${group.groupId}`}
              >
                {busyAction === `join-${group.groupId}` ? "Sending request..." : "Request to join this group"}
              </button>
            )}

            {group.currentUserJoinRequestStatus && (
              <div className="notice">Your join request is pending review by the group admins.</div>
            )}

            {currentInvitation && (
              <div className="panel stack invitation-box">
                <div className="section-title">Pending invitation</div>
                <p className="muted compact-text">
                  {currentInvitation.sender.name} invited you to join this group.
                </p>
                <div className="nav-links">
                  <button
                    className="button"
                    type="button"
                    onClick={() => handleRequestAction(currentInvitation.requestId, "approve")}
                    disabled={busyAction === `approve-${currentInvitation.requestId}`}
                  >
                    Accept invitation
                  </button>
                  <button
                    className="button-danger"
                    type="button"
                    onClick={() => handleRequestAction(currentInvitation.requestId, "reject")}
                    disabled={busyAction === `reject-${currentInvitation.requestId}`}
                  >
                    Reject invitation
                  </button>
                </div>
              </div>
            )}

            <div className="section-title">Members and profiles</div>
            <div className="group-members-grid">
              {(group.members || []).map((member) => (
                <article className="panel member-card" key={member.userId}>
                  <div className="student-identity">
                    {member.profilePicture ? (
                      <img className="student-avatar" src={member.profilePicture} alt={member.name} />
                    ) : (
                      <div className="student-avatar student-avatar-placeholder">
                        {getInitials(member.name)}
                      </div>
                    )}
                    <div>
                      <div className="student-name">{member.name}</div>
                      <div className="muted compact-text">{member.email}</div>
                      <div className="muted compact-text">{member.department || "Department not added"}</div>
                    </div>
                  </div>
                  <div className="chip-row compact-chips">
                    <div className="chip">{member.admin ? "Admin" : "Member"}</div>
                    <div className="chip">{member.university || "University missing"}</div>
                  </div>
                  <p className="muted compact-text">{member.bio || "No bio added yet."}</p>
                  <div className="chip-row compact-chips">
                    {(member.researchInterests || []).slice(0, 4).map((interest) => (
                      <div className="chip" key={interest}>{interest}</div>
                    ))}
                  </div>
                  {group.currentUserAdmin && !member.admin && (
                    <button
                      className="button-secondary"
                      type="button"
                      onClick={() => assignAdmin(member.userId)}
                      disabled={busyAction === `assign-admin-${member.userId}`}
                    >
                      Make admin
                    </button>
                  )}
                </article>
              ))}
            </div>

            {group.currentUserAdmin && (
              <div className="group-admin-grid">
                <section className="panel stack">
                  <div className="section-title">Invite student</div>
                  <form className="auth-form" onSubmit={inviteMember}>
                    <label className="field">
                      <span>Choose student</span>
                      <select
                        className="select-input"
                        value={inviteUserId}
                        onChange={(event) => setInviteUserId(event.target.value)}
                        disabled={studentsLoading || availableInviteStudents.length === 0}
                      >
                        <option value="">Select a student</option>
                        {availableInviteStudents.map((student) => (
                          <option key={student.userId} value={student.userId}>
                            {student.name} - {student.email}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button className="button" type="submit" disabled={!inviteUserId || busyAction === `invite-${group.groupId}`}>
                      {busyAction === `invite-${group.groupId}` ? "Sending..." : "Send invitation"}
                    </button>
                  </form>
                  {!studentsLoading && availableInviteStudents.length === 0 && (
                    <div className="notice">There are no additional students available to invite right now.</div>
                  )}
                </section>

                <section className="panel stack">
                  <div className="section-title">Pending join requests</div>
                  {group.pendingJoinRequests.length === 0 ? (
                    <div className="notice">No pending join requests.</div>
                  ) : (
                    <div className="stack">
                      {group.pendingJoinRequests.map((request) => (
                        <div className="notification-card" key={request.requestId}>
                          <div>
                            <strong>{request.sender.name}</strong> wants to join this group.
                          </div>
                          <div className="footer-note">{request.sender.email}</div>
                          <div className="nav-links">
                            <button
                              className="button"
                              type="button"
                              onClick={() => handleRequestAction(request.requestId, "approve")}
                              disabled={busyAction === `approve-${request.requestId}`}
                            >
                              Approve
                            </button>
                            <button
                              className="button-danger"
                              type="button"
                              onClick={() => handleRequestAction(request.requestId, "reject")}
                              disabled={busyAction === `reject-${request.requestId}`}
                            >
                              Reject
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>

                <section className="panel stack">
                  <div className="section-title">Pending invitations</div>
                  {group.pendingInvitations.length === 0 ? (
                    <div className="notice">No pending invitations.</div>
                  ) : (
                    <div className="stack">
                      {group.pendingInvitations.map((request) => (
                        <div className="notification-card" key={request.requestId}>
                          <div>
                            Invitation sent to <strong>{request.recipient ? request.recipient.name : "student"}</strong>.
                          </div>
                          <div className="footer-note">
                            {request.recipient ? request.recipient.email : "Awaiting response"}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<GroupDetailsPage />);
