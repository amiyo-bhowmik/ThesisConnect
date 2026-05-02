const features = [
  {
    title: "Profile-first setup",
    text: "Create a polished academic profile with department, university, research interests, skills, a short bio, and a profile picture."
  },
  {
    title: "Ready for collaboration",
    text: "ThesisConnect helps students surface the context future thesis partners need before joining a team."
  },
  {
    title: "Student discovery live",
    text: "Search by name or research interest, filter by department or university, view student profiles, and spot who is looking for thesis groups."
  },
  {
    title: "Simple workspace hub",
    text: "Use this homepage to jump directly to profile editing, partner discovery, or logout after authentication."
  }
];

function HomePage() {
  React.useEffect(() => {
    const token = window.localStorage.getItem("thesisconnect_token");
    if (!token) {
      window.location.href = "/login.html";
    }
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
          <a className="button-secondary" href="/groups.html">Thesis groups</a>
          <a className="button-secondary" href="/notifications.html">Notifications</a>
          <button className="button" type="button" onClick={logout}>Logout</button>
        </nav>
      </header>

      <section className="hero">
        <div>
          <h1>Welcome to your ThesisConnect homepage.</h1>
          <div className="nav-links" style={{marginTop: "24px"}}>
            <a className="button" href="/profile.html">Edit My Profile</a>
            <a className="button-secondary" href="/discover.html">Discover Thesis Partners</a>
            <a className="button-secondary" href="/groups.html">Open Thesis Groups</a>
            <a className="button-secondary" href="/notifications.html">Open Notifications</a>
          </div>
          <div className="hero-stats">
            <div className="mini-card">
              <h3>9</h3>
              <p className="muted">Profile and discovery capabilities now available.</p>
            </div>
            <div className="mini-card">
              <h3>Name</h3>
              <p className="muted">Student search now supports searching by student name too.</p>
            </div>
            <div className="mini-card">
              <h3>JWT</h3>
              <p className="muted">Protected pages stay behind token-based authentication.</p>
            </div>
          </div>
        </div>

        <div className="panel stack">
          <div>
            <div className="section-title">Quick Access</div>
            <p className="muted">
              Go to profile editing to update your academic presence, or open discovery
              to search students by name, interest, department, university, and availability.
            </p>
          </div>
          <div className="nav-links">
            <a className="button-secondary" href="/profile.html">Edit my profile</a>
            <a className="button-secondary" href="/discover.html">Discover thesis partners</a>
            <a className="button-secondary" href="/groups.html">Open thesis groups</a>
            <a className="button-secondary" href="/notifications.html">Open notifications</a>
          </div>
          <div>
            <div className="section-title">Session</div>
            <p className="muted">
              Use logout when you want to end the current session and return to the login page.
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
