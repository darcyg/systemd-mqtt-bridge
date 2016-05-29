package name.kevinross.ha.bridge;
import org.apache.commons.io.FilenameUtils;
import org.freedesktop.DBus;
import org.freedesktop.dbus.*;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.systemd1.Manager;
import org.freedesktop.systemd1.Pair;
import org.freedesktop.systemd1.Struct4;
import org.freedesktop.systemd1.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Kevin Ross on 2016-05-24.
 */
public class SystemdListener {
    private static String SYSD_IFACE = "org.freedesktop.systemd1";
    private static String SYSD_PATH = "/org/freedesktop/systemd1";
    private static List<Pair<String, String>> replacements = new ArrayList<>();
    private Map<String, Pair<String, String>> unit_states = new ExpiringDict<>(100);
    private DBusConnection conn;
    private Manager systemd;
    private UnitRemovedHandler rm_unit_handler;
    private Boolean has_handler = false;
    private SystemdListenerEventHandler handler = null;

    /**
     * Connect and sub to prop change events automatically
     * @throws DBusException
     */
    public SystemdListener(SystemdListenerEventHandler handler) {
        try {
            conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
            systemd = conn.getRemoteObject(SYSD_IFACE, SYSD_PATH, Manager.class);
            rm_unit_handler = new UnitRemovedHandler();
            this.handler = handler;
            conn.addSigHandler(DBus.Properties.PropertiesChanged.class, new DBusSigHandler<DBus.Properties.PropertiesChanged>() {
                @Override
                public void handle(DBus.Properties.PropertiesChanged s) {
                    try {
                        SystemdListener.this.handle_prop_changed(s.getPath(), s.changed_keys);
                    } catch (DBusException e) {
                        e.printStackTrace();
                    }
                }
            });
            this.subscribe_rm_events();
        } catch (DBusException e) {
            e.printStackTrace();
            throw new DBusExecutionException("couldn't do the dbus thing");
        }
    }

    public void publish_all() {
        List<Struct4> units = systemd.ListUnits();
        for (Struct4 s : units) {
            String unit = s.a, active = s.d, sub = s.e;
            handler.publish_state(sanitize(unit), new Pair<>(active, sub));
        }
    }

    /**
     * Start a unit (such as "testing.service")
     * @param unit the unit to start
     * @return a pair of ("ActiveState", "SubState") obtained after 1 second
     * @throws DBusException
     */
    public Pair<String, String> start_unit(String unit) throws DBusException {
        synchronized (this) {
            try {
                systemd.StartUnit(unit, "fail");
            } catch (DBusExecutionException e) {
                // "job" can't ever be instantiated so library fails
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return unit_state(unit);
        }
    }

    /**
     * Stop a unit (such as "testing.service")
     * @param unit the unit to start
     * @return a pair of ("ActiveState", "SubState") obtained after 1 second
     * @throws DBusException
     */
    public Pair<String, String> stop_unit(String unit) throws DBusException {
        synchronized (this) {
            try {
                systemd.StopUnit(unit, "fail");
            } catch (DBusExecutionException e) {
                // "job" can't ever be instantiated so library fails
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return unit_state(unit);
        }
    }

    /**
     * Get state for a given unit
     * @param unit something like "testing.service"
     * @return ("ActiveState", "SubState") pair
     * @throws DBusException
     */
    public Pair<String, String> unit_state(String unit) throws DBusException {
        DBus.Properties props =  conn.getRemoteObject(SYSD_IFACE, SYSD_PATH + "/unit/" + bastardize(unit), DBus.Properties.class);
        return new Pair<>(props.Get(SYSD_IFACE + ".Unit", "ActiveState"), props.Get(SYSD_IFACE + ".Unit", "SubState"));
    }

    /**
     * Subscribe to unit removed events
     * @throws DBusException
     */
    private void subscribe_rm_events() throws DBusException {
        flush_queue_and_exec(new Runnable() {
            @Override
            public void run() {
                synchronized (has_handler) {
                    if (has_handler)
                        return;
                    try {
                        conn.addSigHandler(Manager.UnitRemoved.class, rm_unit_handler);
                        has_handler = true;
                    } catch (DBusException e) {

                    }
                }
            }
        });
    }

    /**
     * Unsubscribe from unit removed events
     * @throws DBusException
     */
    private void unsubscribe_rm_events() throws DBusException {
        flush_queue_and_exec(new Runnable() {
            @Override
            public void run() {
                synchronized (has_handler) {
                    if (!has_handler)
                        return;
                    try {
                        conn.removeSigHandler(Manager.UnitRemoved.class, rm_unit_handler);
                        has_handler = false;
                    } catch (DBusException e) {

                    }
                }
            }
        });
    }

    /**
     * Handle property change events for a unit
     * @param path
     */
    private void handle_prop_changed(String path, Map<String, Variant> changed_keys) throws DBusException {
        if (!path.contains("/unit/"))
            return;
        handle_event(sanitize(path));
    }

    /**
     * Handle unit stop events
     * @param unit
     */
    private void handle_unit_stop(String unit) throws DBusException {
        unsubscribe_rm_events();
        handle_event(unit);
        subscribe_rm_events();
    }

    /**
     * dispatch events
     * @param service something like "testing.service"
     * @throws DBusException
     */
    private void handle_event(String service) throws DBusException {
        synchronized (this) {
            Pair<String, String> state = unit_state(service);
            if (!unit_states.containsKey(service) || (!unit_states.get(service).a.equals(state.a) && !unit_states.get(service).b.equals(state.b)))
                handler.publish_state(service, state);
            unit_states.put(service, state);
        }
    }


    private class UnitRemovedHandler implements DBusSigHandler<Manager.UnitRemoved> {
        @Override
        public void handle(Manager.UnitRemoved s) {
            try {
                SystemdListener.this.handle_unit_stop(s.a);
            } catch (DBusException e) {

            }
        }
    }

    @Override
    public void finalize() throws Throwable {
        unsubscribe_rm_events();
        systemd.Unsubscribe();
        conn.disconnect();
        super.finalize();
    }

    /**
     * Make unit names sane ("/org/freedesktop/systemd1/testing_2eservice" -> "testing.service") for dbus
     * @param path something like /org/freedesktop/systemd1/testing_2eservice
     * @return something like "testing.service"
     */
    private static String sanitize(String path) {
        String base = FilenameUtils.getName(path);
        for (Pair<String, String> e : replacements) {
            base = base.replace(e.a, e.b);
        }
        return base;
    }

    /**
     * Almost the inverse of {@link #sanitize(String)}: undo's the character replacements
     * however doesn't make a path out of it
     * @param unit something like "testing.service"
     * @return something like "testing_2eservice"
     */
    private static String bastardize(String unit) {
        String out = unit;
        for (Pair<String, String> e : replacements) {
            out = out.replace(e.b, e.a);
        }
        return out;
    }

    /**
     * java-dbus chokes when adding/removing signal handlers, especially with multiple in-flight
     * messages. Run a given runnable with no in-flight messages
     * @param r
     */
    private void flush_queue_and_exec(Runnable r) {
        synchronized (this) {
            // force all threads to exit gracefully (implemented as a "run" flag in loop)
            conn.changeThreadCount((byte)0);
            // wait for exit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // add a single thread to run the runnable
            conn.changeThreadCount((byte)1);
            r.run();
            // reset back to the normal number of threads
            conn.changeThreadCount((byte)4);
        }
    }
    static {
        replacements.add(new Pair<>("_2e", "."));
        replacements.add(new Pair<>("_2d", "-"));
        replacements.add(new Pair<>("_40l", "@"));
        replacements.add(new Pair<>("\\x2d", "-"));
    }

}
