import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  AlertTriangle,
  Bell,
  Building2,
  Check,
  ChevronRight,
  CircleDot,
  Clock3,
  Filter,
  Loader2,
  Lock,
  LogOut,
  RefreshCw,
  Search,
  Shield,
  UserPlus,
  X,
  UserRound,
} from "lucide-react";
import "./styles.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api/v1";

const columns = [
  { id: "TODO", label: "To do" },
  { id: "IN_PROGRESS", label: "In progress" },
  { id: "IN_REVIEW", label: "In review" },
  { id: "BLOCKED", label: "Blocked" },
  { id: "DONE", label: "Done" },
];

const nextStatus = {
  TODO: "IN_PROGRESS",
  IN_PROGRESS: "IN_REVIEW",
  IN_REVIEW: "DONE",
};

const priorityLabel = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
};

const roleOptions = ["ADMIN", "MANAGER", "MEMBER"];

function unwrap(payload) {
  return payload && Object.prototype.hasOwnProperty.call(payload, "data")
    ? payload.data
    : payload;
}

function decodeRoles(token) {
  if (!token) return [];

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const rawRole = payload.role || payload.roles || "";
    return roleOptions.filter((role) => String(rawRole).includes(role));
  } catch {
    return [];
  }
}

async function api(path, token, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    credentials: "include",
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const error = unwrap(payload);
    throw new Error(error?.message || error?.error || "Request failed");
  }

  return unwrap(payload);
}

function App() {
  const [token, setToken] = useState(
    () => localStorage.getItem("taskBoardToken") || "",
  );
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [orgId, setOrgId] = useState(
    () => localStorage.getItem("taskBoardOrgId") || "",
  );
  const [orgName, setOrgName] = useState("");
  const [memberUserId, setMemberUserId] = useState("");
  const [roleUserId, setRoleUserId] = useState("");
  const [selectedRoles, setSelectedRoles] = useState(["MEMBER"]);
  const [projectId, setProjectId] = useState(
    () => localStorage.getItem("taskBoardProjectId") || "",
  );
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [analytics, setAnalytics] = useState([]);
  const [query, setQuery] = useState("");
  const [priority, setPriority] = useState("ALL");
  const [assigneeId, setAssigneeId] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [streamConnected, setStreamConnected] = useState(false);
  const [pendingTask, setPendingTask] = useState(null);
  const [isSignup, setIsSignup] = useState(false);

  const [signupName, setSignupName] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");

  const [projectName, setProjectName] = useState("");
  const [projectDescription, setProjectDescription] = useState("");

  const [projectMemberUserId, setProjectMemberUserId] = useState("");

  const [taskTitle, setTaskTitle] = useState("");
  const [taskDescription, setTaskDescription] = useState("");
  const [taskPriority, setTaskPriority] = useState("MEDIUM");
  const [taskAssigneeId, setTaskAssigneeId] = useState("");
  const [taskDueDate, setTaskDueDate] = useState("");
  const [showCreateTask, setShowCreateTask] = useState(false);
  const [showCreateProject, setShowCreateProject] = useState(false);

  const authenticated = Boolean(token);
  const roles = useMemo(() => decodeRoles(token), [token]);
  const isAdmin = roles.includes("ADMIN");
  const canManageTasks = roles.some((role) =>
    ["ADMIN", "MANAGER"].includes(role),
  );
  const selectedProjectId = projectId || projects[0]?.id || "";

  useEffect(() => {
    if (token) {
      localStorage.setItem("taskBoardToken", token);
    } else {
      localStorage.removeItem("taskBoardToken");
    }
  }, [token]);

  useEffect(() => {
    if (orgId) localStorage.setItem("taskBoardOrgId", orgId);
  }, [orgId]);

  useEffect(() => {
    if (projectId) localStorage.setItem("taskBoardProjectId", projectId);
  }, [projectId]);

  useEffect(() => {
    if (!token) return;

    let stopped = false;

    async function connectStream() {
      try {
        const response = await fetch(`${API_BASE}/notifications/stream`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!response.ok || !response.body) return;

        setStreamConnected(true);
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!stopped) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const events = buffer.split("\n\n");
          buffer = events.pop() || "";

          for (const raw of events) {
            if (raw.includes("event:task-status-changed")) {
              const dataLine = raw
                .split("\n")
                .find((line) => line.startsWith("data:"));
              if (dataLine) {
                const update = JSON.parse(dataLine.replace("data:", "").trim());
                setNotice(
                  `Task #${update.taskId} moved to ${formatStatus(update.currentStatus)}`,
                );
                if (String(update.projectId) === String(selectedProjectId)) {
                  loadTasks();
                }
              }
            }
          }
        }
      } catch {
        setStreamConnected(false);
      }
    }

    connectStream();

    return () => {
      stopped = true;
      setStreamConnected(false);
    };
  }, [token, selectedProjectId]);

  async function signup(event) {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      await api("/auth/signup", null, {
        method: "POST",
        body: JSON.stringify({
          name: signupName,
          email: signupEmail,
          password: signupPassword,
        }),
      });

      // Login immediately after signup
      const loginResponse = await api("/auth/login", null, {
        method: "POST",
        body: JSON.stringify({
          email: signupEmail,
          password: signupPassword,
        }),
      });

      setToken(loginResponse.token);

      setNotice("Account created and signed in successfully");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function login(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await api("/auth/login", null, {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(result.token);
      setNotice("Signed in");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function createProject(event) {
    event.preventDefault();

    if (!orgId) {
      setError("Organization id is required");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const project = await api(`/organizations/${orgId}/projects`, token, {
        method: "POST",
        body: JSON.stringify({
          name: projectName,
          description: projectDescription,
        }),
      });

      setNotice(`Project ${project.name} created`);

      setProjectName("");
      setProjectDescription("");

      await loadProjects();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function addProjectMember(event) {
    event.preventDefault();

    if (!selectedProjectId || !projectMemberUserId) {
      setError("Project and user id required");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await api(
        `/organizations/${orgId}/projects/${selectedProjectId}/members`,
        token,
        {
          method: "POST",
          body: JSON.stringify({
            userId: Number(projectMemberUserId),
          }),
        },
      );

      setNotice("Project member added");
      setProjectMemberUserId("");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadProjects() {
    if (!orgId) {
      setError("Enter an organization id");
      return;
    }

    setError("");
    setLoading(true);

    try {
      const result = await api(`/organizations/${orgId}/projects`, token);
      setProjects(result || []);
      if (!projectId && result?.length) setProjectId(String(result[0].id));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function createOrganization(event) {
    event.preventDefault();
    if (!orgName.trim()) {
      setError("Enter an organization name");
      return;
    }

    setError("");
    setLoading(true);

    try {
      const result = await api("/organizations", token, {
        method: "POST",
        body: JSON.stringify({ name: orgName.trim() }),
      });
      setOrgId(String(result.id));
      setOrgName("");
      setNotice(`Created organization ${result.name}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function addOrganizationMember(event) {
    event.preventDefault();
    if (!orgId || !memberUserId.trim()) {
      setError("Enter an organization id and user id");
      return;
    }

    setError("");
    setLoading(true);

    try {
      await api(`/organizations/${orgId}/members`, token, {
        method: "POST",
        body: JSON.stringify({ userId: Number(memberUserId) }),
      });
      setNotice(`User ${memberUserId} added to organization`);
      setMemberUserId("");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function updateUserRoles(event) {
    event.preventDefault();
    if (!roleUserId.trim() || selectedRoles.length === 0) {
      setError("Enter a user id and at least one role");
      return;
    }

    setError("");
    setLoading(true);

    try {
      await api(`/users/${roleUserId}/roles`, token, {
        method: "PATCH",
        body: JSON.stringify({ roles: selectedRoles }),
      });
      setNotice(`Updated roles for user ${roleUserId}`);
      setRoleUserId("");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function createTask(event) {
    event.preventDefault();

    if (!selectedProjectId) {
      setError("Select project first");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await api(`/projects/${selectedProjectId}/tasks`, token, {
        method: "POST",
        body: JSON.stringify({
          title: taskTitle,
          description: taskDescription,
          priority: taskPriority,
          assigneeId: taskAssigneeId ? Number(taskAssigneeId) : null,
          dueDate: taskDueDate || null,
        }),
      });

      setNotice("Task created");

      setTaskTitle("");
      setTaskDescription("");
      setTaskPriority("MEDIUM");
      setTaskAssigneeId("");
      setTaskDueDate("");

      await loadTasks();
      await loadAnalytics();

      setShowCreateTask(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadTasks() {
    if (!selectedProjectId) return;

    setError("");
    setLoading(true);

    const params = new URLSearchParams({ page: "0", limit: "50" });
    if (priority !== "ALL") params.set("priority", priority);
    if (assigneeId.trim()) params.set("assigneeId", assigneeId.trim());

    try {
      const result = await api(
        `/projects/${selectedProjectId}/tasks?${params}`,
        token,
      );
      setTasks(result?.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadAnalytics() {
    if (!selectedProjectId) return;

    try {
      const result = await api(
        `/projects/${selectedProjectId}/tasks/analytics`,
        token,
      );
      setAnalytics(result || []);
    } catch {
      setAnalytics([]);
    }
  }

  function requestAdvance(task) {
    setPendingTask(task);
  }

  async function advanceTask() {
    if (!pendingTask) return;
    const task = pendingTask;
    const target = nextStatus[task.status];
    if (!target) return;

    setError("");
    setPendingTask(null);

    try {
      await api(
        `/projects/${selectedProjectId}/tasks/${task.id}/status`,
        token,
        {
          method: "PATCH",
          body: JSON.stringify({ status: target }),
        },
      );
      setNotice(`${task.title} moved to ${formatStatus(target)}`);
      await loadTasks();
      await loadAnalytics();
    } catch (err) {
      setError(err.message);
    }
  }

  async function signup(event) {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      await api("/auth/signup", null, {
        method: "POST",
        body: JSON.stringify({
          name: signupName,
          email: signupEmail,
          password: signupPassword,
        }),
      });

      setNotice("Account created successfully. Please sign in.");

      setSignupName("");
      setSignupEmail("");
      setSignupPassword("");

      setIsSignup(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    setToken("");
    setTasks([]);
    setProjects([]);
    setAnalytics([]);
    setNotice("");
    setError("");
  }

  function toggleRole(role) {
    setSelectedRoles((current) =>
      current.includes(role)
        ? current.filter((item) => item !== role)
        : [...current, role],
    );
  }

  function handleChangeToSignUp() {
    setError("");
    setIsSignup(true);
  }

  const filteredTasks = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return tasks;
    return tasks.filter((task) =>
      [task.title, task.description, task.assigneeId, task.id]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalized)),
    );
  }, [tasks, query]);

  const tasksByColumn = useMemo(() => {
    return columns.reduce((grouped, column) => {
      grouped[column.id] = filteredTasks.filter(
        (task) => task.status === column.id,
      );
      return grouped;
    }, {});
  }, [filteredTasks]);

  const stats = useMemo(() => {
    const overdue = analytics.reduce(
      (sum, item) => sum + (item.overdueTaskCount || 0),
      0,
    );
    const completed = tasks.filter((task) => task.status === "DONE").length;
    const blocked = tasks.filter((task) => task.status === "BLOCKED").length;
    return { overdue, completed, blocked };
  }, [analytics, tasks]);

  if (!authenticated) {
    return (
      <main className="auth-page">
        <section className="login-panel">
          <div className="brand-mark">
            <Activity size={24} />
          </div>

          <h1>Task Tracker</h1>

          {!isSignup ? (
            <form onSubmit={login} className="login-form">
              <label>
                Email
                <input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  type="email"
                />
              </label>

              <label>
                Password
                <input
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  type="password"
                />
              </label>

              {error && <p className="error-text">{error}</p>}

              <button type="submit" disabled={loading}>
                {loading ? (
                  <Loader2 className="spin" size={18} />
                ) : (
                  <Lock size={18} />
                )}
                Sign In
              </button>

              {/* <p className="auth-switch">
                Don't have an account?{" "}
                <span onClick={() => setIsSignup(true)}>Sign Up</span>
              </p> */}

              <p className="auth-switch">
                Don't have an account?{" "}
                <span onClick={handleChangeToSignUp}>Sign Up</span>
              </p>
            </form>
          ) : (
            <form onSubmit={signup} className="login-form">
              <label>
                Name
                <input
                  value={signupName}
                  onChange={(e) => setSignupName(e.target.value)}
                  type="text"
                />
              </label>

              <label>
                Email
                <input
                  value={signupEmail}
                  onChange={(e) => setSignupEmail(e.target.value)}
                  type="email"
                />
              </label>

              <label>
                Password
                <input
                  value={signupPassword}
                  onChange={(e) => setSignupPassword(e.target.value)}
                  type="password"
                />
              </label>

              {error && <p className="error-text">{error}</p>}

              <button type="submit" disabled={loading}>
                {loading ? (
                  <Loader2 className="spin" size={18} />
                ) : (
                  <UserPlus size={18} />
                )}
                Create Account
              </button>

              <p className="auth-switch">
                Already have an account?{" "}
                <span onClick={() => setIsSignup(false)}>Sign In</span>
              </p>
            </form>
          )}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="app-title">
          <span>
            <Activity size={20} />
          </span>
          <div>
            <strong>Task Tracker</strong>
            <small>{roles.length ? roles.join(" / ") : "Signed in"}</small>
          </div>
        </div>

        <div className="field-stack">
          <label>
            Organization id
            <input
              value={orgId}
              onChange={(event) => setOrgId(event.target.value)}
              placeholder="1"
            />
          </label>
          <button className="secondary" onClick={loadProjects}>
            <RefreshCw size={16} />
            Load projects
          </button>
        </div>

        <section className="admin-panel">
          <form onSubmit={createOrganization} className="mini-form">
            <label>
              New organization
              <input
                value={orgName}
                onChange={(event) => setOrgName(event.target.value)}
                placeholder="Acme Operations"
              />
            </label>
            <button className="secondary" type="submit">
              <Building2 size={16} />
              Create
            </button>
          </form>

          <form onSubmit={addOrganizationMember} className="mini-form">
            <label>
              Add member by user id
              <input
                value={memberUserId}
                onChange={(event) => setMemberUserId(event.target.value)}
                placeholder="42"
              />
            </label>
            <button className="secondary" type="submit">
              <UserPlus size={16} />
              Add member
            </button>
          </form>

          <form onSubmit={updateUserRoles} className="mini-form">
            <label>
              Change user role
              <input
                value={roleUserId}
                onChange={(event) => setRoleUserId(event.target.value)}
                placeholder="42"
              />
            </label>
            <div className="role-picker">
              {roleOptions.map((role) => (
                <button
                  key={role}
                  type="button"
                  className={selectedRoles.includes(role) ? "selected" : ""}
                  onClick={() => toggleRole(role)}
                >
                  {role}
                </button>
              ))}
            </div>
            <button className="secondary accent" type="submit">
              <Shield size={16} />
              Update roles
            </button>
          </form>
        </section>

        <button
          className="secondary"
          onClick={() => setShowCreateProject(true)}
        >
          <Building2 size={16} />
          Create Project
        </button>

        {showCreateProject && (
          <CreateProjectDialog
            onClose={() => setShowCreateProject(false)}
            onSubmit={createProject}
            projectName={projectName}
            setProjectName={setProjectName}
            projectDescription={projectDescription}
            setProjectDescription={setProjectDescription}
          />
        )}

        <div className="field-stack">
          <label>
            Project
            <select
              value={selectedProjectId}
              onChange={(event) => setProjectId(event.target.value)}
            >
              <option value="">Select project</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="status-list">
          <Metric
            icon={<AlertTriangle size={17} />}
            label="Overdue"
            value={stats.overdue}
          />
          <Metric
            icon={<Check size={17} />}
            label="Done"
            value={stats.completed}
          />
          <Metric
            icon={<CircleDot size={17} />}
            label="Blocked"
            value={stats.blocked}
          />
        </div>

        <button className="ghost" onClick={logout}>
          <LogOut size={16} />
          Sign out
        </button>
      </aside>

      <section className="board-area">
        <header className="topbar">
          <div>
            <p className="kicker">Task board</p>
            <h2>
              {projects.find(
                (project) => String(project.id) === String(selectedProjectId),
              )?.name || "Project workspace"}
            </h2>
          </div>

          <div className="topbar-actions">
            <button
              className="primary-action"
              onClick={() => setShowCreateTask(true)}
            >
              Create Task
            </button>

            <div className={`stream-pill ${streamConnected ? "online" : ""}`}>
              <Bell size={16} />
              {streamConnected ? "Live" : "Stream idle"}
            </div>
          </div>
        </header>

        {/* {roles.some((role) => ["ADMIN", "MANAGER"].includes(role)) &&
          selectedProjectId && (
            <section className="task-create-panel">
              <h3>Create Task</h3>

              <input
                placeholder="Title"
                value={taskTitle}
                onChange={(e) => setTaskTitle(e.target.value)}
              />

              <input
                placeholder="Description"
                value={taskDescription}
                onChange={(e) => setTaskDescription(e.target.value)}
              />

              <select
                value={taskPriority}
                onChange={(e) => setTaskPriority(e.target.value)}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>

              <input
                placeholder="Assignee Id"
                value={taskAssigneeId}
                onChange={(e) => setTaskAssigneeId(e.target.value)}
              />

              <input
                type="date"
                value={taskDueDate}
                onChange={(e) => setTaskDueDate(e.target.value)}
              />

              <button onClick={createTask}>Create Task</button>
            </section>
          )} */}

        <section className="toolbar">
          <div className="search-box">
            <Search size={17} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search tasks"
            />
          </div>
          <select
            value={priority}
            onChange={(event) => setPriority(event.target.value)}
          >
            <option value="ALL">All priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
          <div className="assignee-filter">
            <UserRound size={16} />
            <input
              value={assigneeId}
              onChange={(event) => setAssigneeId(event.target.value)}
              placeholder="Assignee id"
            />
          </div>
          <button
            onClick={() => {
              loadTasks();
              loadAnalytics();
            }}
            disabled={!selectedProjectId || loading}
          >
            {loading ? (
              <Loader2 className="spin" size={17} />
            ) : (
              <Filter size={17} />
            )}
            Apply
          </button>

          {/* <button
            className="primary-action"
            onClick={() => setShowCreateTask(true)}
          >
            Create Task
          </button> */}
        </section>

        {notice && <div className="notice">{notice}</div>}
        {error && <div className="error-bar">{error}</div>}

        <section className="board">
          {columns.map((column) => (
            <Column
              key={column.id}
              column={column}
              tasks={tasksByColumn[column.id] || []}
              onAdvance={requestAdvance}
              canManageTasks={canManageTasks}
            />
          ))}
        </section>
      </section>

      {pendingTask && (
        <ConfirmDialog
          task={pendingTask}
          onCancel={() => setPendingTask(null)}
          onConfirm={advanceTask}
        />
      )}

      {showCreateTask && (
        <CreateTaskDialog
          onClose={() => setShowCreateTask(false)}
          onSubmit={createTask}
          taskTitle={taskTitle}
          setTaskTitle={setTaskTitle}
          taskDescription={taskDescription}
          setTaskDescription={setTaskDescription}
          taskPriority={taskPriority}
          setTaskPriority={setTaskPriority}
          taskAssigneeId={taskAssigneeId}
          setTaskAssigneeId={setTaskAssigneeId}
          taskDueDate={taskDueDate}
          setTaskDueDate={setTaskDueDate}
        />
      )}
    </main>
  );
}

function Column({ column, tasks, onAdvance, canManageTasks }) {
  return (
    <div className="column">
      <div className="column-header">
        <span>{column.label}</span>
        <strong>{tasks.length}</strong>
      </div>
      <div className="task-list">
        {tasks.length === 0 ? (
          <div className="empty-state">No tasks</div>
        ) : (
          tasks.map((task) => (
            <TaskCard key={task.id} task={task} onAdvance={onAdvance} />
          ))
        )}
      </div>
    </div>
  );
}

function TaskCard({ task, onAdvance }) {
  const overdue =
    task.dueDate &&
    task.status !== "DONE" &&
    new Date(task.dueDate) < new Date();
  const target = nextStatus[task.status];

  return (
    <article className="task-card">
      <div className="task-head">
        <span className={`priority priority-${task.priority?.toLowerCase()}`}>
          {priorityLabel[task.priority] || task.priority}
        </span>
        <span className="task-id">#{task.id}</span>
      </div>
      <h3>{task.title}</h3>
      {task.description && <p>{task.description}</p>}
      <div className="task-meta">
        <span>
          <UserRound size={14} />{" "}
          {task.assigneeId ? `User ${task.assigneeId}` : "Unassigned"}
        </span>
        {task.dueDate && (
          <span className={overdue ? "overdue" : ""}>
            <Clock3 size={14} /> {formatDate(task.dueDate)}
          </span>
        )}
      </div>
      {target && (
        <button className="advance" onClick={() => onAdvance(task)}>
          {canManageActionLabel(task.status)} {formatStatus(target)}
          <ChevronRight size={16} />
        </button>
      )}
    </article>
  );
}

function canManageActionLabel() {
  return "Move to";
}

function ConfirmDialog({ task, onCancel, onConfirm }) {
  const target = nextStatus[task.status];

  return (
    <div className="dialog-backdrop" role="presentation">
      <section
        className="confirm-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
      >
        <button
          className="icon-button"
          onClick={onCancel}
          aria-label="Close confirmation"
        >
          <X size={18} />
        </button>
        <div className="dialog-icon">
          <ChevronRight size={22} />
        </div>
        <h2 id="confirm-title">Confirm status change</h2>
        <p>
          Move <strong>{task.title}</strong> from{" "}
          <strong>{formatStatus(task.status)}</strong> to{" "}
          <strong>{formatStatus(target)}</strong>?
        </p>
        <div className="dialog-actions">
          <button className="secondary" onClick={onCancel}>
            Cancel
          </button>
          <button className="primary-action" onClick={onConfirm}>
            Confirm change
          </button>
        </div>
      </section>
    </div>
  );
}

function Metric({ icon, label, value }) {
  return (
    <div className="metric">
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatStatus(status) {
  return status
    ?.replaceAll("_", " ")
    .toLowerCase()
    .replace(/^\w/, (letter) => letter.toUpperCase());
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
  }).format(new Date(value));
}

function CreateTaskDialog({
  onClose,
  onSubmit,
  taskTitle,
  setTaskTitle,
  taskDescription,
  setTaskDescription,
  taskPriority,
  setTaskPriority,
  taskAssigneeId,
  setTaskAssigneeId,
  taskDueDate,
  setTaskDueDate,
}) {
  return (
    <div className="dialog-backdrop">
      <section className="task-dialog">
        <div className="dialog-header">
          <h2>Create Task</h2>

          <button className="icon-button" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={onSubmit}>
          <input
            placeholder="Task Title"
            value={taskTitle}
            onChange={(e) => setTaskTitle(e.target.value)}
            required
          />

          <textarea
            placeholder="Description"
            value={taskDescription}
            onChange={(e) => setTaskDescription(e.target.value)}
          />

          <select
            value={taskPriority}
            onChange={(e) => setTaskPriority(e.target.value)}
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>

          <input
            placeholder="Assignee Id"
            value={taskAssigneeId}
            onChange={(e) => setTaskAssigneeId(e.target.value)}
          />

          <input
            type="date"
            value={taskDueDate}
            onChange={(e) => setTaskDueDate(e.target.value)}
          />

          <div className="dialog-actions">
            <button type="button" className="secondary" onClick={onClose}>
              Cancel
            </button>

            <button type="submit" className="primary-action">
              Create
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

function CreateProjectDialog({
  onClose,
  onSubmit,
  projectName,
  setProjectName,
  projectDescription,
  setProjectDescription,
}) {
  return (
    <div className="dialog-backdrop">
      <section className="task-dialog">
        <div className="dialog-header">
          <h2>Create Project</h2>

          <button className="icon-button" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={onSubmit}>
          <input
            value={projectName}
            onChange={(e) => setProjectName(e.target.value)}
            placeholder="Project Name"
          />

          <textarea
            value={projectDescription}
            onChange={(e) => setProjectDescription(e.target.value)}
            placeholder="Description"
          />

          <div className="dialog-actions">
            <button type="button" className="secondary" onClick={onClose}>
              Cancel
            </button>

            <button type="submit" className="primary-action">
              Create
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
