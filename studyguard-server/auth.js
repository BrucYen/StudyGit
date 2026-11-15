// 密码工具：PBKDF2 加盐哈希（与 Android 端 Crypto.kt 保持一致的思路）
const crypto = require('crypto')

const ITERATIONS = 100000
const KEY_LEN = 32
const SALT_LEN = 16

function hashPassword(password) {
  const salt = crypto.randomBytes(SALT_LEN).toString('hex')
  const hash = crypto.pbkdf2Sync(password, salt, ITERATIONS, KEY_LEN, 'sha256').toString('hex')
  return { salt, hash }
}

function verifyPassword(password, salt, expectedHash) {
  const hash = crypto.pbkdf2Sync(password, salt, ITERATIONS, KEY_LEN, 'sha256').toString('hex')
  return hash === expectedHash
}

// 4-6 位数字 PIN 的哈希（家长 PIN）
function hashPin(pin) {
  const salt = crypto.randomBytes(SALT_LEN).toString('hex')
  const hash = crypto.pbkdf2Sync(pin, salt, ITERATIONS / 10, KEY_LEN, 'sha256').toString('hex')
  return { salt, hash }
}

module.exports = { hashPassword, verifyPassword, hashPin }
