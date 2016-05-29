#!/usr/bin/env python

from distutils.core import setup

setup(
	name = 'systemd-mqtt',
	version = '1.0',
	description = 'bridge systemd and mqtt',
	author = 'Kevin Ross',
	author_email = 'contact@kevinross.name',
	url = 'https://github.com/kevinross/systemd-mqtt-bridge',
	packages = ['systemd_mqtt'],
	scripts = ['systemd_mqtt/systemd_mqtt.py'],
	install_requires = ['paho-mqtt', 'pydbus', 'expiringdict']
)
