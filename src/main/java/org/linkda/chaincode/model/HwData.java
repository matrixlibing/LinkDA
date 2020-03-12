package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName HwDataTek
 * @Description: TODO 欢网数据
 * @Author malei
 * @Date 2020/3/2
 * @Version V1.0
 **/
public class HwData {

    /**
     * 手机 mac
     */
    @Nonnull
    public String mac;

    /**
     * OTT mac
     */
    public Set<String> ottMac  = new HashSet<String>();
    
    /** 
    * @Author malei
    * @Description //TODO  对应的人群包ID列表
    * @Date 13:30 2020/3/2
    * @Param 
    * @return 
    **/
    public Set<String> crowds = new HashSet<String>();

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
