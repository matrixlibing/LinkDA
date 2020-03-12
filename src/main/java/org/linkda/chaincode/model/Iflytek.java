package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.List;

public class Iflytek {
    /**
     * 手机MAC
     */
    @Nonnull
    public String mac;

    /**
     * 手机IMEI
     */
    public List<String> imei  = Lists.newArrayList();

    /**
     * 设备人群ID列表
     */
    public List<String> gid = Lists.newArrayList();

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
