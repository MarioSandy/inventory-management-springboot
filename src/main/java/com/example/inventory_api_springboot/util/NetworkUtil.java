package com.example.inventory_api_springboot.util;

import jakarta.servlet.http.HttpServletRequest;

public class NetworkUtil {
    // Note to self: static doesn't create new instance, so it is Heap Free
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim(); // Extract first original IP if multiple proxies exist
        }
        return ip;
    }
}
