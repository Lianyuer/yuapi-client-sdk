package com.yu.yuapiclientsdk.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;

/**
 * 签名工具类
 */
@Slf4j
public class SignUtils {

    public static String signature(String timestamp, String accessKey,
                                   String secretKey, String nonceStr, String body, Map<String, String> resMap) {
        if (StrUtil.hasBlank(accessKey, secretKey, timestamp, nonceStr)) {
            throw new RuntimeException("签名错误");
        }
        // 参数名按照ASCII码表升序排序
        resMap.put("secretKey", secretKey);
        resMap.put("body", body);
        String[] keys = resMap.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        // 按照排序拼接参数和参数值
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : keys) {
            if (resMap.get(key).length() != 0 && !resMap.get(key).equals("0")) {
                stringBuilder.append("&&").append(key).append("=").append(resMap.get(key));
            }
        }
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        String digestHex = md5.digestHex(stringBuilder.toString());
        log.info("accessKey:{}>>>>>用户应加密串顺序为：{}", accessKey, stringBuilder.toString());
        log.info("签名: {}", digestHex);
        return digestHex;
    }
}
