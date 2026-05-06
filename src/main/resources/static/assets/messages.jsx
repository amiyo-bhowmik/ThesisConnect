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

function formatDateTime(value) {
  if (!value) {
    return "Unknown";
  }

  return new Date(value).toLocaleString();
}

function MessagesPage() {
  const [students, setStudents] = useState([]);
  const [studentsLoading, setStudentsLoading] = useState(true);
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [conversation, setConversation] = useState(null);
  const [conversationLoading, setConversationLoading] = useState(false);
  const [draft, setDraft] = useState("");
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [busyAction, setBusyAction] = useState("");
  const [searchTerm, setSearchTerm] = useState("");

  const preferredStudentId = new URLSearchParams(window.location.search).get("studentId");

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    loadStudents();
  }, []);

  function loadStudents() {
    setStudentsLoading(true);
    setError("");

    fetch("/api/profile/students", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load students"))
      .then((data) => {
        setStudents(data);
        if (data.length === 0) {
          setSelectedStudentId(null);
          setConversation(null);
          return;
        }

        const preferred = preferredStudentId
          ? data.find((student) => String(student.userId) === preferredStudentId)
          : null;
        const nextStudent = preferred || data[0];
        setSelectedStudentId(nextStudent.userId);
        loadConversation(nextStudent.userId);
      })
      .catch((err) => setError(err.message))
      .finally(() => setStudentsLoading(false));
  }

  function loadConversation(studentId) {
    setConversationLoading(true);
    setError("");

    fetch(`/api/messages/direct/${studentId}`, {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load conversation"))
      .then((data) => setConversation(data))
      .catch((err) => {
        setConversation(null);
        setError(err.message);
      })
      .finally(() => setConversationLoading(false));
  }

  function openConversation(studentId) {
    setSelectedStudentId(studentId);
    loadConversation(studentId);
  }

  function sendMessage(event) {
    event.preventDefault();
    if (!selectedStudentId || !draft.trim()) {
      return;
    }

    setBusyAction("send");
    setStatus("");
    setError("");

    fetch("/api/messages/direct", {
      method: "POST",
      headers: buildAuthHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify({
        recipientUserId: selectedStudentId,
        content: draft.trim()
      })
    })
      .then((response) => handleApiResponse(response, "Could not send message"))
      .then((data) => {
        setConversation(data);
        setDraft("");
        setStatus("Direct message sent.");
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  function togglePin(message) {
    const action = message.pinned ? "unpin" : "pin";
    setBusyAction(`${action}-${message.messageId}`);
    setStatus("");
    setError("");

    fetch(`/api/messages/direct/${message.messageId}/${action}`, {
      method: "POST",
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, `Could not ${action} message`))
      .then((data) => {
        setConversation(data);
        setStatus(message.pinned ? "Message unpinned." : "Message pinned.");
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  function logout() {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
  }

  const filteredStudents = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) {
      return students;
    }

    return students.filter((student) => {
      return [student.name, student.email, student.department, student.university]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query));
    });
  }, [students, searchTerm]);

  const pinnedMessages = useMemo(() => {
    return (conversation?.messages || []).filter((message) => message.pinned);
  }, [conversation]);

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">TC</div>
          <div>ThesisConnect</div>
        </div>
        <nav className="nav-links">
          <a className="button-secondary" href="/discover.html">Discover students</a>
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary" href="/notifications.html">Notifications</a>
          <a className="button-secondary" href="/home">Homepage</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Direct messages</h1>
          <p className="helper">
            Talk with potential thesis partners, continue planning with classmates, and pin the messages you want to keep within easy reach.
          </p>
        </div>

        {status && <div className="success">{status}</div>}
        {error && <div className="error">{error}</div>}

        <div className="directory-layout">
          <div className="results-column">
            <div className="results-header">
              <div className="section-title" style={{marginBottom: 0}}>Students</div>
              <div className="footer-note">
                {studentsLoading ? "Loading..." : `${filteredStudents.length} available`}
              </div>
            </div>

            <label className="field">
              <span>Search conversations</span>
              <input
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Search by name, email, department, or university"
              />
            </label>

            {studentsLoading ? (
              <div className="notice">Loading students...</div>
            ) : filteredStudents.length === 0 ? (
              <div className="notice">No students match your search.</div>
            ) : (
              <div className="student-list">
                {filteredStudents.map((student) => (
                  <button
                    type="button"
                    key={student.userId}
                    className={`student-card ${selectedStudentId === student.userId ? "student-card-active" : ""}`}
                    onClick={() => openConversation(student.userId)}
                  >
                    <div className="student-card-head">
                      <div className="student-identity">
                        {student.profilePicture ? (
                          <img className="student-avatar" src={student.profilePicture} alt={student.name} />
                        ) : (
                          <div className="student-avatar student-avatar-placeholder">{getInitials(student.name)}</div>
                        )}
                        <div>
                          <div className="student-name">{student.name}</div>
                          <div className="muted compact-text">{student.email}</div>
                        </div>
                      </div>
                      <span className={`status-badge ${student.lookingForGroup ? "status-open" : "status-closed"}`}>
                        {student.lookingForGroup ? "Open" : "Busy"}
                      </span>
                    </div>
                    <div className="compact-text muted">{student.department || "Department not added"}</div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="panel detail-panel">
            <div className="results-header">
              <div className="section-title" style={{marginBottom: 0}}>Conversation</div>
              {conversation?.partner && (
                <span className={`status-badge ${conversation.partner.lookingForGroup ? "status-open" : "status-closed"}`}>
                  {conversation.partner.lookingForGroup ? "Looking for group" : "Not searching"}
                </span>
              )}
            </div>

            {conversationLoading ? (
              <div className="notice">Loading conversation...</div>
            ) : !conversation ? (
              <div className="notice">Choose a student to start a conversation.</div>
            ) : (
              <div className="stack">
                <div className="student-identity">
                  {conversation.partner.profilePicture ? (
                    <img className="avatar" src={conversation.partner.profilePicture} alt={conversation.partner.name} />
                  ) : (
                    <div className="avatar-placeholder">{getInitials(conversation.partner.name)}</div>
                  )}
                  <div>
                    <h3 className="section-title" style={{marginBottom: "8px"}}>{conversation.partner.name}</h3>
                    <p className="muted compact-text">{conversation.partner.email}</p>
                    <p className="muted compact-text">{conversation.partner.university || "University not added yet"}</p>
                  </div>
                </div>

                {pinnedMessages.length > 0 && (
                  <section className="detail-block">
                    <div className="detail-label">Pinned messages</div>
                    <div className="stack">
                      {pinnedMessages.map((message) => (
                        <div className="pinned-banner" key={`pinned-${message.messageId}`}>
                          <div>
                            <strong>{message.sender.name}</strong>
                            <div className="compact-text">{message.content}</div>
                          </div>
                          <button
                            className="button-secondary"
                            type="button"
                            onClick={() => togglePin(message)}
                            disabled={busyAction === `unpin-${message.messageId}`}
                          >
                            {busyAction === `unpin-${message.messageId}` ? "Working..." : "Unpin"}
                          </button>
                        </div>
                      ))}
                    </div>
                  </section>
                )}

                <div className="message-list">
                  {conversation.messages.length === 0 ? (
                    <div className="notice">No messages yet. Send the first one.</div>
                  ) : (
                    conversation.messages.map((message) => (
                      <article
                        className={`message-bubble ${message.outgoing ? "message-bubble-own" : ""}`}
                        key={message.messageId}
                      >
                        <div className="message-toolbar">
                          <strong>{message.outgoing ? "You" : message.sender.name}</strong>
                          <button
                            className="button-secondary"
                            type="button"
                            onClick={() => togglePin(message)}
                            disabled={busyAction === `${message.pinned ? "unpin" : "pin"}-${message.messageId}`}
                          >
                            {message.pinned ? "Unpin" : "Pin"}
                          </button>
                        </div>
                        <p className="compact-text">{message.content}</p>
                        <div className="footer-note">
                          {formatDateTime(message.timestamp)}
                        </div>
                      </article>
                    ))
                  )}
                </div>

                <form className="auth-form" onSubmit={sendMessage}>
                  <label className="field">
                    <span>New message</span>
                    <textarea
                      value={draft}
                      onChange={(event) => setDraft(event.target.value)}
                      placeholder="Ask about ideas, schedule a meeting, or discuss a thesis topic"
                      maxLength={1200}
                    />
                  </label>
                  <button className="button" type="submit" disabled={!draft.trim() || busyAction === "send"}>
                    {busyAction === "send" ? "Sending..." : "Send message"}
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<MessagesPage />);
