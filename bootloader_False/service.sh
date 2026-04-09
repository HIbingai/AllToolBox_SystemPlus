#!/system/bin/sh
# 此脚本在 service 阶段执行

MODDIR=${0%/*}
LOG_FILE="$MODDIR/SystemPlus.log"

# 启动我们注入的监控服务
if [ -f "${MODDIR}/system/etc/init/block_bootloader.rc" ]; then
    echo "$(date): Starting bootloader_blocker service." >> $LOG_FILE
    start bootloader_blocker
fi