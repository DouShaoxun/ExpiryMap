package cn.crduer.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带过期时间的map  每5分钟 一次回收  线程安全
 *
 * @Author: cruder
 * @Date: 2021/12/24/16:51
 */
public class ExpiryMap<K, V> extends ConcurrentHashMap<K, V> {

    /**
     * 默认过期时间(毫秒)
     * <br/>
     * 即put的时候不指定过期时间,则采用此值作为过期时间
     */
    private long expiry;

    private Map<K, Long> expiryMap = new ConcurrentHashMap<>();


    /**
     * @param expiryTime 单位毫秒（不可小于0）
     */
    public ExpiryMap(long expiryTime) {

        this(1 << 4, expiryTime);
    }

    /**
     * @param initialCapacity 初始容量
     * @param expiryTime      单位毫秒
     */
    public ExpiryMap(int initialCapacity, long expiryTime) {
        super(initialCapacity);
        if (expiryTime <= 0) {
            throw new RuntimeException();
        }
        this.expiry = expiryTime;
    }

    @Override
    public V put(K key, V value) {
        expiryMap.put(key, System.currentTimeMillis() + this.expiry);
        return super.put(key, value);
    }

    @Override
    public boolean containsKey(Object key) {
        return !checkExpiry(key, true) && super.containsKey(key);
    }

    /**
     * @param key        key
     * @param value      value
     * @param expiryTime 键值对有效期 毫秒
     * @return value
     */
    public V put(K key, V value, long expiryTime) {
        expiryMap.put(key, System.currentTimeMillis() + expiryTime);
        return super.put(key, value);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return Boolean.FALSE;
        }
        Set<Entry<K, V>> set = super.entrySet();
        if (set == null) {
            return Boolean.FALSE;
        }
        Iterator<Entry<K, V>> iterator = set.iterator();
        while (iterator.hasNext()) {
            Entry<K, V> entry = iterator.next();
            if (value.equals(entry.getValue())) {
                if (checkExpiry(entry.getKey(), false)) {
                    iterator.remove();
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Collection<V> values() {

        Collection<V> values = super.values();

        if (values.size() < 1) {
            return values;
        }

        values.removeIf(next -> !containsValue(next));
        return values;
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }
        if (checkExpiry(key, true)) {
            return null;
        }
        return super.get(key);
    }


    public Object isInvalid(Object key) {
        if (key == null) {
            return null;
        }
        if (!expiryMap.containsKey(key)) {
            return null;
        }
        long expiryTime = expiryMap.get(key);

        boolean flag = System.currentTimeMillis() > expiryTime;

        if (flag) {
            super.remove(key);
            expiryMap.remove(key);
            return -1;
        }
        return super.get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            expiryMap.put(e.getKey(), System.currentTimeMillis() + this.expiry);
        }
        super.putAll(m);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> set = super.entrySet();
        if (set != null) {
            set.removeIf(entry -> checkExpiry(entry.getKey(), false));
        }
        return set;
    }


    private boolean checkExpiry(Object key, boolean isRemoveSuper) {

        if (!expiryMap.containsKey(key)) {
            return Boolean.FALSE;
        }
        long expiryTime = expiryMap.get(key);

        boolean flag = System.currentTimeMillis() > expiryTime;

        if (flag) {
            if (isRemoveSuper) {
                super.remove(key);
            }
            expiryMap.remove(key);
        }
        return flag;
    }
}