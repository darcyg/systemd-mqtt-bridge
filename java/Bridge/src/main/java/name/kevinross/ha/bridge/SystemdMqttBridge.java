package name.kevinross.ha.bridge;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.systemd1.Pair;

/**
 * Created by Kevin Ross on 2016-05-23.
 */
public class SystemdMqttBridge {
    private static String SYSD_IFACE = "org.freedesktop.systemd1";
    private static String SYSD_PATH = "/org/freedesktop/systemd1";
    public static class BridgeClass implements SystemdListenerEventHandler, MqttListenerEventHandler {
        private SystemdListener systemdListener = null;
        private MqttListener mqttListener = null;
        public BridgeClass(String broker, String base_path, String on_word, String off_word) {
            systemdListener = new SystemdListener(this);
            mqttListener = new MqttListener(broker, base_path, on_word, off_word, this);
        }
        @Override
        public Pair<String, String> handle_start(String service) {
            try {
                return systemdListener.start_unit(service);
            } catch (DBusException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Pair<String, String> handle_stop(String service) {
            try {
                return systemdListener.stop_unit(service);
            } catch (DBusException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void handle_refresh() {
            systemdListener.publish_all();
        }

        @Override
        public void publish_state(String service, Pair<String, String> state) {
            mqttListener.publish_state(service, state);
        }
    }
    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("must pass broker hostname and topic prefix (in that order)");
            System.exit(1);
        } else if (args.length == 1) {
            System.out.println("must pass topic prefix after broker hostname");
            System.exit(1);
        }
        BridgeClass bridge = new BridgeClass(args[0], args[1], "ON", "OFF");
        bridge.handle_refresh();
    }
}
