package cn.crduer.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带过期时间的map  每5分钟 一次回收  线程安全
 *
 * @Author: cruder
 * @Date: 2021/12/24/16:51
 */
public class ExpiryMap<K, V> implements Map<K, V> {

    /**
     * 存放k，v
     */
    private ConcurrentHashMap workMap;
    /**
     * value为到期时间
     */
    private ConcurrentHashMap expiryMap;

    /**
     * 默认保存时间1分钟
     */
    private long expiry_time;

    /**
     * 默认保存时间 60 秒
     */
    private static final long DEFAULT_EXPIRY_TIME = 1000 * 60;


    /**
     * circulation 循环时间  默认5分钟
     */
    private static final long CIRCULATION_TIME = 1000 * 60 * 5;

    /**
     * delay 启动延迟时间  默认1分钟
     */
    private static final long DELAY = 1000 * 60;


    public ExpiryMap() {
        /**
         * 默认过期时间 60秒
         */
        this(16, 60);
    }

    /**
     * 单位秒
     *
     * @param initialCapacity 容量
     * @param expiryTime      过期时间
     */
    public ExpiryMap(int initialCapacity, long expiryTime) {
        this.expiry_time = DEFAULT_EXPIRY_TIME;
        if (expiryTime > 0) {
            this.expiry_time = expiryTime * 1000;
        }
        workMap = new ConcurrentHashMap(initialCapacity);
        expiryMap = new ConcurrentHashMap(initialCapacity);
        // 利用定时任务 循环removeExpiryKeys()
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                removeExpiryKeys();
            }
        }, DELAY, CIRCULATION_TIME);
    }

    /**
     * 使用 默认过期时间
     *
     * @param key   key
     * @param value value
     * @return V
     */
    @Override
    public V put(K key, V value) {
        expiryMap.put(key, System.currentTimeMillis() + expiry_time);
        return (V) workMap.put(key, value);
    }

    /**
     * 使用 自定义过期时间 单位秒
     *
     * @param key    key
     * @param value  value
     * @param exrity exrity
     * @return
     */
    public V put(K key, V value, long exrity) {
        if (exrity <= 0) {
            exrity = this.expiry_time;
        }
        // 设置过期时间
        expiryMap.put(key, System.currentTimeMillis() + exrity * 1000);
        return (V) workMap.put(key, value);
    }

    /**
     * 删除已到期的 键值对
     */
    private void removeExpiryKeys() {
        expiryMap.keySet().forEach(key -> {
            checkExpiry((K) key, true);
        });
    }

    /**
     * 是否过期判断函数
     *
     * @param key      key
     * @param isDelete 是否删除
     * @return 过期true  不过期false
     */
    private boolean checkExpiry(K key, boolean isDelete) {
        Object timeObject = expiryMap.get(key);
        if (timeObject == null) {
            return true;
        }
        long setTime = (long) timeObject;
        boolean isExpiry = System.currentTimeMillis() - setTime >= 0;
        if (isExpiry) {
            if (isDelete) {
                expiryMap.remove(key);
                workMap.remove(key);
            }
            return true;
        }
        return false;
    }

    @Override
    public V get(Object key) {
        boolean isExpiry = checkExpiry((K) key, true);
        if (isExpiry) {
            return null;
        }
        return (V) workMap.get(key);
    }

    @Override
    public int size() {
        removeExpiryKeys();
        return workMap.size();
    }

    @Override
    public boolean isEmpty() {
        removeExpiryKeys();
        return workMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        removeExpiryKeys();
        return workMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        removeExpiryKeys();
        return workMap.containsValue(value);
    }

    @Override
    public V remove(Object key) {
        expiryMap.remove(key);
        return (V) workMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.entrySet().forEach(en -> {
            expiryMap.put(en.getKey(), System.currentTimeMillis() + expiry_time);
            workMap.put(en.getKey(), en.getValue());
        });
    }

    @Override
    public void clear() {
        expiryMap.clear();
        workMap.clear();
    }

    @Override
    public Set<K> keySet() {
        removeExpiryKeys();
        return workMap.keySet();
    }

    @Override
    public Collection<V> values() {
        removeExpiryKeys();
        return workMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        removeExpiryKeys();
        return workMap.entrySet();
    }

}


