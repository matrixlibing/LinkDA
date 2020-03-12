package org.linkda.chaincode.utils;

import org.linkda.chaincode.model.HwData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName FileUtils
 * @Description: TODO 主要用来读取本地对应的文件，获取文件结果
 * @Author malei
 * @Date 2020/3/2
 * @Version V1.0
 **/
public class FileUtils {

    /**
     * @return java.util.Map<java.lang.String,org.linkda.chaincode.model.HwDataTek>
     * @Author malei
     * @Description //TODO 读取文件 并返回对应的欢网数据集合
     * @Date 13:43 2020/3/2
     * @Param [filePath]
     **/
    public static Map<String, HwData> readDataFromFile() {
        //初始化Map集合，用来加载读取为文件流数据
        Map<String, HwData> dataTekMap = new HashMap<>();
        InputStream is = FileUtils.class.getClassLoader().getResourceAsStream("file.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String hwData = "";
        try {
            while(true) {
                try {
                    if (!((hwData=br.readLine())!=null))
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String[] hwDataSplit = hwData.split(" ");
                String mac = hwDataSplit[0];
                String data = hwDataSplit[1];
                HwData hwDataTek = HwData.deserialize(data.getBytes());
                hwDataTek.crowds.add("testFiled");
                dataTekMap.put(mac, hwDataTek);
            }
        } catch (Exception e){
//            e.printStackTrace();
            return dataTekMap;
        }
        return dataTekMap;
    }




}