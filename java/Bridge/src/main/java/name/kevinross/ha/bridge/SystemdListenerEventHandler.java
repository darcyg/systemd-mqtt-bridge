package name.kevinross.ha.bridge;

import org.freedesktop.systemd1.Pair;

/**
 * Created by Kevin Ross on 2016-05-24.
 */
public interface SystemdListenerEventHandler {
    void publish_state(String service, Pair<String, String> state);
}
