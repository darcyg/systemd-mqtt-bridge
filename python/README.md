# Compilation

pip will do it all so just `pip install -r requirements.txt' and `setup.py install` to install the deps and script.

# Usage

`python systemd_mqtt.py $hostname_of_broker $prefix_to_handle`

Hardcoded to connect to default MQTT port (1883).

Prefix is the parent of this hosts systemd units so should probably incorporate the hostname somewhere; something like `/systemd/$hostname` works.
