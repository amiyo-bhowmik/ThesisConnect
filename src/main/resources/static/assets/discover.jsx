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
function DiscoverPage() {
  const [students, setStudents] = useState([]);
  const [directoryLoading, setDirectoryLoading] = useState(true);
  const [directoryError, setDirectoryError] = useState("");
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [selectedStudentLoading, setSelectedStudentLoading] = useState(false);
  const [selectedStudentError, setSelectedStudentError] = useState("");
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
}
