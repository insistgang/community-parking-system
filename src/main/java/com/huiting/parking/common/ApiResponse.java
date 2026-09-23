package com.huiting.parking.common;

public class ApiResponse {
    public int code = 200;
    public String message = "success";
    public Object data;

    public static ApiResponse ok(Object data) {
        ApiResponse r = new ApiResponse();
        r.data = data;
        return r;
    }

    public static ApiResponse ok(String message, Object data) {
        ApiResponse r = new ApiResponse();
        r.message = message;
        r.data = data;
        return r;
    }

    public static ApiResponse msg(String message) {
        ApiResponse r = new ApiResponse();
        r.message = message;
        return r;
    }
}
