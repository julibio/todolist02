-- ============================================
-- Todolist02 - Schema com Users, Categories e Tags
-- ============================================

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de categorias
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) DEFAULT '#4361ee',
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de tags
CREATE TABLE IF NOT EXISTS tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) DEFAULT '#10b981',
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Tabela de tarefas
CREATE TABLE IF NOT EXISTS tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN DEFAULT FALSE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela associativa: tasks <-> tags (N:N)
CREATE TABLE IF NOT EXISTS task_tags (
    task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_category_id ON tasks(category_id);
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_tags_user_id ON tags(user_id);
CREATE INDEX IF NOT EXISTS idx_task_tags_tag_id ON task_tags(tag_id);

-- Usuário padrão para testes (senha: 123456)
INSERT INTO users (name, email, password_hash)
VALUES ('Usuário Teste', 'teste@teste.com', '$2b$10$defaulthashplaceholder')
ON CONFLICT (email) DO NOTHING;

-- Categorias padrão
INSERT INTO categories (name, color, user_id)
SELECT name, color, 1 FROM (VALUES
    ('Trabalho', '#4361ee'),
    ('Pessoal', '#f59e0b'),
    ('Estudos', '#10b981')
) AS c(name, color)
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

-- Tags padrão
INSERT INTO tags (name, color, user_id)
SELECT name, color, 1 FROM (VALUES
    ('Urgente', '#ef4444'),
    ('Importante', '#f59e0b'),
    ('Opcional', '#6b7280')
) AS t(name, color)
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);
