package com.hotel.a2a.protocol;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 Request wrapper for A2A protocol
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonRpcRequest {
    private String jsonrpc;
    private Object id;
    private String method;
    private Map<String, Object> params;
}
