package com.hotel.a2a.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * JSON-RPC 2.0 Response wrapper for A2A protocol
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonRpcResponse {
    private String jsonrpc;
    private Object id;
    private Object result;
    private JsonRpcError error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;
    }

    public static JsonRpcResponse success(Object id, Object result) {
        return JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .id(id)
                .result(result)
                .build();
    }

    public static JsonRpcResponse error(Object id, int code, String message) {
        return JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .id(id)
                .error(JsonRpcError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    public static JsonRpcResponse error(Object id, int code, String message, Object data) {
        return JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .id(id)
                .error(JsonRpcError.builder()
                        .code(code)
                        .message(message)
                        .data(data)
                        .build())
                .build();
    }
}
