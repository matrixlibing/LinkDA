package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.util.Set;

public class Iflytek {
    /**
     * 手机MAC
     */
    @Nonnull
    public String mac;

    /**
     * 手机IMEI
     */
    public Set<String> imei  = Sets.newHashSet();

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
