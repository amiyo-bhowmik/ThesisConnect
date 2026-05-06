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

function getInitials(name) {
  return (name || "?")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

function buildStudentQuery(filters) {
  const params = new URLSearchParams();

  if (filters.name.trim()) {
    params.set("name", filters.name.trim());
  }

  if (filters.email.trim()) {
    params.set("email", filters.email.trim());
  }

  if (filters.interest.trim()) {
    params.set("interest", filters.interest.trim());
  }

  if (filters.department.trim()) {
    params.set("department", filters.department.trim());
  }

  if (filters.university.trim()) {
    params.set("university", filters.university.trim());
  }

  if (filters.lookingForGroupOnly) {
    params.set("lookingForGroup", "true");
  }

  const queryString = params.toString();
  return queryString ? `/api/profile/students?${queryString}` : "/api/profile/students";
}

function DiscoverPage() {
  const [students, setStudents] = useState([]);
  const [directoryLoading, setDirectoryLoading] = useState(true);
  const [directoryError, setDirectoryError] = useState("");
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [selectedStudentLoading, setSelectedStudentLoading] = useState(false);
  const [selectedStudentError, setSelectedStudentError] = useState("");
  const [hasUnreadNotifications, setHasUnreadNotifications] = useState(false);
  const [directoryFilters, setDirectoryFilters] = useState({
    name: "",
    email: "",
    interest: "",
    department: "",
    university: "",
    lookingForGroupOnly: false
  });

  useEffect(() => {
    if (!getToken()) {
      window.location.href = "/login.html";
      return;
    }

    loadStudents(directoryFilters);

    fetch("/api/groups/notifications/unread", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load unread notification status"))
      .then((data) => setHasUnreadNotifications(!!data))
      .catch(() => setHasUnreadNotifications(false));
  }, []);

  function loadStudents(filters) {
    setDirectoryLoading(true);
    setDirectoryError("");

    fetch(buildStudentQuery(filters), {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load students"))
      .then((data) => {
        setStudents(data);
        if (data.length === 0) {
          setSelectedStudentId(null);
          setSelectedStudent(null);
          return;
        }

        const preservedStudent = data.find((student) => student.userId === selectedStudentId);
        const nextStudent = preservedStudent || data[0];
        setSelectedStudentId(nextStudent.userId);
        loadStudentProfile(nextStudent.userId);
      })
      .catch((err) => {
        setDirectoryError(err.message);
        setStudents([]);
        setSelectedStudentId(null);
        setSelectedStudent(null);
      })
      .finally(() => setDirectoryLoading(false));
  }

  function loadStudentProfile(userId) {
    setSelectedStudentLoading(true);
    setSelectedStudentError("");

    fetch(`/api/profile/students/${userId}`, {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load student profile"))
      .then((data) => setSelectedStudent(data))
      .catch((err) => setSelectedStudentError(err.message))
      .finally(() => setSelectedStudentLoading(false));
  }

  function updateDirectoryField(event) {
    const { name, value, type, checked } = event.target;
    setDirectoryFilters((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value
    }));
  }

  function searchStudents(event) {
    event.preventDefault();
    loadStudents(directoryFilters);
  }

  function clearFilters() {
    const clearedFilters = {
      name: "",
      email: "",
      interest: "",
      department: "",
      university: "",
      lookingForGroupOnly: false
    };
    setDirectoryFilters(clearedFilters);
    loadStudents(clearedFilters);
  }

  function openStudent(studentId) {
    setSelectedStudentId(studentId);
    loadStudentProfile(studentId);
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
          <a className="button-secondary" href="/messages.html">Inbox</a>
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary nav-notification-link" href="/notifications.html">
            Notifications
            {hasUnreadNotifications && <span className="nav-notification-dot" aria-label="Unread notifications" />}
          </a>
          <a className="button-secondary" href="/home">Homepage</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="panel stack">
        <div>
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Discover thesis partners</h1>
          <p className="helper">
            Search students by name, email, or research interests, filter by department or university,
            view full profiles, and quickly see who is currently looking for a thesis group.
          </p>
        </div>

        <form className="auth-form" onSubmit={searchStudents}>
          <div className="directory-grid">
            <label className="field">
              <span>Name</span>
              <input
                name="name"
                value={directoryFilters.name}
                onChange={updateDirectoryField}
                placeholder="Search by student name"
              />
            </label>
            <label className="field">
              <span>Email</span>
              <input
                name="email"
                value={directoryFilters.email}
                onChange={updateDirectoryField}
                placeholder="Search by student email"
              />
            </label>
            <label className="field">
              <span>Research interest</span>
              <input
                name="interest"
                value={directoryFilters.interest}
                onChange={updateDirectoryField}
                placeholder="AI, Cybersecurity, NLP"
              />
            </label>
            <label className="field">
              <span>Department</span>
              <input
                name="department"
                value={directoryFilters.department}
                onChange={updateDirectoryField}
                placeholder="Computer Science"
              />
            </label>
            <label className="field">
              <span>University</span>
              <input
                name="university"
                value={directoryFilters.university}
                onChange={updateDirectoryField}
                placeholder="BRAC University"
              />
            </label>
          </div>

          <div className="directory-toolbar">
            <label className="field inline-toggle">
              <input
                type="checkbox"
                name="lookingForGroupOnly"
                checked={directoryFilters.lookingForGroupOnly}
                onChange={updateDirectoryField}
              />
              <span>Show only students looking for thesis groups</span>
            </label>
            <div className="nav-links">
              <button className="button" type="submit">Search students</button>
              <button className="button-secondary" type="button" onClick={clearFilters}>Clear filters</button>
            </div>
          </div>
        </form>

        {directoryError && <div className="error">{directoryError}</div>}

        <div className="directory-layout">
          <div className="results-column">
            <div className="results-header">
              <div className="section-title" style={{marginBottom: 0}}>Student results</div>
              <div className="footer-note">
                {directoryLoading ? "Searching students..." : `${students.length} student${students.length === 1 ? "" : "s"} found`}
              </div>
            </div>

            {directoryLoading ? (
              <div className="notice">Loading discovery results...</div>
            ) : students.length === 0 ? (
              <div className="notice">
                No students matched your current search. Try broadening the research interest or filter values.
              </div>
            ) : (
              <div className="student-list">
                {students.map((student) => (
                  <button
                    type="button"
                    key={student.userId}
                    className={`student-card ${selectedStudentId === student.userId ? "student-card-active" : ""}`}
                    onClick={() => openStudent(student.userId)}
                  >
                    <div className="student-card-head">
                      <div className="student-identity">
                        {student.profilePicture ? (
                          <img className="student-avatar" src={student.profilePicture} alt={student.name} />
                        ) : (
                          <div className="student-avatar student-avatar-placeholder">
                            {getInitials(student.name)}
                          </div>
                        )}
                        <div>
                          <div className="student-name">{student.name}</div>
                          <div className="muted compact-text">
                            {student.department || "Department not added"}
                          </div>
                        </div>
                      </div>
                      <span className={`status-badge ${student.lookingForGroup ? "status-open" : "status-closed"}`}>
                        {student.lookingForGroup ? "Looking for group" : "Not searching"}
                      </span>
                    </div>

                    <div className="compact-text muted">
                      {student.university || "University not added yet"}
                    </div>

                    <div className="chip-row compact-chips">
                      {(student.researchInterests || []).slice(0, 3).map((interest) => (
                        <div className="chip" key={interest}>{interest}</div>
                      ))}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="panel detail-panel">
            <div className="results-header">
              <div className="section-title" style={{marginBottom: 0}}>Student profile</div>
              {selectedStudent && (
                <span className={`status-badge ${selectedStudent.lookingForGroup ? "status-open" : "status-closed"}`}>
                  {selectedStudent.lookingForGroup ? "Open to join a thesis group" : "Currently not looking"}
                </span>
              )}
            </div>

            {selectedStudentLoading ? (
              <div className="notice">Loading selected student profile...</div>
            ) : selectedStudentError ? (
              <div className="error">{selectedStudentError}</div>
            ) : !selectedStudent ? (
              <div className="notice">Select a student from the results list to view the full profile.</div>
            ) : (
              <div className="stack">
                <div className="student-identity">
                  {selectedStudent.profilePicture ? (
                    <img className="avatar" src={selectedStudent.profilePicture} alt={selectedStudent.name} />
                  ) : (
                    <div className="avatar-placeholder">{getInitials(selectedStudent.name)}</div>
                  )}
                  <div>
                    <h3 className="section-title" style={{marginBottom: "8px"}}>{selectedStudent.name}</h3>
                    <p className="muted compact-text">{selectedStudent.email}</p>
                    <p className="muted compact-text">{selectedStudent.university || "University not added yet"}</p>
                  </div>
                </div>

                <div className="detail-block">
                  <div className="detail-label">Department</div>
                  <div>{selectedStudent.department || "Department not added yet"}</div>
                </div>

                <div className="detail-block">
                  <div className="detail-label">Academic details</div>
                  <div>{selectedStudent.academicDetails || "Academic details not added yet"}</div>
                </div>

                <div className="detail-block">
                  <div className="detail-label">Bio</div>
                  <div>{selectedStudent.bio || "This student has not added a short bio yet."}</div>
                </div>

                <div className="detail-block">
                  <div className="detail-label">Research interests</div>
                  <div className="chip-row">
                    {(selectedStudent.researchInterests || []).map((interest) => (
                      <div className="chip" key={interest}>{interest}</div>
                    ))}
                  </div>
                </div>

                <div className="detail-block">
                  <div className="detail-label">Skills</div>
                  <div className="chip-row">
                    {(selectedStudent.skills || []).map((skill) => (
                      <div className="chip" key={skill}>{skill}</div>
                    ))}
                  </div>
                </div>

                <div className="nav-links">
                  <a
                    className="button"
                    href={`/messages.html?studentId=${encodeURIComponent(selectedStudent.userId)}`}
                  >
                    Send direct message
                  </a>
                </div>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<DiscoverPage />);
