package org.linkda.chaincode;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.linkda.chaincode.model.HwData;
import org.linkda.chaincode.model.Iflytek;
import org.linkda.chaincode.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
            logger.debug("transient data：{}", iflytek.tojson());

            //查看collection是否存在人群ID,有则增量更新数据
            byte[] stateData = stub.getPrivateData("collectionVirtual", mac);
            if (stateData != null) {
                Iflytek old = Iflytek.deserialize(stateData);
                iflytek.gid.addAll(old.gid);
                // TODO: 过期 gid 处理
            }
            stub.putPrivateData("collectionVirtual", mac, iflytek.tojson());

           /* //查看private data是否已经有该数据:没有直接放入，有就更新
            byte[] stateData = stub.getPrivateData("collectionVirtual", mac);
            if (stateData == null || stateData.length == 0) {
                Common common = new Common();
                common.mac = iflytek.mac;
                common.imei = iflytek.imei;
                common.gid = iflytek.gid;
                stub.putPrivateData("collectionVirtual", mac, iflytek.tojson());
            } else {
                // TODO：更新IMEI，数据冲突暂未考虑
                Common common = Common.deserialize(stateData);
                common.imei = iflytek.imei;
                //将stateData放回private data
                stub.putPrivateData("collectionVirtual", mac, common.tojson());
            }*/
        }

        return State.OK.getCode();
    }



    /**
     * @Author malei
     * @Description // 欢网数据上链合约
     * @Date 11:41 2020/3/2
     * @Param [ctx]
     * @return int
     **/
    @Transaction
    public int HWUpload(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"MGMMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，非欢网成员（HWMSP）。", cid.getMSPID());
                return State.Unauthorized.getCode();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getCode();
        }

        // 数据上传读取本地数据（欢网原始数据）
        // 遍历及格式化数据信息
        Map<String, HwData> hwDataTekMap = FileUtils.readDataFromFile();
        // 数据上链及更新
        try {
            hwDataTekMap.forEach(
                (mac, hwDataTek) -> {//遍历上传的数据
                    //查看private data是否已经有该数据:没有直接放入，有就更新
                    byte[] stateData = stub.getPrivateData("collectionHuan", mac);
                    if (stateData == null || stateData.length == 0) { //如果不存在，则插入对应的数据信息
                        stub.putPrivateData("collectionHuan", mac, hwDataTek.tojson());
                    } else {
                        // TODO：如果有，更新对应的 ottmac
                        HwData hstateData = HwData.deserialize(stateData);
                        // 更新对应的OTTmac信息集合，把没有出现过的追加进去
                        hstateData.ottMac.addAll(hwDataTek.ottMac);
                        //将hstateData放回private data
                        stub.putPrivateData("collectionHuan", mac, hstateData.tojson());
                    }
                }
            );
        }catch (Exception e){
            logger.error("欢网数据初始化失败，", e);
            return State.ServerError.getCode();
        }
        // 返回更新结果
        return State.OK.getCode();
    }



    /**
     * @Author malei
     * @Description //TODO  数据对比标记
     * @Date 11:51 2020/3/2
     * @Param [ctx, crowdId]
     * @return int
     **/
    @Transaction
    public int HWDataComparison(Context ctx,String crowdId) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID()) || !"XFMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，无权限调用 {} 方法。", cid.getMSPID(),"HWDataComparison");
                return State.Unauthorized.getCode();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return State.ServerError.getCode();
        }

        String query = "{\"selector\":{\"gid\":{\"$elemMatch\":{\"$eq\":\""+crowdId+"\"}}}}";
        //读取讯飞对应的数据集数据
        try {
            QueryResultsIterator<KeyValue> collectionHuan = stub.getPrivateDataQueryResult("collectionVirtual", query);
            HWDataComparisonDetail(ctx, collectionHuan,crowdId);
        }catch (ChaincodeException e){
            logger.error("匹配更新失败");
            return State.ServerError.getCode();
        }
        return State.OK.getCode();
    }

    /**
     * @Author malei
     * @Description //TODO  处理数据
     * @Date 11:51 2020/3/2
     * @Param [ctx, crowdId]
     * @return int
     **/
    @Transaction
    public int HWDataComparisonDetail(Context ctx,QueryResultsIterator<KeyValue> collectionHuan,String crowdId) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID()) || !"XFMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，无权限调用 {} 方法。", cid.getMSPID(),"HWDataComparisonDetail");
                return State.Unauthorized.getCode();
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return 500;
        }
       if (collectionHuan.iterator().hasNext()){
            collectionHuan.iterator().forEachRemaining(l -> {
                // 根据规则查询欢网的对应数据，如果能匹配上，则在欢网对应的数据中添加对应的人群ID信息，未匹配则不进行此操作。
                String mac = l.getKey();
                // 查询欢网是否有此设备数据相关的信息
                byte[] stateData = stub.getPrivateData("collectionHuan", mac);
                if (stateData != null || stateData.length > 0) { //如果存在，则更新对应的数据信息
                    // TODO：如果有，更新对应的人群包信息
                    HwData hstateData = HwData.deserialize(stateData);
                    hstateData.crowds.add(crowdId);
                    //将stateData放回private data
                    stub.putPrivateData("collectionHuan", mac, hstateData.tojson());
                }
            });
        }else{
            logger.error("没有匹配到对应人群{}的设备信息!" , crowdId);
            throw new ChaincodeException("未查询到对应人群 {} 的相关数据!" , crowdId);
        }
        return State.OK.getCode();
    }


    /**
     * @Author malei
     * @Description //TODO  查询验证数据
     * @Date 11:51 2020/3/2
     * @Param [ctx, crowdId]
     * @return int
     **/
    @Transaction
    public String HWDataSearch(Context ctx,String key) {
        ChaincodeStub stub = ctx.getStub();
        //身份校验
        try {
            ClientIdentity cid = new ClientIdentity(stub);
            if (!"HWMSP".equals(cid.getMSPID())) {
                logger.error("客户端MSPID为 {}，无权限调用 {} 方法。", cid.getMSPID(),"HWDataSearch");
                return "401";
            }
        } catch (Exception e) {
            logger.error("客户端身份创建错误，", e);
            return "500";
        }

        byte[] stateData = stub.getPrivateData("collectionHuan", key);
        if (stateData == null || stateData.length == 0) {
            MappingContract.logger.error("KEY $ HWDataSearch :", key);
            throw new ChaincodeException("KEY $ HWDataSearch :" + key);
        }
        HwData hwData = HwData.deserialize(stateData);
        return hwData.tojson();
    }


}
