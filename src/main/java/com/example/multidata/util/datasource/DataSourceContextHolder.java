package com.example.multidata.util.datasource;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DataSourceContextHolder {

    private static final ThreadLocal<String> context = new ThreadLocal<>();

    public static void setRoutingKey(String tenantId) {
        clear();
        context.set(tenantId);
    }

    public static String getRoutingKey() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}
