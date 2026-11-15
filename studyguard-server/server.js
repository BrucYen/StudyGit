const express = require('express')
const fs = require('fs')
const path = require('path')
const db = require('./db')
const { hashPassword, verifyPassword, hashPin } = require('./auth')

const app = express()
const PORT = process.env.PORT || 3000

// 最新版本信息（更新 App 时改这里）
const LATEST_VERSION = {
  versionCode: 3,
  versionName: '1.2.0',
  downloadUrl: 'https://wytek.cloud/apk/studyguard-latest.apk',
  releaseNotes: '1. 新增云端数据同步（账号/设置/错题本/学习记录）\n2. 新增错题本功能：答错自动记录，答对自动移除\n3. 考试优先出错题，针对性巩固薄弱知识点\n4. 新增在线更新和题库同步',
  forceUpdate: false
}

app.use(express.json())

// ---- 认证中间件 ----
function authRequired(req, res, next) {
  const header = req.headers['authorization'] || ''
  const token = header.startsWith('Bearer ') ? header.slice(7) : ''
  const username = db.verifyToken(token)
  if (!username) return res.status(401).json({ error: '未登录或登录已过期' })
  req.user = username
  next()
}

// ---- 公开接口 ----
app.get('/health', (req, res) => res.json({ ok: true }))

app.get('/api/version', (req, res) => {
  const clientVersion = parseInt(req.query.versionCode) || 0
  res.json({ ...LATEST_VERSION, hasUpdate: clientVersion < LATEST_VERSION.versionCode })
})

app.get('/api/questions', (req, res) => {
  try {
    const data = fs.readFileSync(path.join(__dirname, 'data', 'questions.json'), 'utf-8')
    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    res.send(data)
  } catch (e) {
    res.status(500).json({ error: '题库加载失败' })
  }
})

// ---- 用户认证 ----
app.post('/api/auth/register', (req, res) => {
  const { username, password, displayName } = req.body || {}
  if (!username || !password) return res.status(400).json({ error: '用户名和密码不能为空' })
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) return res.status(400).json({ error: '用户名需 3-20 位字母数字下划线' })
  if (password.length < 6) return res.status(400).json({ error: '密码至少 6 位' })
  if (db.findUser(username)) return res.status(409).json({ error: '用户名已存在' })

  const { salt, hash } = hashPassword(password)
  db.createUser({ username, displayName: displayName || username, passwordHash: hash, salt })
  const token = db.createToken(username)
  res.json({ token, username, displayName: displayName || username })
})

app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body || {}
  const user = db.findUser(username)
  if (!user || !verifyPassword(password, user.salt, user.passwordHash)) {
    return res.status(401).json({ error: '用户名或密码错误' })
  }
  const token = db.createToken(username)
  res.json({ token, username, displayName: user.displayName })
})

app.post('/api/auth/logout', authRequired, (req, res) => {
  const token = (req.headers['authorization'] || '').slice(7)
  db.revokeToken(token)
  res.json({ ok: true })
})

// ---- 孩子档案 ----
app.get('/api/profile', authRequired, (req, res) => {
  res.json({ profile: db.getProfile(req.user) })
})

app.put('/api/profile', authRequired, (req, res) => {
  const { childName, province, city, grade } = req.body || {}
  const profile = db.saveProfile(req.user, {
    childName: childName || '',
    province: province || '广东省',
    city: city || '深圳市',
    grade: typeof grade === 'number' ? grade : 3
  })
  res.json({ profile })
})

// ---- 家长设置 ----
app.get('/api/settings', authRequired, (req, res) => {
  res.json({ settings: db.getSettings(req.user) })
})

app.put('/api/settings', authRequired, (req, res) => {
  const settings = db.saveSettings(req.user, req.body || {})
  res.json({ settings })
})

// ---- 错题本 ----
app.get('/api/wrong-questions', authRequired, (req, res) => {
  res.json({ questions: db.getWrongQuestions(req.user) })
})

app.post('/api/wrong-questions', authRequired, (req, res) => {
  const { questionId, subject, stem } = req.body || {}
  if (!questionId) return res.status(400).json({ error: '缺少 questionId' })
  const list = db.addWrongQuestion(req.user, { questionId, subject: subject || '', stem: stem || '' })
  res.json({ questions: list })
})

app.delete('/api/wrong-questions/:questionId', authRequired, (req, res) => {
  const list = db.removeWrongQuestion(req.user, req.params.questionId)
  res.json({ questions: list })
})

app.post('/api/wrong-questions/:questionId/mastered', authRequired, (req, res) => {
  const list = db.markMastered(req.user, req.params.questionId)
  res.json({ questions: list })
})

// ---- 学习记录 ----
app.post('/api/study-record', authRequired, (req, res) => {
  const { date, subject, correct, total } = req.body || {}
  db.addStudyRecord(req.user, {
    date: date || new Date().toISOString().slice(0, 10),
    subject: subject || '',
    correct: correct || 0,
    total: total || 0
  })
  res.json({ ok: true })
})

app.get('/api/study-records', authRequired, (req, res) => {
  const limit = parseInt(req.query.limit) || 50
  res.json({ records: db.getStudyRecords(req.user, limit) })
})

// ---- 静态资源：APK 下载 ----
const apkDir = path.join(__dirname, 'public', 'apk')
if (!fs.existsSync(apkDir)) fs.mkdirSync(apkDir, { recursive: true })
app.use('/apk', express.static(apkDir, {
  setHeaders: (res) => res.setHeader('Content-Type', 'application/vnd.android.package-archive')
}))

app.listen(PORT, '0.0.0.0', () => {
  console.log(`StudyGuard server running on port ${PORT}`)
})
