package com.yu.yuapiclientsdk.client;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.yu.yuapiclientsdk.entity.User;
import com.yu.yuapiclientsdk.exception.ApiException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.yu.yuapiclientsdk.utils.SignUtils.signature;

@Slf4j
public class YuApiClient {

    private final String accessKey;
    private final String secretKey;

    // 网关地址（可以配置化）
    private String gatewayUrl = "http://localhost:8201";

    public YuApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public YuApiClient(String accessKey, String secretKey, String gatewayUrl) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        if (StrUtil.isNotBlank(gatewayUrl)) {
            this.gatewayUrl = gatewayUrl;
        }
    }

    // 保留原有方法以保持向后兼容
    public String getNameByGet(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.get(gatewayUrl + "/api/user/get", paramMap);
        return result;
    }

    public String getNameByPost(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.post(gatewayUrl + "/api/user/getName", paramMap);
        return result;
    }

    /**
     * 获取请求头
     *
     * @param body 请求体内容（JSON字符串）
     * @return 包含签名的请求头
     */
    private Map<String, String> getHeaderMap(String body) {
        Map<String, String> headerMap = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");

        // 对body进行Base64编码防止中文乱码
        String encodedBody = StrUtil.isNotBlank(body) ? Base64.encode(body) : "";

        // 放入需要参与签名的参数
        headerMap.put("accessKey", accessKey);
        headerMap.put("timestamp", timestamp);
        headerMap.put("nonceStr", nonceStr);

        // 解码后的原始body用于签名
        String rawBody = StrUtil.isNotBlank(body) ? body : "";

        // 生成签名
        String sign = signature(timestamp, accessKey, secretKey, nonceStr, rawBody, headerMap);

        headerMap.put("body", encodedBody);
        headerMap.put("signature", sign);

        // 添加通用请求头
        headerMap.put("Content-Type", "application/json;charset=UTF-8");

        return headerMap;
    }

    public String getUserNameByPost(User user) {
        String jsonStr = JSONUtil.toJsonStr(user);
        HttpResponse response = HttpRequest.post(gatewayUrl + "/api/user/getUserName")
                .addHeaders(getHeaderMap(jsonStr))
                .body(jsonStr)
                .execute();

        if (!response.isOk()) {
            throw new ApiException("API调用失败，状态码: " + response.getStatus());
        }

        return response.body();
    }

    /**
     * 通用接口调用方法（推荐使用）
     *
     * @param method 请求方法：GET, POST, PUT, DELETE
     * @param path   接口路径（不包含网关地址）
     * @param params 请求参数（JSON字符串）
     * @return 接口响应结果
     */
    public String invokeInterface(String method, String path, String params) {
        return invokeInterface(method, path, params, new HashMap<>());
    }

    /**
     * 通用接口调用方法（支持自定义请求头）
     *
     * @param method        请求方法
     * @param path          接口路径
     * @param params        请求参数（JSON字符串）
     * @param customHeaders 自定义请求头
     * @return 接口响应结果
     */
    public String invokeInterface(String method, String path, String params, Map<String, String> customHeaders) {
        // 构建完整URL
        String fullUrl = buildFullUrl(path);

        // 构建请求头
        Map<String, String> headers = getHeaderMap(params);
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }

        try {
            log.debug("调用接口 - 方法: {}, URL: {}, 参数: {}", method, fullUrl, params);

            HttpResponse response = null;
            switch (method.toUpperCase()) {
                case "GET":
                    // GET请求，参数放在URL中
                    response = doGet(fullUrl, params, headers);
                    break;
                case "POST":
                    response = doPost(fullUrl, params, headers);
                    break;
                case "PUT":
                    response = doPut(fullUrl, params, headers);
                    break;
                case "DELETE":
                    response = doDelete(fullUrl, params, headers);
                    break;
                default:
                    throw new ApiException("不支持的请求方法: " + method);
            }

            // 检查响应状态
            if (!response.isOk()) {
                throw new ApiException(String.format(
                        "API接口地址: %s 调用失败，状态码: %d, 响应: %s",
                        gatewayUrl + path,
                        response.getStatus(),
                        response.body()
                ));
            }

            String result = response.body();
            log.debug("接口调用成功，响应: {}", result);
            return result;

        } catch (Exception e) {
            log.error("接口调用异常", e);
            if (e instanceof ApiException) {
                throw e;
            }
            throw new ApiException("接口调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行GET请求
     */
    private HttpResponse doGet(String url, String params, Map<String, String> headers) {
        HttpRequest request = HttpRequest.get(url)
                .addHeaders(headers);

        // 如果有参数，解析为查询参数
        if (StrUtil.isNotBlank(params)) {
            Map<String, Object> paramMap = parseParamsToMap(params);
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                request.form(entry.getKey(), entry.getValue());
            }
        }

        return request.execute();
    }

    /**
     * 执行POST请求
     */
    private HttpResponse doPost(String url, String params, Map<String, String> headers) {
        return HttpRequest.post(url)
                .addHeaders(headers)
                .body(params)
                .execute();
    }

    /**
     * 执行PUT请求
     */
    private HttpResponse doPut(String url, String params, Map<String, String> headers) {
        return HttpRequest.put(url)
                .addHeaders(headers)
                .body(params)
                .execute();
    }

    /**
     * 执行DELETE请求
     */
    private HttpResponse doDelete(String url, String params, Map<String, String> headers) {
        HttpRequest request = HttpRequest.delete(url)
                .addHeaders(headers);

        // DELETE请求可能带参数
        if (StrUtil.isNotBlank(params)) {
            request.body(params);
        }

        return request.execute();
    }

    /**
     * 构建完整URL
     */
    private String buildFullUrl(String path) {
        if (StrUtil.isBlank(path)) {
            throw new ApiException("接口路径不能为空");
        }

        // 如果已经是完整URL，直接返回
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        // 确保path以斜杠开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return gatewayUrl + path;
    }

    /**
     * 将JSON参数解析为Map（用于GET请求的查询参数）
     */
    private Map<String, Object> parseParamsToMap(String params) {
        try {
            return JSONUtil.toBean(params, Map.class);
        } catch (Exception e) {
            log.warn("参数不是有效的JSON，将作为字符串处理", e);
            Map<String, Object> map = new HashMap<>();
            map.put("params", params);
            return map;
        }
    }

    /**
     * 简化调用 - 使用对象作为参数
     */
    public String invokeInterface(String method, String path, Object params) {
        String jsonParams = params != null ? JSONUtil.toJsonStr(params) : "";
        return invokeInterface(method, path, jsonParams);
    }

    /**
     * 简化调用 - 调用InterfaceInfo对象
     */
    public String invokeInterface(InterfaceInfo interfaceInfo, String params) {
        if (interfaceInfo == null) {
            throw new ApiException("接口信息不能为空");
        }

        return invokeInterface(
                interfaceInfo.getMethod(),
                interfaceInfo.getPath(),
                params,
                parseHeaders(interfaceInfo.getRequestHeaders())
        );
    }

    /**
     * 解析请求头配置
     */
    private Map<String, String> parseHeaders(String headersConfig) {
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(headersConfig)) {
            try {
                Map<String, String> configMap = JSONUtil.toBean(headersConfig, Map.class);
                headers.putAll(configMap);
            } catch (Exception e) {
                log.warn("解析请求头配置失败", e);
            }
        }
        return headers;
    }

    /**
     * 接口信息辅助类（可以在Controller中定义相同的类）
     */
    @Setter
    @Getter
    public static class InterfaceInfo {
        // getters and setters
        private String method;
        private String path;
        private String requestHeaders;

        // 构造函数
        public InterfaceInfo() {
        }

        public InterfaceInfo(String method, String path) {
            this.method = method;
            this.path = path;
        }

    }
}