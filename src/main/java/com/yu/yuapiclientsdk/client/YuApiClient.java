package com.yu.yuapiclientsdk.client;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.yu.yuapiclientsdk.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.yu.yuapiclientsdk.utils.SignUtils.signature;


@Slf4j
public class YuApiClient {

    private String accessKey;

    private String secretKey;

    public YuApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getNameByGet(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.get("http://localhost:8201/api/user/get", paramMap);
        return result;
    }

    public String getNameByPost(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.post("http://localhost:8201/api/user/getName", paramMap);
        return result;
    }

    /**
     * 获取请求头
     *
     * @return
     */
    private Map<String, String> getHeaderMap(String encodedBodyJson) {
        Map<String, String> headerMap = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");

        // 放入需要参与签名的参数
        headerMap.put("accessKey", accessKey);
        headerMap.put("timestamp", timestamp);
        headerMap.put("nonceStr", nonceStr);
        String rawBody = new String(Base64.decode(encodedBodyJson), StandardCharsets.UTF_8); // 解码后的原始body

        // 生成签名（secretKey会在signature方法内部添加到签名参数中）
        String sign = signature(timestamp, accessKey, secretKey, nonceStr, rawBody, headerMap);

        headerMap.put("body", encodedBodyJson);
        headerMap.put("signature", sign);

        return headerMap;
    }

    public String getUserNameByPost(User user) {
        String jsonStr = JSONUtil.toJsonStr(user);
        String encodedBody = Base64.encode(jsonStr); // 进行编码防止中文乱码
        HttpResponse response = HttpRequest.post("http://localhost:8201/api/user/getUserName")
                .addHeaders(getHeaderMap(encodedBody))
                .body(jsonStr)
                .execute();
        return response.body();
    }

}
