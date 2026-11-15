# 学伴守护 一键上传部署脚本（Windows PowerShell）
# 用法:
#   .\upload.ps1 -Apk <本地APK路径> [-VersionCode 3] [-VersionName "1.2.0"] [-ReleaseNotes "更新说明"]
#   .\upload.ps1 -Questions <本地questions.json路径>
#   .\upload.ps1 -Apk <APK路径> -Questions <题库路径> [-VersionCode 3] ...
#   .\upload.ps1 -Status
#   .\upload.ps1 -Restart

param(
    [string]$Apk,
    [string]$Questions,
    [int]$VersionCode,
    [string]$VersionName,
    [string]$ReleaseNotes,
    [switch]$Status,
    [switch]$Restart
)

$Server = "ubuntu@1.14.156.163"
$RemoteTmp = "/tmp"
$RemoteDeploy = "/home/ubuntu/studyguard-server/deploy.sh"

function Remote-Exec {
    param([string]$Cmd)
    ssh $Server $Cmd
}

function Upload-File {
    param([string]$LocalPath, [string]$RemoteName)
    if (-not (Test-Path $LocalPath)) {
        Write-Host "❌ 文件不存在: $LocalPath" -ForegroundColor Red
        exit 1
    }
    $item = Get-Item $LocalPath
    Write-Host "📤 上传 $($item.Name) ($([math]::Round($item.Length/1MB,2)) MB)..."
    scp $LocalPath "${Server}:${RemoteTmp}/$RemoteName"
}

# 1) 状态查询
if ($Status) {
    Remote-Exec "bash $RemoteDeploy status"
    exit 0
}

# 2) 重启
if ($Restart) {
    Remote-Exec "bash $RemoteDeploy restart"
    exit 0
}

# 3) 至少需要一个文件
if (-not $Apk -and -not $Questions) {
    Write-Host "学伴守护一键上传工具" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "用法:"
    Write-Host "  上传APK:    .\upload.ps1 -Apk app-debug.apk -VersionCode 3 -VersionName 1.2.0 -ReleaseNotes '优化体验'"
    Write-Host "  上传题库:   .\upload.ps1 -Questions questions.json"
    Write-Host "  两者都传:   .\upload.ps1 -Apk app-debug.apk -Questions questions.json -VersionCode 3"
    Write-Host "  查状态:     .\upload.ps1 -Status"
    Write-Host "  重启服务:   .\upload.ps1 -Restart"
    exit 0
}

# 4) 上传文件
$remoteApk = ""
$remoteQuestions = ""

if ($Apk) {
    Upload-File $Apk "studyguard-new.apk"
    $remoteApk = "$RemoteTmp/studyguard-new.apk"
}

if ($Questions) {
    Upload-File $Questions "questions-new.json"
    $remoteQuestions = "$RemoteTmp/questions-new.json"
}

# 5) 远程执行部署
Write-Host "🚀 远程部署中..."
$deployCmd = "bash $RemoteDeploy"

if ($Apk -and $Questions) {
    $deployCmd += " both $remoteApk $remoteQuestions"
    if ($VersionCode) { $deployCmd += " $VersionCode '$VersionName' '$ReleaseNotes'" }
} elseif ($Apk) {
    $deployCmd += " apk $remoteApk"
    if ($VersionCode) { $deployCmd += " $VersionCode '$VersionName' '$ReleaseNotes'" }
} elseif ($Questions) {
    $deployCmd += " questions $remoteQuestions"
}

Remote-Exec $deployCmd

Write-Host ""
Write-Host "✅ 部署完成！" -ForegroundColor Green
