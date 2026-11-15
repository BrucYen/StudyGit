const express = require('express')
const fs = require('fs')
const path = require('path')

const app = express()
const PORT = process.env.PORT || 3000

// 最新版本信息（更新 App 时改这里）
const LATEST_VERSION = {
  versionCode: 2,
  versionName: '1.1.0',
  downloadUrl: 'https://wytek.cloud/apk/studyguard-latest.apk',
  releaseNotes: '1. 新增在线更新功能，可在 App 内检测并下载新版本\n2. 优化弹窗考试体验\n3. 修复已知问题',
  forceUpdate: false
}

app.use(express.json())

// 健康检查
app.get('/health', (req, res) => res.json({ ok: true }))

// 版本检查接口
app.get('/api/version', (req, res) => {
  const clientVersion = parseInt(req.query.versionCode) || 0
  const hasUpdate = clientVersion < LATEST_VERSION.versionCode
  res.json({
    ...LATEST_VERSION,
    hasUpdate
  })
})

// 题库下发接口
app.get('/api/questions', (req, res) => {
  try {
    const data = fs.readFileSync(path.join(__dirname, 'questions.json'), 'utf-8')
    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    res.send(data)
  } catch (e) {
    res.status(500).json({ error: '题库加载失败' })
  }
})

// APK 下载目录
app.use('/apk', express.static(path.join(__dirname, 'apks'), {
  setHeaders: (res) => {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive')
  }
}))

app.listen(PORT, '0.0.0.0', () => {
  console.log(`StudyGuard server running on port ${PORT}`)
})
