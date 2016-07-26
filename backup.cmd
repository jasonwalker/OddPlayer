adb shell su -c 'mkdir /mnt/sdcard/backup'
adb shell su -c 'cp -r /data/data/com.jmw.rd.podplay/ /mnt/sdcard/backup/dbs'
adb shell su -c 'cp -r /mnt/sdcard/Android/data/com.jmw.rd.podplay/files/ /mnt/sdcard/backup/files'