package com.rockwill.deploy.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用于记录每个appId的访问时间戳
 */
public class AccessRecord {
    private Queue<Long> timestamps = new ConcurrentLinkedQueue<>();

    public Queue<Long> getTimestamps() {
        return timestamps;
    }

    /**
     * 添加一个新的访问记录，并清理过期记录（例如24小时前的记录）
     */
    public void addRecord() {
        long now = System.currentTimeMillis();
        timestamps.offer(now); // 添加新记录
        cleanupExpired(now); // 触发清理
    }

    /**
     * 清理过期记录（例如，保留最近24小时内的记录）
     */
    private void cleanupExpired(long currentTime) {
        long expireTime = currentTime - (24 * 60 * 60 * 1000);
        while (!timestamps.isEmpty() && timestamps.peek() < expireTime) {
            timestamps.poll();
        }
    }

    /**
     * 获取指定时间窗口内的访问次数
     * @param timeWindowMillis 时间窗口大小（毫秒），例如1小时：3600000
     * @return 该时间窗口内的访问次数
     */
    public int getCountInWindow(long timeWindowMillis) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - timeWindowMillis;
        cleanupExpired(currentTime);

        int count = 0;
        for (Long timestamp : timestamps) {
            if (timestamp >= windowStart) {
                count++;
            }
        }
        return count;
    }
}