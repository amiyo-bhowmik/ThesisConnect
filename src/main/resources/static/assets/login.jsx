function LoginPage() {
  const [mode, setMode] = React.useState("login");
  const [formData, setFormData] = React.useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  const [error, setError] = React.useState("");
  const [status, setStatus] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [registerFieldsUnlocked, setRegisterFieldsUnlocked] = React.useState(false);

  React.useEffect(() => {
    const token = window.localStorage.getItem("thesisconnect_token");
    if (token) {
      window.location.href = "/home";
    }
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    const fieldNameMap = {
      register_display_name: "name",
      register_contact: "email",
      register_secret: "password",
      register_secret_confirm: "confirmPassword"
    };
    const targetField = fieldNameMap[name] || name;

    setFormData((current) => ({
      ...current,
      [targetField]: value
    }));
  }

  async function submitForm(event) {
    event.preventDefault();
    setError("");
    setStatus("");

    if (mode === "register" && formData.password !== formData.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);

    const payload = mode === "register"
      ? {
          name: formData.name,
          email: formData.email,
          password: formData.password
        }
      : {
          email: formData.email,
          password: formData.password
        };

    try {
      const response = await fetch(mode === "register" ? "/api/auth/register" : "/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });

      const rawBody = await response.text();
      let data = {};
      try {
        data = rawBody ? JSON.parse(rawBody) : {};
      } catch (parseError) {
        data = { message: rawBody };
      }

      if (!response.ok) {
        if (mode === "register" && response.status === 409) {
          throw new Error("Email is already taken");
        }
        throw new Error(data.message || data.detail || data.error || "Authentication failed");
      }

      window.localStorage.setItem("thesisconnect_token", data.token);
      setStatus(mode === "register" ? "Registration successful." : "Login successful.");
      window.location.href = "/home";
    } catch (err) {
      setError(err.message || "Authentication failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-shell">
      <div className="hero auth-card">
        <section className="auth-panel">
          <div className="brand">
            <div className="brand-badge">TC</div>
            <div>ThesisConnect</div>
          </div>
          <h1 className="page-title" style={{marginTop: "28px"}}>Create your account or sign in with JWT access.</h1>
          <p className="helper">
            Register with your own email and password, then manage your ThesisConnect profile through token-based authentication.
          </p>
          <div className="footer-note">
            Your token is stored in the browser and sent with profile API requests.
          </div>
        </section>

        <section className="auth-form-wrap">
          <form style={{display: "none"}} autoComplete="on" aria-hidden="true">
            <input type="text" name="username" autoComplete="username" />
            <input type="password" name="password" autoComplete="current-password" />
          </form>
          <div className="nav-links" style={{marginBottom: "12px"}}>
            <button
              className={mode === "login" ? "button" : "button-secondary"}
              type="button"
              onClick={() => {
                setMode("login");
                setError("");
                setStatus("");
              }}
            >
              Login
            </button>
            <button
              className={mode === "register" ? "button" : "button-secondary"}
              type="button"
              onClick={() => {
                setMode("register");
                setError("");
                setStatus("");
                setRegisterFieldsUnlocked(false);
              }}
            >
              Register
            </button>
          </div>
          <h2>{mode === "register" ? "Create account" : "Welcome back"}</h2>
          <p className="muted">
            {mode === "register"
              ? "Start with your name, email, and password. You can complete the rest of your profile afterward."
              : "Sign in to manage your profile, academic details, bio, and profile picture."}
          </p>
          {error && <div className="error">{error}</div>}
          {status && <div className="success">{status}</div>}
          <form className="auth-form" onSubmit={submitForm} autoComplete="off">
            {mode === "register" && (
              <div className="field">
                <label htmlFor="name">Full name</label>
                <input
                  id="name"
                  name="register_display_name"
                  type="text"
                  value={formData.name}
                  onChange={updateField}
                  readOnly={!registerFieldsUnlocked}
                  onFocus={() => setRegisterFieldsUnlocked(true)}
                  autoComplete="nope"
                  required
                />
              </div>
            )}
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name={mode === "register" ? "register_contact" : "email"}
                type="email"
                value={formData.email}
                onChange={updateField}
                readOnly={mode === "register" && !registerFieldsUnlocked}
                onFocus={() => mode === "register" && setRegisterFieldsUnlocked(true)}
                autoComplete={mode === "register" ? "nope" : "username"}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                name={mode === "register" ? "register_secret" : "password"}
                type="password"
                value={formData.password}
                onChange={updateField}
                readOnly={mode === "register" && !registerFieldsUnlocked}
                onFocus={() => mode === "register" && setRegisterFieldsUnlocked(true)}
                autoComplete={mode === "register" ? "new-password" : "current-password"}
                required
              />
            </div>
            {mode === "register" && (
              <div className="field">
                <label htmlFor="confirmPassword">Confirm password</label>
                <input
                  id="confirmPassword"
                  name="register_secret_confirm"
                  type="password"
                  value={formData.confirmPassword}
                  onChange={updateField}
                  readOnly={!registerFieldsUnlocked}
                  onFocus={() => setRegisterFieldsUnlocked(true)}
                  autoComplete="new-password"
                  required
                />
              </div>
            )}
            <button className="button" type="submit" disabled={submitting}>
              {submitting
                ? "Please wait..."
                : mode === "register"
                  ? "Register and continue"
                  : "Login to ThesisConnect"}
            </button>
          </form>
          <div className="footer-note">
            <a href="/home">Go to homepage</a>
          </div>
        </section>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<LoginPage />);
