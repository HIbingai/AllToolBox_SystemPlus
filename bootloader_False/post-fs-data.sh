#!/system/bin/sh
# 此脚本在 post-fs-data 阶段执行

MODDIR=${0%/*}
LOG_FILE="$MODDIR/bootloader_block.log"

# 拦截重启到 Bootloader 的指令
# 由于此时系统属性可能还未设置，我们用循环来检查
check_interval=0.15
while true; do
    current_powerctl=$(getprop sys.powerctl)
    if [ -n "$current_powerctl" ] && echo "$current_powerctl" | grep -q "bootloader"; then
        echo "$(date): [post-fs-data] Bootloader reboot blocked. Value: $current_powerctl" >> $LOG_FILE
        resetprop --delete sys.powerctl
        resetprop --delete sys.powerctl
    fi
    sleep $check_interval
done