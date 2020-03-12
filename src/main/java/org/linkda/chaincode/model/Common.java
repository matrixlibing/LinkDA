package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.util.Set;

public class Common {
    /**
     * 手机MAC
     */
    @Nonnull
    public String mac;

    /**
     * 所在家庭的大屏MAC
     */
    public Set<String> ottmac = Sets.newHashSet();

    /**
     * 手机IMEI
     */
    public Set<String> imei = Sets.newHashSet();

    /**
     * 设备人群ID列表
     */
    public Set<String> gid = Sets.newHashSet();

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
