package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;

import java.util.Set;

public class Iflytek {
    /**
     * 设备人群ID列表
     */
    public Set<String> gid = Sets.newHashSet();

    public static Iflytek deserialize(byte[] data) {
        return JSON.parseObject(new String(data), Iflytek.class);
    }

    public byte[] serialize() {
        return JSON.toJSONString(this).getBytes();
    }

    public String tojson() {
        return JSON.toJSONString(this);
    }
}
