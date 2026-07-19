import { useState, useEffect } from 'react';

function getApiBase() {
  const saved = localStorage.getItem('apiBase');
  if (saved) return saved;
  return '/api';
}

function App() {
  const [apiBase, setApiBase] = useState(getApiBase);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(null);

  const [authMode, setAuthMode] = useState('login');
  const [authName, setAuthName] = useState('');
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authError, setAuthError] = useState('');

  const [tasks, setTasks] = useState([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedTags, setSelectedTags] = useState([]);

  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryColor, setNewCategoryColor] = useState('#4361ee');
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState('#10b981');
  const [showSettings, setShowSettings] = useState(false);
  const [showServerConfig, setShowServerConfig] = useState(false);
  const [serverInput, setServerInput] = useState(apiBase);

  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');

  const [filterCategory, setFilterCategory] = useState('');
  const [filterTag, setFilterTag] = useState('');

  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  useEffect(() => {
    if (token) {
      fetchTasks();
      fetchCategories();
      fetchTags();
    }
  }, [token, apiBase]);

  function saveServer(e) {
    e.preventDefault();
    const clean = serverInput.trim().replace(/\/+$/, '');
    if (!clean) return;
    localStorage.setItem('apiBase', clean);
    setApiBase(clean);
    setShowServerConfig(false);
  }

  // ---- Auth ----
  async function handleAuth(e) {
    e.preventDefault();
    setAuthError('');
    const endpoint = authMode === 'login'
      ? `${apiBase}/auth/login`
      : `${apiBase}/auth/register`;
    const body = authMode === 'login'
      ? { email: authEmail, password: authPassword }
      : { name: authName, email: authEmail, password: authPassword };
    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      localStorage.setItem('token', data.token);
      setToken(data.token);
      setUser(data.user);
    } catch (err) {
      setAuthError(err.message);
    }
  }

  function logout() {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setTasks([]);
    setCategories([]);
    setTags([]);
  }

  // ---- Fetch data ----
  async function fetchTasks() {
    const res = await fetch(`${apiBase}/tasks`, { headers });
    if (res.ok) setTasks(await res.json());
  }

  async function fetchCategories() {
    const res = await fetch(`${apiBase}/categories`, { headers });
    if (res.ok) setCategories(await res.json());
  }

  async function fetchTags() {
    const res = await fetch(`${apiBase}/tags`, { headers });
    if (res.ok) setTags(await res.json());
  }

  // ---- Tasks ----
  async function addTask(e) {
    e.preventDefault();
    if (!title.trim()) return;
    await fetch(`${apiBase}/tasks`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        title,
        description,
        category_id: selectedCategory || null,
        tag_ids: selectedTags,
      }),
    });
    setTitle('');
    setDescription('');
    setSelectedCategory('');
    setSelectedTags([]);
    fetchTasks();
  }

  async function toggleTask(task) {
    await fetch(`${apiBase}/tasks/${task.id}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify({ completed: !task.completed }),
    });
    fetchTasks();
  }

  async function deleteTask(id) {
    await fetch(`${apiBase}/tasks/${id}`, { method: 'DELETE', headers });
    fetchTasks();
  }

  function startEditing(task) {
    setEditingId(task.id);
    setEditTitle(task.title);
    setEditDescription(task.description || '');
  }

  async function saveEdit(e) {
    e.preventDefault();
    if (!editTitle.trim()) return;
    await fetch(`${apiBase}/tasks/${editingId}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify({ title: editTitle, description: editDescription }),
    });
    setEditingId(null);
    fetchTasks();
  }

  // ---- Categories ----
  async function addCategory(e) {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    await fetch(`${apiBase}/categories`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ name: newCategoryName, color: newCategoryColor }),
    });
    setNewCategoryName('');
    fetchCategories();
  }

  async function deleteCategory(id) {
    await fetch(`${apiBase}/categories/${id}`, { method: 'DELETE', headers });
    fetchCategories();
    fetchTasks();
  }

  // ---- Tags ----
  async function addTag(e) {
    e.preventDefault();
    if (!newTagName.trim()) return;
    await fetch(`${apiBase}/tags`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ name: newTagName, color: newTagColor }),
    });
    setNewTagName('');
    fetchTags();
  }

  async function deleteTag(id) {
    await fetch(`${apiBase}/tags/${id}`, { method: 'DELETE', headers });
    fetchTags();
    fetchTasks();
  }

  function toggleTagSelection(tagId) {
    setSelectedTags((prev) =>
      prev.includes(tagId) ? prev.filter((id) => id !== tagId) : [...prev, tagId]
    );
  }

  // ---- Filtering ----
  const filteredTasks = tasks.filter((t) => {
    if (filterCategory && t.category_id !== parseInt(filterCategory)) return false;
    if (filterTag && !t.tags.some((tag) => tag.id === parseInt(filterTag))) return false;
    return true;
  });

  const pending = filteredTasks.filter((t) => !t.completed);
  const completed = filteredTasks.filter((t) => t.completed);

  // ---- Auth Screen ----
  if (!token) {
    return (
      <div className="container">
        <h1>To Do List v{__APP_VERSION__}</h1>
        <div className="auth-box">
          <div className="auth-tabs">
            <button
              className={`auth-tab ${authMode === 'login' ? 'active' : ''}`}
              onClick={() => setAuthMode('login')}
            >
              Login
            </button>
            <button
              className={`auth-tab ${authMode === 'register' ? 'active' : ''}`}
              onClick={() => setAuthMode('register')}
            >
              Cadastro
            </button>
          </div>
          <form onSubmit={handleAuth} className="auth-form">
            {authMode === 'register' && (
              <input
                type="text"
                placeholder="Nome"
                value={authName}
                onChange={(e) => setAuthName(e.target.value)}
                className="input-field"
              />
            )}
            <input
              type="email"
              placeholder="Email"
              value={authEmail}
              onChange={(e) => setAuthEmail(e.target.value)}
              className="input-field"
            />
            <input
              type="password"
              placeholder="Senha"
              value={authPassword}
              onChange={(e) => setAuthPassword(e.target.value)}
              className="input-field"
            />
            {authError && <p className="error-msg">{authError}</p>}
            <button type="submit" className="btn-add">
              {authMode === 'login' ? 'Entrar' : 'Cadastrar'}
            </button>
          </form>
          <button
            className="btn-server"
            onClick={() => setShowServerConfig(!showServerConfig)}
          >
            Servidor: {apiBase}
          </button>
          {showServerConfig && (
            <form onSubmit={saveServer} className="server-form">
              <input
                type="text"
                value={serverInput}
                onChange={(e) => setServerInput(e.target.value)}
                placeholder="http://192.168.15.15:3002/api"
                className="input-field"
              />
              <button type="submit" className="btn-add">Salvar</button>
            </form>
          )}
        </div>
      </div>
    );
  }

  // ---- Main App ----
  return (
    <div className="container">
      <div className="header">
        <h1>To Do List v{__APP_VERSION__}</h1>
        <div className="header-actions">
          <button className="btn-settings" onClick={() => setShowSettings(!showSettings)}>
            {showSettings ? 'Tarefas' : 'Categorias & Tags'}
          </button>
          <button className="btn-logout" onClick={logout}>Sair</button>
        </div>
      </div>

      {showSettings ? (
        <div className="settings-panel">
          <h2 className="section-title">Categorias</h2>
          <form onSubmit={addCategory} className="inline-form">
            <input
              type="text"
              placeholder="Nome da categoria"
              value={newCategoryName}
              onChange={(e) => setNewCategoryName(e.target.value)}
              className="input-field"
            />
            <input
              type="color"
              value={newCategoryColor}
              onChange={(e) => setNewCategoryColor(e.target.value)}
              className="input-color"
            />
            <button type="submit" className="btn-add">Adicionar</button>
          </form>
          <ul className="chip-list">
            {categories.map((cat) => (
              <li key={cat.id} className="chip" style={{ background: cat.color + '22', borderColor: cat.color }}>
                <span style={{ color: cat.color }}>{cat.name}</span>
                <button className="chip-delete" onClick={() => deleteCategory(cat.id)}>x</button>
              </li>
            ))}
          </ul>

          <h2 className="section-title">Tags</h2>
          <form onSubmit={addTag} className="inline-form">
            <input
              type="text"
              placeholder="Nome da tag"
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              className="input-field"
            />
            <input
              type="color"
              value={newTagColor}
              onChange={(e) => setNewTagColor(e.target.value)}
              className="input-color"
            />
            <button type="submit" className="btn-add">Adicionar</button>
          </form>
          <ul className="chip-list">
            {tags.map((tag) => (
              <li key={tag.id} className="chip" style={{ background: tag.color + '22', borderColor: tag.color }}>
                <span style={{ color: tag.color }}>{tag.name}</span>
                <button className="chip-delete" onClick={() => deleteTag(tag.id)}>x</button>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <>
          <form onSubmit={addTask} className="add-form-v2">
            <input
              type="text"
              placeholder="O que precisa ser feito?"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="input-title"
            />
            <input
              type="text"
              placeholder="Descricao (opcional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="input-desc"
            />
            <div className="form-row">
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="select-field"
              >
                <option value="">Sem categoria</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
              <div className="tag-selector">
                {tags.map((tag) => (
                  <button
                    key={tag.id}
                    type="button"
                    className={`tag-btn ${selectedTags.includes(tag.id) ? 'selected' : ''}`}
                    style={{
                      background: selectedTags.includes(tag.id) ? tag.color : tag.color + '22',
                      color: selectedTags.includes(tag.id) ? 'white' : tag.color,
                      borderColor: tag.color,
                    }}
                    onClick={() => toggleTagSelection(tag.id)}
                  >
                    {tag.name}
                  </button>
                ))}
              </div>
              <button type="submit" className="btn-add">Adicionar</button>
            </div>
          </form>

          <div className="filters">
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="select-field"
            >
              <option value="">Todas categorias</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            <select
              value={filterTag}
              onChange={(e) => setFilterTag(e.target.value)}
              className="select-field"
            >
              <option value="">Todas tags</option>
              {tags.map((tag) => (
                <option key={tag.id} value={tag.id}>{tag.name}</option>
              ))}
            </select>
          </div>

          <div className="stats">
            <span>{pending.length} pendentes</span>
            <span>{completed.length} concluidas</span>
          </div>

          <ul className="task-list">
            {pending.map((task) => (
              <li key={task.id} className="task-item">
                {editingId === task.id ? (
                  <form onSubmit={saveEdit} className="edit-form">
                    <input type="text" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} className="input-title" />
                    <input type="text" value={editDescription} onChange={(e) => setEditDescription(e.target.value)} className="input-desc" />
                    <div className="edit-actions">
                      <button type="submit" className="btn-save">Salvar</button>
                      <button type="button" className="btn-cancel" onClick={() => setEditingId(null)}>Cancelar</button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="task-content" onClick={() => toggleTask(task)}>
                      <span className="checkbox">&#9744;</span>
                      <div className="task-info">
                        <p className="task-title">{task.title}</p>
                        {task.description && <p className="task-desc">{task.description}</p>}
                        <div className="task-meta">
                          {task.category_name && (
                            <span className="meta-category" style={{ background: task.category_color + '22', color: task.category_color }}>
                              {task.category_name}
                            </span>
                          )}
                          {task.tags && task.tags.map((tag) => (
                            <span key={tag.id} className="meta-tag" style={{ background: tag.color + '22', color: tag.color }}>
                              {tag.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="task-actions">
                      <button className="btn-edit" onClick={() => startEditing(task)}>Editar</button>
                      <button className="btn-delete" onClick={() => deleteTask(task.id)}>Excluir</button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>

          {completed.length > 0 && (
            <>
              <h2 className="section-title">Concluidas</h2>
              <ul className="task-list">
                {completed.map((task) => (
                  <li key={task.id} className="task-item completed">
                    <div className="task-content" onClick={() => toggleTask(task)}>
                      <span className="checkbox">&#9745;</span>
                      <div className="task-info">
                        <p className="task-title">{task.title}</p>
                        {task.description && <p className="task-desc">{task.description}</p>}
                        <div className="task-meta">
                          {task.category_name && (
                            <span className="meta-category" style={{ background: task.category_color + '22', color: task.category_color }}>
                              {task.category_name}
                            </span>
                          )}
                          {task.tags && task.tags.map((tag) => (
                            <span key={tag.id} className="meta-tag" style={{ background: tag.color + '22', color: tag.color }}>
                              {tag.name}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="task-actions">
                      <button className="btn-delete" onClick={() => deleteTask(task.id)}>Excluir</button>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}
    </div>
  );
}

export default App;
