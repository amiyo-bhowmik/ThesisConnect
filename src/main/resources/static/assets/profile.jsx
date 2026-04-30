const { useEffect, useState } = React;

function splitTags(value) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinTags(value) {
  return (value || []).join(", ");
}

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

function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    department: "",
    university: "",
    academicDetails: "",
    bio: "",
    researchInterests: "",
    skills: "",
    lookingForGroup: false
  });
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      window.location.href = "/login.html";
      return;
    }

    fetch("/api/profile/me", {
      headers: buildAuthHeaders()
    })
      .then((response) => handleApiResponse(response, "Could not load profile"))
      .then((data) => {
        setProfile(data);
        setFormData({
          name: data.name || "",
          email: data.email || "",
          department: data.department || "",
          university: data.university || "",
          academicDetails: data.academicDetails || "",
          bio: data.bio || "",
          researchInterests: joinTags(data.researchInterests),
          skills: joinTags(data.skills),
          lookingForGroup: !!data.lookingForGroup
        });
      })
      .catch((err) => setError(err.message));
  }, []);

  function updateField(event) {
    const { name, value, type, checked } = event.target;
    setFormData((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value
    }));
  }

  function saveProfile(event) {
    event.preventDefault();
    setStatus("");
    setError("");

    fetch("/api/profile/me", {
      method: "PUT",
      headers: {
        ...buildAuthHeaders({
          "Content-Type": "application/json"
        })
      },
      body: JSON.stringify({
        ...formData,
        researchInterests: splitTags(formData.researchInterests),
        skills: splitTags(formData.skills)
      })
    })
      .then((response) => handleApiResponse(response, "Profile update failed"))
      .then((data) => {
        setProfile(data);
        setFormData((current) => ({
          ...current,
          researchInterests: joinTags(data.researchInterests),
          skills: joinTags(data.skills)
        }));
        setStatus("Profile updated successfully.");
      })
      .catch((err) => setError(err.message));
  }

  function uploadPicture(event) {
    const file = event.target.files && event.target.files[0];
    if (!file) {
      return;
    }

    const body = new FormData();
    body.append("file", file);
    setUploading(true);
    setStatus("");
    setError("");

    fetch("/api/profile/me/picture", {
      method: "POST",
      headers: buildAuthHeaders(),
      body
    })
      .then((response) => handleApiResponse(response, "Image upload failed"))
      .then((data) => {
        setProfile(data);
        setStatus("Profile picture uploaded successfully.");
      })
      .catch((err) => setError(err.message))
      .finally(() => setUploading(false));
  }

  function logout() {
    window.localStorage.removeItem("thesisconnect_token");
    window.location.href = "/login.html";
  }

  function deleteProfile() {
    const confirmed = window.confirm(
      "Delete your profile permanently? This cannot be undone."
    );
    if (!confirmed) {
      return;
    }

    setDeleting(true);
    setStatus("");
    setError("");

    fetch("/api/profile/me", {
      method: "DELETE",
      headers: buildAuthHeaders()
    })
      .then((response) => {
        if (response.status === 204 || response.status === 200) {
          return null;
        }
        return handleApiResponse(response, "Profile deletion failed");
      })
      .then(() => {
        window.localStorage.removeItem("thesisconnect_token");
        window.location.href = "/login.html";
      })
      .catch((err) => {
        setDeleting(false);
        setError(err.message);
      });
  }

  if (error && !profile) {
    return (
      <div className="page-shell">
        <div className="error">{error}</div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="page-shell">
        <div className="notice card">Loading your ThesisConnect profile...</div>
      </div>
    );
  }

  const initials = getInitials(profile.name);

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-badge">TC</div>
          <div>ThesisConnect</div>
        </div>
        <nav className="nav-links">
          <a className="button-secondary" href="/home">Homepage</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="profile-grid">
        <aside className="panel stack">
          <div>
            {profile.profilePicture ? (
              <img className="avatar" src={profile.profilePicture} alt={profile.name} />
            ) : (
              <div className="avatar-placeholder">{initials}</div>
            )}
          </div>
          <div>
            <h2 className="section-title">{profile.name}</h2>
            <p className="muted">{profile.email}</p>
            <p className="muted">{profile.department || "Department not added yet"}</p>
            <p className="muted">{profile.university || "University not added yet"}</p>
          </div>
          <div>
            <label className="field">
              <span>Upload profile picture</span>
              <input type="file" accept="image/png,image/jpeg,image/webp" onChange={uploadPicture} />
            </label>
            <div className="footer-note">{uploading ? "Uploading image..." : "JPG, PNG, and WEBP up to 5MB."}</div>
          </div>
          <div>
            <div className="section-title">Bio</div>
            <p className="muted">{profile.bio || "Add a short bio to introduce your research direction."}</p>
          </div>
          <div>
            <div className="section-title">Research Interests</div>
            <div className="chip-row">
              {(profile.researchInterests || []).map((interest) => (
                <div className="chip" key={interest}>{interest}</div>
              ))}
            </div>
          </div>
          <div>
            <div className="section-title">Skills</div>
            <div className="chip-row">
              {(profile.skills || []).map((skill) => (
                <div className="chip" key={skill}>{skill}</div>
              ))}
            </div>
          </div>
        </aside>

        <main className="panel">
          <h1 className="page-title" style={{fontSize: "2.8rem"}}>Edit my profile</h1>
          <p className="helper">
            Update your personal details, academic background, research interests, skills,
            short bio, and thesis-group availability here.
          </p>

          {status && <div className="success">{status}</div>}
          {error && <div className="error">{error}</div>}

          <form className="auth-form" onSubmit={saveProfile}>
            <div className="info-grid">
              <label className="field">
                <span>Full name</span>
                <input name="name" value={formData.name} onChange={updateField} required />
              </label>
              <label className="field">
                <span>Email</span>
                <input type="email" name="email" value={formData.email} onChange={updateField} required />
              </label>
            </div>

            <div className="info-grid">
              <label className="field">
                <span>Department</span>
                <input name="department" value={formData.department} onChange={updateField} />
              </label>
              <label className="field">
                <span>University</span>
                <input name="university" value={formData.university} onChange={updateField} />
              </label>
            </div>

            <label className="field">
              <span>Academic details</span>
              <textarea
                name="academicDetails"
                value={formData.academicDetails}
                onChange={updateField}
                placeholder="Example: BSc in CSE, 3rd Year, CGPA 3.82"
              />
            </label>

            <label className="field">
              <span>Short bio</span>
              <textarea
                name="bio"
                value={formData.bio}
                onChange={updateField}
                placeholder="Add a short overview of your thesis goals and collaboration style."
              />
            </label>

            <div className="info-grid">
              <label className="field">
                <span>Research interests</span>
                <input
                  name="researchInterests"
                  value={formData.researchInterests}
                  onChange={updateField}
                  placeholder="Machine Learning, HCI, Data Mining"
                />
              </label>
              <label className="field">
                <span>Skills</span>
                <input
                  name="skills"
                  value={formData.skills}
                  onChange={updateField}
                  placeholder="Java, React, MySQL"
                />
              </label>
            </div>

            <label className="field inline-toggle">
              <input
                type="checkbox"
                name="lookingForGroup"
                checked={formData.lookingForGroup}
                onChange={updateField}
              />
              <span>Currently looking for a thesis group</span>
            </label>

            <div className="nav-links">
              <button className="button" type="submit">Save profile changes</button>
              <button
                className="button-danger"
                type="button"
                onClick={deleteProfile}
                disabled={deleting}
              >
                {deleting ? "Deleting profile..." : "Delete profile"}
              </button>
            </div>
          </form>
        </main>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<ProfilePage />);
