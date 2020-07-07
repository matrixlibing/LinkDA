package org.linkda.chaincode;

import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.linkda.chaincode.model.HwData;
import org.linkda.chaincode.model.Iflytek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Contract(
        name = "org.linkda.chaincode",
        info = @Info(
                title = "数据映射合约", description = "合约用于多方数据打通", version = "0.0.1-snapshot",
                license = @License(name = "", url = ""),
                contact = @Contact(email = "", name = "mapping", url = "")))
@Default
public class MappingContract implements ContractInterface {
    private final static Logger logger = LoggerFactory.getLogger(MappingContract.class.getName());

    /**
     * 必要的资源初始化
     *
     * @param {Context} ctx the transaction context
     */
    @Transaction
    public void instantiate(Context ctx) {
        logger.info("没有资源需要初始化");
    }

    /**
     * 科大讯飞数据上链合约
     *
     * @param {Context} ctx the transaction context
     */
    @Transaction
    public int XFUpload(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"XFMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，非科大讯飞成员（XFMSP）。", cid.getMSPID());
                return State.Unauthorized.getCode();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getCode();
        }

        //数据上传
        Map<String, byte[]> transientData = stub.getTransient();
        for (Map.Entry<String, byte[]> entry : transientData.entrySet()) {

            String mac = entry.getKey();
            byte[] data = entry.getValue();
            Iflytek iflytek = Iflytek.deserialize(data);
            //查看collection是否存在人群ID,有则增量更新数据
//            byte[] stateData = stub.getState(mac);
//            if (stateData != null && stateData.length != 0) {
//                Iflytek old = Iflytek.deserialize(stateData);
//                iflytek.gid.addAll(old.gid);
//            }
            stub.putStringState(mac, iflytek.tojson());
        }
        return State.OK.getCode();
    }

    /**
     * @description //TODO 欢网数据上链合约
     * @author malei
     * @date 2020/6/24
     * @param ctx
     * @return int
     */
    @Transaction
    public int HWUpload(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，非欢网成员（HWMSP）。", cid.getMSPID());
                return State.Unauthorized.getCode();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getCode();
        }

        //数据上传
        Map<String, byte[]> transientData = stub.getTransient();
        for (Map.Entry<String, byte[]> entry : transientData.entrySet()) {
            String mac = entry.getKey();
            byte[] data = entry.getValue();
            HwData hwData = HwData.deserialize(data);
            //查看collection是否存在人群ID,有则增量更新数据
//            byte[] stateData = stub.getPrivateData("collectionHuan", mac);
//            HwData old = HwData.deserialize(stateData);
//            if (stateData != null && stateData.length != 0) {
//                HwData old = HwData.deserialize(stateData);
//                hwData.gid.addAll(old.gid);
//                hwData.ottMac.addAll(old.ottMac);
//            }
            stub.putPrivateData("collectionHuan", mac, hwData.tojson());
        }

        // 返回更新结果
        return State.OK.getCode();
    }

    /**
     * @description //TODO 更新欢网数据
     * @author malei
     * @date 2020/6/24
     * @param ctx
     * @return java.lang.String
     */
    @Transaction
    public String HWUpdate(Context ctx) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，非欢网成员（HWMSP）。", cid.getMSPID());
//                System.out.println("客户端MSPID为 "+cid.getMSPID()+"，非科大讯飞成员（XFMSP）。");
                return State.Unauthorized.getDescription();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getDescription();
        }

        //当次查询总数据量
        int all_ct = 0;
        //跟欢网数据匹配到的数量
        int match_count = 0;

        long start_time = System.currentTimeMillis();
        //根据讯飞数据进行更新
        Map<String, byte[]> transientData = stub.getTransient();
        for (Map.Entry<String, byte[]> entry : transientData.entrySet()) {
            String mac = entry.getKey();
            byte[] data = entry.getValue();
            Iflytek iflytek = Iflytek.deserialize(data);

            all_ct += 1;
            //查看collection是否存在人群ID,有则增量更新数据
            byte[] stateData = stub.getPrivateData("collectionHuan", mac);
            if (stateData != null && stateData.length != 0) {
                HwData hwData = HwData.deserialize(stateData);
                hwData.gid.addAll(iflytek.gid);
                stub.putPrivateData("collectionHuan", mac, hwData.tojson());

                match_count += 1;
            }
        }
        long end_time = System.currentTimeMillis();
        System.out.println("本次处理耗时"+ (end_time - start_time) +"毫秒!处理更新数据"+ match_count +"条");

        JSONObject result = new JSONObject();
        result.put("all_ct",all_ct);
        result.put("match_count",match_count);
        // 返回更新结果
        return result.toJSONString();
    }


    /**
     * @description //TODO 范围查询（根据时间区间进行公共数据检索，然后更新欢网私有数据）
     * @author malei
     * @date 2020/6/22
     * @param ctx
     * @return java.lang.String
     */
    @Transaction
    public String HwUpdateByRange(Context ctx,String query){
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，非欢网成员（HWMSP）。", cid.getMSPID());
                return State.Unauthorized.getDescription();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getDescription();
        }

        long start_time = System.currentTimeMillis();

        // 通过富查询查询公共数据
        Iterator<KeyValue> iterator = stub.getQueryResult(query).iterator();

        //当次查询总数据量
        int all_ct = 0;
        //跟欢网数据匹配到的数量
        int match_count = 0;

        while (iterator.hasNext()) {
            KeyValue next = iterator.next();
            String mac = next.getKey();
            Iflytek iflytek = Iflytek.deserialize(next.getValue());

            all_ct += 1;

            byte[] stateData = stub.getPrivateData("collectionHuan", mac);
            if (stateData != null && stateData.length != 0) {

                HwData hwData = HwData.deserialize(stateData);
                hwData.gid.addAll(iflytek.gid);
                stub.putPrivateData("collectionHuan", mac, hwData.tojson());

                match_count += 1;
            }
        }

        long end_time = System.currentTimeMillis();
        System.out.println("本次处理耗时"+ (end_time - start_time) +"毫秒!处理更新数据"+ match_count +"条;总记录数为"+all_ct+"条！");

        JSONObject result = new JSONObject();
        result.put("all_ct",all_ct);
        result.put("match_count",match_count);
        // 返回更新结果
        return result.toJSONString();
    }


}
