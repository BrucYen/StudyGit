#!/bin/bash
# 学伴守护 服务器端部署脚本
# 用法:
#   bash deploy.sh apk <apk路径> [versionCode] [versionName] [releaseNotes]
#   bash deploy.sh questions <questions.json路径>
#   bash deploy.sh both <apk路径> <questions.json路径> [versionCode] [versionName] [releaseNotes]
#   bash deploy.sh status          # 查看服务状态
#   bash deploy.sh restart         # 重启服务

set -e

SERVER_DIR="/home/ubuntu/studyguard-server"
APK_DIR="$SERVER_DIR/public/apk"
DATA_DIR="$SERVER_DIR/data"
SERVER_JS="$SERVER_DIR/server.js"

log() { echo "[$(date '+%H:%M:%S')] $1"; }

restart_service() {
    log "重启 studyguard 服务..."
    sudo systemctl restart studyguard
    sleep 2
}

verify() {
    log "验证接口..."
    local code=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:3000/health)
    if [ "$code" = "200" ]; then
        log "✅ 服务正常 (HTTP $code)"
        echo "--- 版本接口 ---"
        curl -s http://127.0.0.1:3000/api/version
        echo ""
    else
        log "❌ 服务异常 (HTTP $code)"
        sudo systemctl status studyguard --no-pager -l
        exit 1
    fi
}

update_version() {
    local vcode=$1
    local vname=$2
    local notes=$3
    log "更新版本号: code=$vcode, name=$vname"
    # 用 node 安全地修改 JSON 风格的 JS 常量
    node -e "
const fs = require('fs');
let s = fs.readFileSync('$SERVER_JS', 'utf8');
s = s.replace(/versionCode:\s*\d+/, 'versionCode: $vcode');
s = s.replace(/versionName:\s*'[^']*'/, \"versionName: '$vname'\");
s = s.replace(/releaseNotes:\s*'[^']*'/, \"releaseNotes: '$notes'\");
fs.writeFileSync('$SERVER_JS', s);
console.log('版本信息已更新');
"
}

case "$1" in
    apk)
        APK_SRC="$2"
        if [ -z "$APK_SRC" ] || [ ! -f "$APK_SRC" ]; then
            echo "用法: bash deploy.sh apk <apk路径> [versionCode] [versionName] [releaseNotes]"
            exit 1
        fi
        mkdir -p "$APK_DIR"
        cp "$APK_SRC" "$APK_DIR/studyguard-latest.apk"
        log "APK 已更新: $(du -h $APK_DIR/studyguard-latest.apk | cut -f1)"
        if [ -n "$3" ]; then
            update_version "$3" "${4:-1.0.0}" "${5:-优化体验}"
        fi
        restart_service
        verify
        ;;

    questions)
        Q_SRC="$2"
        if [ -z "$Q_SRC" ] || [ ! -f "$Q_SRC" ]; then
            echo "用法: bash deploy.sh questions <questions.json路径>"
            exit 1
        fi
        mkdir -p "$DATA_DIR"
        cp "$Q_SRC" "$DATA_DIR/questions.json"
        local count=$(node -e "console.log(JSON.parse(require('fs').readFileSync('$Q_SRC','utf8')).questions.length)")
        log "题库已更新: $count 道题"
        restart_service
        verify
        ;;

    both)
        APK_SRC="$2"
        Q_SRC="$3"
        if [ -z "$APK_SRC" ] || [ -z "$Q_SRC" ]; then
            echo "用法: bash deploy.sh both <apk路径> <questions.json路径> [versionCode] [versionName] [releaseNotes]"
            exit 1
        fi
        mkdir -p "$APK_DIR" "$DATA_DIR"
        cp "$APK_SRC" "$APK_DIR/studyguard-latest.apk"
        cp "$Q_SRC" "$DATA_DIR/questions.json"
        log "APK + 题库已更新"
        if [ -n "$4" ]; then
            update_version "$4" "${5:-1.0.0}" "${6:-优化体验}"
        fi
        restart_service
        verify
        ;;

    restart)
        restart_service
        verify
        ;;

    status)
        sudo systemctl status studyguard --no-pager -l
        ;;

    *)
        echo "学伴守护部署脚本"
        echo ""
        echo "用法:"
        echo "  bash deploy.sh apk <apk路径> [versionCode] [versionName] [releaseNotes]"
        echo "  bash deploy.sh questions <questions.json路径>"
        echo "  bash deploy.sh both <apk路径> <questions.json路径> [versionCode] [versionName] [releaseNotes]"
        echo "  bash deploy.sh restart"
        echo "  bash deploy.sh status"
        ;;
esac
