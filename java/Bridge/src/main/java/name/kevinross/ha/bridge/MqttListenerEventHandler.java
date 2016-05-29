package name.kevinross.ha.bridge;

import org.freedesktop.systemd1.Pair;

/**
 * Created by Kevin Ross on 2016-05-27.
 */
public interface MqttListenerEventHandler {
    Pair<String, String> handle_start(String service);
    Pair<String, String> handle_stop(String service);
    void handle_refresh();
}
