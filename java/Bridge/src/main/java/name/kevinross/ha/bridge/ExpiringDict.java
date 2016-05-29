package name.kevinross.ha.bridge;

import org.freedesktop.systemd1.Pair;
import org.joda.time.DateTime;

import java.util.*;

/**
 * Created by Kevin Ross on 2016-05-24.
 */
public class ExpiringDict<K, V> implements Map {
    private int max_age;
    private Map<K, Pair<V, Integer>> storage = new HashMap<>();
    private void expire_entries() {
        synchronized (storage) {
            long now = DateTime.now().getMillis() / 1000;
            for (Entry<K, Pair<V, Integer>> e : storage.entrySet()) {
                if (now - e.getValue().b > this.max_age) {
                    storage.remove(e.getKey());
                }
            }
        }
    }
    public ExpiringDict(int max_age) {
        this.max_age = max_age;
    }

    @Override
    public int size() {
        expire_entries();
        synchronized (storage) {
            return storage.size();
        }
    }

    @Override
    public boolean isEmpty() {
        expire_entries();
        synchronized (storage) {
            return storage.isEmpty();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        expire_entries();
        synchronized (storage) {
            return storage.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        expire_entries();
        synchronized (storage) {
            return this.values().contains(value);
        }
    }

    @Override
    public Object get(Object key) {
        expire_entries();
        synchronized (storage) {
            return storage.get(key).a;
        }
    }

    @Override
    public Object put(Object key, Object value) {
        expire_entries();
        synchronized (storage) {
            return storage.put((K) key, new Pair<>((V) value, (int) (DateTime.now().getMillis() / 1000)));
        }
    }

    @Override
    public Object remove(Object key) {
        synchronized (storage) {
            return storage.remove(key);
        }
    }

    @Override
    public void putAll(Map m) {
        expire_entries();
        synchronized (storage) {
            for (Object e : m.entrySet()) {
                Entry<K, V> es = (Entry<K, V>) e;
                this.put(es.getKey(), es.getValue());
            }
        }
    }

    @Override
    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    @Override
    public Set keySet() {
        synchronized (storage) {
            return storage.keySet();
        }
    }

    @Override
    public Collection values() {
        expire_entries();
        synchronized (storage) {
            List<V> vals = new ArrayList<>(storage.size());
            for (Entry<K, Pair<V, Integer>> e : storage.entrySet()) {
                vals.add(e.getValue().a);
            }
            return vals;
        }
    }

    @Override
    public Set<Entry> entrySet() {
        synchronized (storage) {
            Set<Entry> eset = new HashSet<>();
            for (Entry<K, Pair<V, Integer>> e : storage.entrySet()) {
                eset.add(new AbstractMap.SimpleImmutableEntry<K, V>(e.getKey(), e.getValue().a));
            }
            return eset;
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        expire_entries();
        synchronized (storage) {
            for (Entry<K, Pair<V, Integer>> e : storage.entrySet()) {
                if (e.getKey() == key && e.getValue().a == value) {
                    return storage.remove(key, e.getValue());
                }
            }
            return false;
        }
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        if (!containsValue(oldValue)) {
            return false;
        }
        synchronized (storage) {
            return this.replace(key, newValue) != null;
        }
    }

    @Override
    public Object replace(Object key, Object value) {
        synchronized (storage) {
            return storage.put((K) key, new Pair<V, Integer>((V) value, (int) (DateTime.now().getMillis() / 1000)));
        }
    }
}
