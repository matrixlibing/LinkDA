package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class TestIflytek {

    @Test
    public void test() {
        Iflytek iflytek = new Iflytek();
        iflytek.mac = "92:6c:94:40:5a:e2";
        iflytek.imei = Lists.newArrayList("351384443059895");
        iflytek.gid = Lists.newArrayList("1", "2");

        System.out.println(iflytek.tojson());
    }
}
