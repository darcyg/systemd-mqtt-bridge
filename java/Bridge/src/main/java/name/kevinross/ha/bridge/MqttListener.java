package name.kevinross.ha.bridge;

import org.apache.commons.io.FilenameUtils;
import org.freedesktop.systemd1.Pair;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

/**
 * Created by Kevin Ross on 2016-05-27.
 */
public class MqttListener implements Listener {
    private String base_path, on_word, off_word;
    private MQTT mqtt;
    private CallbackConnection client;
    private MqttListenerEventHandler handler;
    public MqttListener(String broker, String base_path, String on_word, String off_word, MqttListenerEventHandler handler) {
        this.base_path = base_path;
        this.on_word = on_word;
        this.off_word = off_word;
        this.handler = handler;
        try {
            mqtt = new MQTT();
            mqtt.setHost(broker, 1883);
            client = mqtt.callbackConnection();
            client.listener(this);
            client.connect(new Callback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Topic[] topics = {new Topic(base_path + "/update/#", QoS.AT_MOST_ONCE),
                                      new Topic("/refresh", QoS.AT_MOST_ONCE)
                    };
                    client.subscribe(topics, new throwawayHandler<>());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    throwable.printStackTrace();
                    throw new RuntimeException("couldn't connect to broker");
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("URI is bad");
        }
    }

    public void publish_state(String service, Pair<String, String> state) {
        publish_message(base_path + "/state/" + service, state.b.contentEquals("running") ? on_word : off_word);
        publish_message(base_path + "/state/" + service + "/raw", (state.a + "," + state.b));
    }

    private void publish_message(String topic, String payload) {
        System.out.println(topic);
        System.out.println(payload);
        synchronized (this) {
            try {
                client.publish(topic, payload.getBytes("UTF-8"), QoS.AT_MOST_ONCE, false, new throwawayHandler<Void>());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException("bad encoding");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finalize() throws Throwable {
        client.disconnect(new throwawayHandler<Void>());
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onPublish(UTF8Buffer topic_, Buffer payload, Runnable runnable) {
        String topic = topic_.toString();
        String service = FilenameUtils.getName(topic);
        String state = null;
        try {
            state = new String(payload.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (topic.contentEquals("/refresh")) {
            handler.handle_refresh();
        } else if (state.contentEquals(on_word) || state.contentEquals("1") || state.toLowerCase().contentEquals("true")) {
            publish_state(service, handler.handle_start(service));
        } else if (state.contentEquals(off_word) || state.contentEquals("0") || state.toLowerCase().contentEquals("false")) {
            publish_state(service, handler.handle_stop(service));
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        throwable.printStackTrace();
        throw new RuntimeException("listener failed for w/e reason");
    }

    private static class throwawayHandler<T> implements Callback<T> {

        @Override
        public void onSuccess(T t) {

        }

        @Override
        public void onFailure(Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
