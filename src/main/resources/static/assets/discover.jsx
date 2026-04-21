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