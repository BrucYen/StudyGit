// 简易 JSON 文件数据库（零依赖，适合中小规模）
const fs = require('fs')
const path = require('path')
const crypto = require('crypto')

const DATA_DIR = path.join(__dirname, 'data')
const DB_FILE = path.join(DATA_DIR, 'db.json')

if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true })

const defaultDb = {
  users: [],          // {username, displayName, passwordHash, salt, createdAt}
  profiles: {},       // username -> {childName, province, city, grade}
  settings: {},       // username -> {...}
  tokens: {},         // token -> {username, expiresAt}
  wrongQuestions: {}, // username -> [{questionId, subject, stem, wrongCount, lastWrongAt, mastered}]
  studyRecords: {}    // username -> [{date, subject, correct, total, createdAt}]
}

function load() {
  try {
    if (!fs.existsSync(DB_FILE)) {
      fs.writeFileSync(DB_FILE, JSON.stringify(defaultDb, null, 2))
      return JSON.parse(JSON.stringify(defaultDb))
    }
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'))
  } catch (e) {
    console.error('DB load error:', e.message)
    return JSON.parse(JSON.stringify(defaultDb))
  }
}

function save(db) {
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2))
}

let db = load()

// 每 10 秒落盘一次（避免频繁写）
setInterval(() => save(db), 10000)

module.exports = {
  get: () => db,
  save: () => save(db),
  reload: () => { db = load() },

  // ---- 用户 ----
  findUser(username) {
    return db.users.find(u => u.username === username)
  },
  createUser({ username, displayName, passwordHash, salt }) {
    const user = { username, displayName, passwordHash, salt, createdAt: Date.now() }
    db.users.push(user)
    save(db)
    return user
  },

  // ---- Token ----
  createToken(username) {
    const token = crypto.randomBytes(32).toString('hex')
    db.tokens[token] = { username, expiresAt: Date.now() + 30 * 24 * 60 * 60 * 1000 } // 30天
    save(db)
    return token
  },
  verifyToken(token) {
    const t = db.tokens[token]
    if (!t) return null
    if (t.expiresAt < Date.now()) {
      delete db.tokens[token]
      save(db)
      return null
    }
    return t.username
  },
  revokeToken(token) {
    delete db.tokens[token]
    save(db)
  },

  // ---- 档案 ----
  getProfile(username) {
    return db.profiles[username] || null
  },
  saveProfile(username, profile) {
    db.profiles[username] = { ...profile, updatedAt: Date.now() }
    save(db)
    return db.profiles[username]
  },

  // ---- 设置 ----
  getSettings(username) {
    return db.settings[username] || null
  },
  saveSettings(username, settings) {
    db.settings[username] = { ...settings, updatedAt: Date.now() }
    save(db)
    return db.settings[username]
  },

  // ---- 错题本 ----
  getWrongQuestions(username) {
    return db.wrongQuestions[username] || []
  },
  addWrongQuestion(username, { questionId, subject, stem }) {
    if (!db.wrongQuestions[username]) db.wrongQuestions[username] = []
    const list = db.wrongQuestions[username]
    const exist = list.find(q => q.questionId === questionId)
    if (exist) {
      exist.wrongCount = (exist.wrongCount || 1) + 1
      exist.lastWrongAt = Date.now()
      exist.mastered = false
    } else {
      list.push({ questionId, subject, stem, wrongCount: 1, lastWrongAt: Date.now(), mastered: false })
    }
    save(db)
    return list
  },
  removeWrongQuestion(username, questionId) {
    if (!db.wrongQuestions[username]) return []
    db.wrongQuestions[username] = db.wrongQuestions[username].filter(q => q.questionId !== questionId)
    save(db)
    return db.wrongQuestions[username]
  },
  markMastered(username, questionId) {
    const list = db.wrongQuestions[username] || []
    const q = list.find(q => q.questionId === questionId)
    if (q) q.mastered = true
    save(db)
    return list
  },

  // ---- 学习记录 ----
  addStudyRecord(username, { date, subject, correct, total }) {
    if (!db.studyRecords[username]) db.studyRecords[username] = []
    db.studyRecords[username].push({ date, subject, correct, total, createdAt: Date.now() })
    save(db)
  },
  getStudyRecords(username, limit = 50) {
    return (db.studyRecords[username] || []).slice(-limit).reverse()
  }
}
