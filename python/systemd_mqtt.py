import time, paho.mqtt.client as mqtt, os
from pydbus.bus import Bus
from gi.repository import GObject, Gio
from expiringdict import ExpiringDict
from functools import partial

if 'HOSTNAME' not in os.environ:
	os.environ['HOSTNAME'] = open('/etc/hostname').read().strip()

class SystemdListener(object):
	replacements = [('_2e', '.'), ('_2d', '-'), ('_40l', '@'), ('\\x2d', '-')]

	def __init__(self):
		self.unit_states = ExpiringDict(max_len=300, max_age_seconds=100)
		self.bus = Bus(Gio.BusType.SYSTEM, timeout=5000)
		self.systemd = self.bus.get('.systemd1')
		self.systemd.Subscribe()
		self.sub_id = None
		self.prop_sub_id = self.bus.con.signal_subscribe(None, "org.freedesktop.DBus.Properties", "PropertiesChanged", None, None, 0, self.__handle_unit_change)
		self.__subscribe_rm_events()

	def run(self):
		GObject.MainLoop().run()

	def publish_all(self):
		units = self.systemd.ListUnits()[0]
		for u in units:
			state = self.unit_state(self.get_unit(SystemdListener.sanitize(u[0])))
			self.publish_state(SystemdListener.sanitize(u[0]), state)

	@staticmethod
	def __try_until_good(func, *args):
		try:
			return func(*args)
		except:
			import traceback
			traceback.print_exc()
			time.sleep(0.5)
			return SystemdListener.__try_until_good(func, *args)

	@staticmethod
	def sanitize(path):
		p = os.path.basename(path)
		for f, t in SystemdListener.replacements:
			p = p.replace(f, t)
		return p

	def get_unit(self, unit):
		unit_iface = SystemdListener.__try_until_good(self.systemd.LoadUnit, unit)
		return SystemdListener.__try_until_good(self.bus.get, '.systemd1', unit_iface[0])

	def unit_state(self, unit):
		props = SystemdListener.__try_until_good(unit.GetAll, 'org.freedesktop.systemd1.Unit')[0]
		return (props['ActiveState'], props['SubState'])

	def start_unit(self, unit):
		SystemdListener.__try_until_good(self.systemd.StartUnit, unit, 'fail')
		time.sleep(1)
		return self.unit_state(self.get_unit(unit))

	def stop_unit(self, unit):
		SystemdListener.__try_until_good(self.systemd.StopUnit, unit, 'fail')
		time.sleep(1)
		return self.unit_state(self.get_unit(unit))

	def publish_state(self, service, state, substate=None):
		raise NotImplementedError('must implement')

	def __handle_unit_change(self, conn, sender, path, iface, name, params):
		service = SystemdListener.sanitize(path)
		if '/unit/' not in path:
			return
		if 'ActiveState' not in params[1].keys():
			return
		self.__handle_event(service)
		
	def __handle_unit_stop(self, conn, sender, path, iface, name, params):
		# "getting" the unit creates the unit in memory and publishes a duplicate "stopped" event
		self.__unsubscribe_rm_events()
		service, obj = params
		if name == 'UnitRemoved':
			self.__handle_event(service)
		self.__subscribe_rm_events()

	def __handle_event(self, service):
		state = self.unit_state(self.get_unit(service))
		if service not in self.unit_states or (service in self.unit_states and state != self.unit_states[service]):
			self.publish_state(service, state)
		self.unit_states[service] = state

	def __subscribe_rm_events(self):
		if not self.sub_id:
			self.sub_id = self.bus.con.signal_subscribe(None, 'org.freedesktop.systemd1.Manager', 'UnitRemoved', None, None, 0, self.__handle_unit_stop)

	def __unsubscribe_rm_events(self):
		self.bus.con.signal_unsubscribe(self.sub_id)
		self.sub_id = None


class MqttSystemdListener(SystemdListener):
	def __init__(self, mqtt_client, base_path, on_word, off_word):
		self.mqtt_client = mqtt_client
		self.base_path = base_path
		self.on_word = on_word
		self.off_word = off_word
		SystemdListener.__init__(self)

	def publish_state(self, service, state, substate=None):
		base, sub = state
		hab_state = self.on_word if sub == 'running' else self.off_word
		# publish a generic on/off state for openhab et al
		self.mqtt_client.publish_message('/'.join([self.base_path, 'state', service]), hab_state)
		# publish the raw state
		self.mqtt_client.publish_message('/'.join([self.base_path, 'state', service, 'raw']), ':'.join(state))

class MqttListener(object):
	def __init__(self, broker, base_path, on_word, off_word):
		self.base_path = base_path
		self.on_word = on_word
		self.off_word = off_word
		self.client = mqtt.Client()
		self.client.on_connect = self.__handle_connect
		self.client.on_message = self.__handle_message
		self.client.connect(broker)
		self.client.loop_start()
	def handle_start(self, service):
		raise NotImplementedError('must implement')
	def handle_stop(self, service):
		raise NotImplementedError('must implement')
	def handle_refresh(self):
		raise NotImplementedError('must implement')
	def publish_state(self, service, state):
		raise NotImplementedError('must implement')
	def publish_message(self, message, data):
		self.client.publish(message, data)
	def __handle_connect(self, client, userdata, flags, rc):
		self.client.subscribe('/'.join([self.base_path, 'update', '#']))
		self.client.subscribe('/refresh')
		self.handle_refresh()
	def __handle_message(self, client, userdata, msg):
		if msg.topic == '/refresh':
			self.handle_refresh()
			return
		service = os.path.basename(msg.topic)
		if msg.payload in ('ON', 'true', '1'):
			result = self.handle_start(service)
		elif msg.payload in ('OFF', 'false', '0'):
			result = self.handle_stop(service)
		else:
			result = ''
		self.publish_state(service, result)

class SystemdMqttBridge(MqttSystemdListener, MqttListener):
	def __init__(self, broker, base_path="/systemd/%s" % os.environ['HOSTNAME'], on_word='ON', off_word='OFF'):
		MqttSystemdListener.__init__(self, self, base_path, on_word, off_word)
		MqttListener.__init__(self, broker, base_path, on_word, off_word)
	def handle_start(self, service):
		return self.start_unit(service)
	def handle_stop(self, service):
		return self.stop_unit(service)
	def handle_refresh(self):
		self.publish_all()

bridge = SystemdMqttBridge("pellet.cave.kevinross.name")
bridge.handle_refresh()
bridge.run()

