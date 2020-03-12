package org.linkda.chaincode.model;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

public class TestIflytek {

    @Test
    public void test() {
        Iflytek iflytek = new Iflytek();
        iflytek.mac = "92:6c:94:40:5a:e2";
        iflytek.imei = Sets.newHashSet("351384443059895");
        iflytek.gid = Sets.newHashSet("1", "2");

        System.out.println(iflytek.tojson());

        System.out.println(Iflytek.deserialize(iflytek.serialize()).tojson());
    }
}
