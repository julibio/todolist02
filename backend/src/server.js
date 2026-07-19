const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { Pool } = require('pg');

const app = express();
const port = 3002;

app.use(cors());
app.use(express.json());

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER || 'todouser',
  password: process.env.DB_PASSWORD || 'todopass123',
  database: process.env.DB_NAME || 'todolist02',
});

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret';

// Health check — usado pelo CI, pelo healthcheck do Docker e por monitoramento
app.get('/api/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok' });
  } catch {
    res.status(503).json({ status: 'db unavailable' });
  }
});

// ---- Middleware de autenticação ----
function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Token required' });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ error: 'Invalid token' });
  }
}

// ============================================
// AUTH ROUTES
// ============================================

app.post('/api/auth/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email and password are required' });
    }
    const existing = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Email already registered' });
    }
    const password_hash = await bcrypt.hash(password, 10);
    const result = await pool.query(
      'INSERT INTO users (name, email, password_hash) VALUES ($1, $2, $3) RETURNING id, name, email',
      [name, email, password_hash]
    );
    const user = result.rows[0];
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
    res.status(201).json({ user, token });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const user = result.rows[0];
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '7d' });
    res.json({ user: { id: user.id, name: user.name, email: user.email }, token });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ============================================
// CATEGORIES ROUTES
// ============================================

app.get('/api/categories', authMiddleware, async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM categories WHERE user_id = $1 ORDER BY name', [req.user.id]
  );
  res.json(result.rows);
});

app.post('/api/categories', authMiddleware, async (req, res) => {
  const { name, color } = req.body;
  if (!name) return res.status(400).json({ error: 'Name is required' });
  const result = await pool.query(
    'INSERT INTO categories (name, color, user_id) VALUES ($1, $2, $3) RETURNING *',
    [name, color || '#4361ee', req.user.id]
  );
  res.status(201).json(result.rows[0]);
});

app.delete('/api/categories/:id', authMiddleware, async (req, res) => {
  const result = await pool.query(
    'DELETE FROM categories WHERE id = $1 AND user_id = $2 RETURNING *',
    [req.params.id, req.user.id]
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Category not found' });
  res.json({ message: 'Category deleted' });
});

// ============================================
// TAGS ROUTES
// ============================================

app.get('/api/tags', authMiddleware, async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM tags WHERE user_id = $1 ORDER BY name', [req.user.id]
  );
  res.json(result.rows);
});

app.post('/api/tags', authMiddleware, async (req, res) => {
  const { name, color } = req.body;
  if (!name) return res.status(400).json({ error: 'Name is required' });
  const result = await pool.query(
    'INSERT INTO tags (name, color, user_id) VALUES ($1, $2, $3) RETURNING *',
    [name, color || '#10b981', req.user.id]
  );
  res.status(201).json(result.rows[0]);
});

app.delete('/api/tags/:id', authMiddleware, async (req, res) => {
  const result = await pool.query(
    'DELETE FROM tags WHERE id = $1 AND user_id = $2 RETURNING *',
    [req.params.id, req.user.id]
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Tag not found' });
  res.json({ message: 'Tag deleted' });
});

// ============================================
// TASKS ROUTES
// ============================================

app.get('/api/tasks', authMiddleware, async (req, res) => {
  const result = await pool.query(
    `SELECT t.*, c.name as category_name, c.color as category_color,
     COALESCE(json_agg(json_build_object('id', tg.id, 'name', tg.name, 'color', tg.color))
       FILTER (WHERE tg.id IS NOT NULL), '[]') as tags
     FROM tasks t
     LEFT JOIN categories c ON t.category_id = c.id
     LEFT JOIN task_tags tt ON t.id = tt.task_id
     LEFT JOIN tags tg ON tt.tag_id = tg.id
     WHERE t.user_id = $1
     GROUP BY t.id, c.name, c.color
     ORDER BY t.created_at DESC`,
    [req.user.id]
  );
  res.json(result.rows);
});

app.post('/api/tasks', authMiddleware, async (req, res) => {
  const { title, description, category_id, tag_ids } = req.body;
  if (!title || title.trim() === '') {
    return res.status(400).json({ error: 'Title is required' });
  }
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const taskResult = await client.query(
      'INSERT INTO tasks (title, description, user_id, category_id) VALUES ($1, $2, $3, $4) RETURNING *',
      [title.trim(), description?.trim() || null, req.user.id, category_id || null]
    );
    const task = taskResult.rows[0];
    if (tag_ids && tag_ids.length > 0) {
      for (const tagId of tag_ids) {
        await client.query('INSERT INTO task_tags (task_id, tag_id) VALUES ($1, $2)', [task.id, tagId]);
      }
    }
    await client.query('COMMIT');
    res.status(201).json(task);
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

app.put('/api/tasks/:id', authMiddleware, async (req, res) => {
  const { title, description, completed, category_id, tag_ids } = req.body;
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query(
      `UPDATE tasks SET title = COALESCE($1, title), description = COALESCE($2, description),
       completed = COALESCE($3, completed), category_id = COALESCE($4, category_id),
       updated_at = CURRENT_TIMESTAMP
       WHERE id = $5 AND user_id = $6 RETURNING *`,
      [title, description, completed, category_id, req.params.id, req.user.id]
    );
    if (result.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Task not found' });
    }
    if (tag_ids !== undefined) {
      await client.query('DELETE FROM task_tags WHERE task_id = $1', [req.params.id]);
      for (const tagId of tag_ids) {
        await client.query('INSERT INTO task_tags (task_id, tag_id) VALUES ($1, $2)', [req.params.id, tagId]);
      }
    }
    await client.query('COMMIT');
    res.json(result.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

app.delete('/api/tasks/:id', authMiddleware, async (req, res) => {
  const result = await pool.query(
    'DELETE FROM tasks WHERE id = $1 AND user_id = $2 RETURNING *',
    [req.params.id, req.user.id]
  );
  if (result.rows.length === 0) return res.status(404).json({ error: 'Task not found' });
  res.json({ message: 'Task deleted' });
});

app.listen(port, '0.0.0.0', () => {
  console.log(`Backend v2 running on port ${port}`);
});
