# Usage

`python systemd_mqtt.py $hostname_of_broker $prefix_to_handle`

Hardcoded to connect to default MQTT port (1883).

Prefix is the parent of this hosts systemd units so should probably incorporate the hostname somewhere; something like `/systemd/$hostname` works.
