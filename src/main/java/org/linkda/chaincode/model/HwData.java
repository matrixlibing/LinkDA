package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class HwData {

    public Set<String> ottMac  = new HashSet<String>();
    
    public Set<String> gid = new HashSet<String>();

    public long uptime;

    public static HwData deserialize(byte[] data) {
        return JSON.parseObject(new String(data), HwData.class);
    }

    public byte[] serialize() {
        return JSON.toJSONString(this).getBytes();
    }

    public String tojson() {
        return JSON.toJSONString(this);
    }


}
