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

function formatFileSize(bytes) {
  if (!bytes && bytes !== 0) {
    return "Unknown size";
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
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
  const [selectedMember, setSelectedMember] = useState(null);
  const [selectedDocumentId, setSelectedDocumentId] = useState(null);
  const [documentTitle, setDocumentTitle] = useState("");
  const [documentVisibility, setDocumentVisibility] = useState("PRIVATE");
  const [documentFile, setDocumentFile] = useState(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [versionFile, setVersionFile] = useState(null);
  const [groupMessages, setGroupMessages] = useState([]);
  const [discussionLoading, setDiscussionLoading] = useState(false);
  const [discussionDraft, setDiscussionDraft] = useState("");

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

  useEffect(() => {
    if (!group) {
      setSelectedDocumentId(null);
      return;
    }

    const documents = group.documents || [];
    if (documents.length === 0) {
      setSelectedDocumentId(null);
      return;
    }

    const stillExists = documents.some((document) => document.documentId === selectedDocumentId);
    if (!stillExists) {
      setSelectedDocumentId(documents[0].documentId);
    }
  }, [group, selectedDocumentId]);

  useEffect(() => {
    if (!group || !group.currentUserMember) {
      setGroupMessages([]);
      return;
    }

    loadGroupMessages(group.groupId);
  }, [group && group.groupId, group && group.currentUserMember]);

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

  function loadGroupMessages(currentGroupId) {
    setDiscussionLoading(true);

    fetch(`/api/messages/groups/${currentGroupId}`, {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load group discussion"))
      .then((data) => setGroupMessages(data))
      .catch((err) => setError(err.message))
      .finally(() => setDiscussionLoading(false));
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

  function uploadDocument(event) {
    event.preventDefault();
    if (!group || !documentFile) {
      return;
    }

    const formData = new FormData();
    formData.append("title", documentTitle);
    formData.append("visibility", documentVisibility);
    formData.append("file", documentFile);

    runGroupAction(
      `upload-document-${group.groupId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/documents`, {
          method: "POST",
          headers: buildAuthHeaders(),
          body: formData
        }).then((response) => handleApiResponse(response, "Could not upload document")),
      "Document uploaded successfully."
    );

    setDocumentTitle("");
    setDocumentVisibility("PRIVATE");
    setDocumentFile(null);
    event.target.reset();
  }

  function uploadNewVersion(event) {
    event.preventDefault();
    if (!group || !selectedDocument || !versionFile) {
      return;
    }

    const formData = new FormData();
    formData.append("file", versionFile);

    runGroupAction(
      `upload-version-${selectedDocument.documentId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/documents/${selectedDocument.documentId}/versions`, {
          method: "POST",
          headers: buildAuthHeaders(),
          body: formData
        }).then((response) => handleApiResponse(response, "Could not upload new version")),
      "New document version uploaded."
    );

    setVersionFile(null);
    event.target.reset();
  }

  function addComment(event) {
    event.preventDefault();
    if (!group || !selectedDocument || !commentDraft.trim()) {
      return;
    }

    const content = commentDraft.trim();

    runGroupAction(
      `comment-${selectedDocument.documentId}`,
      () =>
        fetch(`/api/groups/${group.groupId}/documents/${selectedDocument.documentId}/comments`, {
          method: "POST",
          headers: buildAuthHeaders({
            "Content-Type": "application/json"
          }),
          body: JSON.stringify({ content })
        }).then((response) => handleApiResponse(response, "Could not add comment")),
      "Comment added."
    );

    setCommentDraft("");
  }

  function postGroupMessage(event) {
    event.preventDefault();
    if (!group || !discussionDraft.trim()) {
      return;
    }

    setBusyAction(`discussion-${group.groupId}`);
    setStatus("");
    setError("");

    fetch(`/api/messages/groups/${group.groupId}`, {
      method: "POST",
      headers: buildAuthHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify({ content: discussionDraft.trim() })
    })
      .then((response) => handleApiResponse(response, "Could not send group message"))
      .then((data) => {
        setGroupMessages(data);
        setDiscussionDraft("");
        setStatus("Group discussion updated.");
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  function toggleGroupMessagePin(message) {
    if (!group) {
      return;
    }

    const action = message.pinned ? "unpin" : "pin";
    setBusyAction(`${action}-discussion-${message.messageId}`);
    setStatus("");
    setError("");

    fetch(`/api/messages/groups/${group.groupId}/${message.messageId}/${action}`, {
      method: "POST",
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, `Could not ${action} message`))
      .then((data) => {
        setGroupMessages(data);
        setStatus(message.pinned ? "Message unpinned." : "Message pinned.");
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusyAction(""));
  }

  async function downloadDocument(documentId, version) {
    if (!group) {
      return;
    }

    const actionKey = version ? `download-${documentId}-${version}` : `download-${documentId}`;
    setBusyAction(actionKey);
    setStatus("");
    setError("");

    try {
      const query = version ? `?version=${version}` : "";
      const response = await fetch(`/api/groups/${group.groupId}/documents/${documentId}/download${query}`, {
        headers: buildAuthHeaders()
      });

      if (response.status === 401) {
        window.localStorage.removeItem("thesisconnect_token");
        window.location.href = "/login.html";
        return;
      }

      if (!response.ok) {
        throw new Error("Could not download document");
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      const disposition = response.headers.get("content-disposition") || "";
      const match = disposition.match(/filename=\"?([^"]+)\"?/i);
      link.href = url;
      link.download = match ? match[1] : "document";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setStatus("Download started.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyAction("");
    }
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

  const allMembers = useMemo(() => {
    if (!group) return [];
    const members = group.members || [];
    const adminAlreadyMember = members.some((m) => m.userId === group.admin.userId);
    if (adminAlreadyMember) return members;
    return [{ ...group.admin, admin: true }, ...members];
  }, [group]);

  const documents = group ? (group.documents || []) : [];
  const selectedDocument = documents.find((document) => document.documentId === selectedDocumentId) || null;
  const pinnedGroupMessages = groupMessages.filter((message) => message.pinned);

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">TC</div>
          <div>ThesisConnect</div>
        </div>
        <nav className="nav-links">
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary" href="/messages.html">Messages</a>
          <a className="button-secondary" href="/home">Homepage</a>
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
              <span className="chip">{documents.length} visible document{documents.length === 1 ? "" : "s"}</span>
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

            {group.currentUserMember && (
              <section className="panel stack">
                <div className="section-title">Group discussion</div>

                {pinnedGroupMessages.length > 0 && (
                  <div className="stack">
                    {pinnedGroupMessages.map((message) => (
                      <div className="pinned-banner" key={`group-pin-${message.messageId}`}>
                        <div>
                          <strong>{message.authoredByCurrentUser ? "You" : message.sender.name}</strong>
                          <div className="compact-text">{message.content}</div>
                        </div>
                        <button
                          className="button-secondary"
                          type="button"
                          onClick={() => toggleGroupMessagePin(message)}
                          disabled={busyAction === `unpin-discussion-${message.messageId}`}
                        >
                          {busyAction === `unpin-discussion-${message.messageId}` ? "Working..." : "Unpin"}
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {discussionLoading ? (
                  <div className="notice">Loading group discussion...</div>
                ) : (
                  <div className="message-list">
                    {groupMessages.length === 0 ? (
                      <div className="notice">No discussion messages yet. Start the conversation with your group.</div>
                    ) : (
                      groupMessages.map((message) => (
                        <article
                          className={`message-bubble ${message.authoredByCurrentUser ? "message-bubble-own" : ""}`}
                          key={message.messageId}
                        >
                          <div className="message-toolbar">
                            <strong>{message.authoredByCurrentUser ? "You" : message.sender.name}</strong>
                            <button
                              className="button-secondary"
                              type="button"
                              onClick={() => toggleGroupMessagePin(message)}
                              disabled={busyAction === `${message.pinned ? "unpin" : "pin"}-discussion-${message.messageId}`}
                            >
                              {message.pinned ? "Unpin" : "Pin"}
                            </button>
                          </div>
                          <p className="compact-text">{message.content}</p>
                          <div className="footer-note">{formatDateTime(message.timestamp)}</div>
                        </article>
                      ))
                    )}
                  </div>
                )}

                <form className="auth-form" onSubmit={postGroupMessage}>
                  <label className="field">
                    <span>Post to the group</span>
                    <textarea
                      value={discussionDraft}
                      onChange={(event) => setDiscussionDraft(event.target.value)}
                      placeholder="Share updates, ideas, or questions with your thesis group"
                      maxLength={1200}
                    />
                  </label>
                  <button
                    className="button"
                    type="submit"
                    disabled={!discussionDraft.trim() || busyAction === `discussion-${group.groupId}`}
                  >
                    {busyAction === `discussion-${group.groupId}` ? "Posting..." : "Post message"}
                  </button>
                </form>
              </section>
            )}

            <div className="section-title">Thesis documents and collaboration</div>
            <div className="directory-layout">
              <div className="results-column stack">
                {group.currentUserMember && (
                  <section className="panel stack">
                    <div className="section-title">Upload document</div>
                    <form className="auth-form" onSubmit={uploadDocument}>
                      <label className="field">
                        <span>Document title</span>
                        <input
                          type="text"
                          value={documentTitle}
                          onChange={(event) => setDocumentTitle(event.target.value)}
                          placeholder="Proposal draft, paper review, final report"
                          maxLength={200}
                        />
                      </label>
                      <label className="field">
                        <span>Visibility</span>
                        <select
                          className="select-input"
                          value={documentVisibility}
                          onChange={(event) => setDocumentVisibility(event.target.value)}
                        >
                          <option value="PRIVATE">Private - only group members</option>
                          <option value="PUBLIC">Public - visible to everyone</option>
                        </select>
                      </label>
                      <label className="field">
                        <span>Choose file</span>
                        <input
                          type="file"
                          accept=".pdf,.doc,.docx,.txt,.rtf"
                          onChange={(event) => setDocumentFile(event.target.files[0] || null)}
                        />
                      </label>
                      <button
                        className="button"
                        type="submit"
                        disabled={!documentTitle.trim() || !documentFile || busyAction === `upload-document-${group.groupId}`}
                      >
                        {busyAction === `upload-document-${group.groupId}` ? "Uploading..." : "Upload document"}
                      </button>
                    </form>
                  </section>
                )}

                <section className="panel stack">
                  <div className="results-header">
                    <div className="section-title" style={{marginBottom: 0}}>Visible documents</div>
                  </div>
                  {documents.length === 0 ? (
                    <div className="notice">
                      No documents are visible yet. Private files will appear only for group members, while public files are visible to everyone.
                    </div>
                  ) : (
                    <div className="document-list">
                      {documents.map((document) => (
                        <article
                          className={`panel member-card ${selectedDocument && selectedDocument.documentId === document.documentId ? "student-card-active" : ""}`}
                          key={document.documentId}
                          onClick={() => setSelectedDocumentId(document.documentId)}
                          style={{ cursor: "pointer" }}
                        >
                          <div className="document-card-header">
                            <div>
                              <div className="student-name">{document.title}</div>
                              <div className="muted compact-text">{document.originalFileName}</div>
                            </div>
                            <div className={`status-badge ${document.visibility === "PUBLIC" ? "status-open" : "status-closed"}`}>
                              {document.visibility}
                            </div>
                          </div>
                          <div className="chip-row compact-chips">
                            <div className="chip">v{document.version}</div>
                            <div className="chip">{formatFileSize(document.fileSize)}</div>
                            <div className="chip">{document.comments.length} comment{document.comments.length === 1 ? "" : "s"}</div>
                          </div>
                          <div className="footer-note">
                            Uploaded by {document.uploadedBy.name} on {formatDateTime(document.uploadDate)}
                          </div>
                        </article>
                      ))}
                    </div>
                  )}
                </section>
              </div>

              <div className="panel detail-panel">
                <div className="results-header">
                  <div className="section-title" style={{marginBottom: 0}}>Document details</div>
                </div>

                {!selectedDocument ? (
                  <div className="notice">Select a document to view details, comments, and versions.</div>
                ) : (
                  <div className="stack">
                    <div className="detail-block">
                      <div className="detail-label">Title</div>
                      <div>{selectedDocument.title}</div>
                    </div>

                    <div className="chip-row compact-chips">
                      <div className="chip">Latest version: v{selectedDocument.version}</div>
                      <div className="chip">{selectedDocument.visibility}</div>
                      <div className="chip">{formatFileSize(selectedDocument.fileSize)}</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Uploaded by</div>
                      <div>{selectedDocument.uploadedBy.name} ({selectedDocument.uploadedBy.email})</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Uploaded on</div>
                      <div>{formatDateTime(selectedDocument.uploadDate)}</div>
                    </div>

                    <div className="nav-links">
                      <button
                        className="button"
                        type="button"
                        onClick={() => downloadDocument(selectedDocument.documentId)}
                        disabled={busyAction === `download-${selectedDocument.documentId}`}
                      >
                        {busyAction === `download-${selectedDocument.documentId}` ? "Preparing..." : "Download latest version"}
                      </button>
                    </div>

                    {selectedDocument.currentUserCanUploadNewVersion && (
                      <section className="panel stack">
                        <div className="section-title">Upload updated version</div>
                        <form className="auth-form" onSubmit={uploadNewVersion}>
                          <label className="field">
                            <span>Select updated file</span>
                            <input
                              type="file"
                              accept=".pdf,.doc,.docx,.txt,.rtf"
                              onChange={(event) => setVersionFile(event.target.files[0] || null)}
                            />
                          </label>
                          <button
                            className="button-secondary"
                            type="submit"
                            disabled={!versionFile || busyAction === `upload-version-${selectedDocument.documentId}`}
                          >
                            {busyAction === `upload-version-${selectedDocument.documentId}` ? "Uploading..." : "Upload new version"}
                          </button>
                        </form>
                      </section>
                    )}

                    <section className="detail-block">
                      <div className="detail-label">Version history</div>
                      {selectedDocument.versions.length === 0 ? (
                        <div className="notice">No versions available yet.</div>
                      ) : (
                        <div className="stack">
                          {selectedDocument.versions.map((version) => (
                            <div className="notification-card document-meta-card" key={version.versionId}>
                              <div className="document-card-header">
                                <strong>Version {version.versionNumber}</strong>
                                <button
                                  className="button-secondary"
                                  type="button"
                                  onClick={() => downloadDocument(selectedDocument.documentId, version.versionNumber)}
                                  disabled={busyAction === `download-${selectedDocument.documentId}-${version.versionNumber}`}
                                >
                                  {busyAction === `download-${selectedDocument.documentId}-${version.versionNumber}` ? "Preparing..." : "Download"}
                                </button>
                              </div>
                              <div className="muted compact-text">{version.originalFileName}</div>
                              <div className="footer-note">
                                {formatFileSize(version.fileSize)} • Uploaded by {version.uploadedBy.name} on {formatDateTime(version.uploadedAt)}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </section>

                    <section className="detail-block">
                      <div className="detail-label">Comments and feedback</div>
                      {selectedDocument.currentUserCanComment && (
                        <form className="auth-form" onSubmit={addComment}>
                          <label className="field">
                            <span>Add comment</span>
                            <textarea
                              value={commentDraft}
                              onChange={(event) => setCommentDraft(event.target.value)}
                              placeholder="Share feedback on this document"
                              maxLength={1200}
                            />
                          </label>
                          <button
                            className="button-secondary"
                            type="submit"
                            disabled={!commentDraft.trim() || busyAction === `comment-${selectedDocument.documentId}`}
                          >
                            {busyAction === `comment-${selectedDocument.documentId}` ? "Posting..." : "Post comment"}
                          </button>
                        </form>
                      )}

                      {selectedDocument.comments.length === 0 ? (
                        <div className="notice">No comments yet.</div>
                      ) : (
                        <div className="stack">
                          {selectedDocument.comments.map((comment) => (
                            <div className="notification-card document-meta-card" key={comment.commentId}>
                              <div className="document-card-header">
                                <strong>{comment.author.name}</strong>
                                <span className={`status-badge ${comment.authorIsGroupMember ? "status-open" : "status-closed"}`}>
                                  {comment.authorScopeLabel}
                                </span>
                              </div>
                              <div className="muted compact-text">{comment.author.email}</div>
                              <p className="compact-text">{comment.content}</p>
                              <div className="footer-note">{formatDateTime(comment.timestamp)}</div>
                            </div>
                          ))}
                        </div>
                      )}
                    </section>
                  </div>
                )}
              </div>
            </div>

            <div className="section-title">Members and profiles</div>
            <div className="directory-layout">
              <div className="results-column">
                <div className="group-members-grid">
                  {allMembers.map((member) => (
                    <article
                      className={`panel member-card ${selectedMember && selectedMember.userId === member.userId ? "student-card-active" : ""}`}
                      key={member.userId}
                      onClick={() => setSelectedMember(member)}
                      style={{ cursor: "pointer" }}
                    >
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
                    </article>
                  ))}
                </div>
              </div>

              <div className="panel detail-panel">
                <div className="results-header">
                  <div className="section-title" style={{marginBottom: 0}}>Member profile</div>
                </div>
                {!selectedMember ? (
                  <div className="notice">Click a member card to view their full profile.</div>
                ) : (
                  <div className="stack">
                    <div className="student-identity">
                      {selectedMember.profilePicture ? (
                        <img className="avatar" src={selectedMember.profilePicture} alt={selectedMember.name} />
                      ) : (
                        <div className="avatar-placeholder">{getInitials(selectedMember.name)}</div>
                      )}
                      <div>
                        <h3 className="section-title" style={{marginBottom: "8px"}}>{selectedMember.name}</h3>
                        <p className="muted compact-text">{selectedMember.email}</p>
                        <p className="muted compact-text">{selectedMember.university || "University not added yet"}</p>
                      </div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Role</div>
                      <div>{selectedMember.admin ? "Admin" : "Member"}</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Department</div>
                      <div>{selectedMember.department || "Department not added yet"}</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Academic details</div>
                      <div>{selectedMember.academicDetails || "Academic details not added yet"}</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Bio</div>
                      <div>{selectedMember.bio || "This member has not added a short bio yet."}</div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Research interests</div>
                      <div className="chip-row">
                        {(selectedMember.researchInterests || []).map((interest) => (
                          <div className="chip" key={interest}>{interest}</div>
                        ))}
                      </div>
                    </div>

                    <div className="detail-block">
                      <div className="detail-label">Skills</div>
                      <div className="chip-row">
                        {(selectedMember.skills || []).map((skill) => (
                          <div className="chip" key={skill}>{skill}</div>
                        ))}
                      </div>
                    </div>

                    <div className="nav-links">
                      <a
                        className="button-secondary"
                        href={`/messages.html?studentId=${encodeURIComponent(selectedMember.userId)}`}
                      >
                        Send direct message
                      </a>
                      {group.currentUserAdmin && !selectedMember.admin && (
                        <button
                          className="button-secondary"
                          type="button"
                          onClick={() => assignAdmin(selectedMember.userId)}
                          disabled={busyAction === `assign-admin-${selectedMember.userId}`}
                        >
                          {busyAction === `assign-admin-${selectedMember.userId}` ? "Updating..." : "Make admin"}
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
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
