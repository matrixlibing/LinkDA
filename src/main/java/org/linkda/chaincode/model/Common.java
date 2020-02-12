package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.List;

public class Common {
    /**
     * 手机MAC
     */
    @Nonnull
    public String mac;

    /**
     * 所在家庭的大屏MAC
     */
    public List<String> ottmac = Lists.newArrayList();

    /**
     * 手机IMEI
     */
    public List<String> imei = Lists.newArrayList();

    public static Common deserialize(byte[] data) {
        return JSON.parseObject(new String(data), Common.class);
    }

    public byte[] serialize() {
        return JSON.toJSONString(this).getBytes();
    }

    public String tojson() {
        return JSON.toJSONString(this);
    }
}
