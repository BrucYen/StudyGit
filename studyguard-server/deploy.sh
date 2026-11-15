#!/bin/bash
# 学伴守护后端一键部署脚本
# 在服务器上执行：bash deploy.sh

set -e

echo "=== 1. 检查 Node.js ==="
if ! command -v node &> /dev/null; then
    echo "安装 Node.js 20.x ..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
fi
node --version
npm --version

echo ""
echo "=== 2. 安装依赖 ==="
cd /home/ubuntu/studyguard-server
npm install --production

echo ""
echo "=== 3. 配置 systemd 服务 ==="
sudo cp studyguard.service /etc/systemd/system/studyguard.service
sudo systemctl daemon-reload
sudo systemctl enable studyguard
sudo systemctl restart studyguard
sleep 2
sudo systemctl status studyguard --no-pager | head -10

echo ""
echo "=== 4. 验证接口 ==="
sleep 1
echo "版本接口："
curl -s http://127.0.0.1:3000/api/version | head -c 200
echo ""
echo "题库接口："
curl -s http://127.0.0.1:3000/api/questions | head -c 100
echo ""

echo ""
echo "=== 部署完成 ==="
echo "服务运行在 http://127.0.0.1:3000"
echo "如需外网访问，请在腾讯云安全组放行 3000 端口，或配置 Nginx 反向代理。"
