package org.linkda.chaincode;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.linkda.chaincode.model.Common;
import org.linkda.chaincode.model.Iflytek;
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

            //查看private data是否已经有该数据:没有直接放入，有就更新
            byte[] stateData = stub.getPrivateData("collectionVirtual", mac);
            if (stateData == null || stateData.length == 0) {
                Common common = new Common();
                common.mac = iflytek.mac;
                common.imei = iflytek.imei;
                stub.putPrivateData("collectionVirtual", mac, iflytek.tojson());
            } else {
                // TODO：更新IMEI，数据冲突暂未考虑
                Common common = Common.deserialize(stateData);
                common.imei = iflytek.imei;
                //将stateData放回private data
                stub.putPrivateData("collectionVirtual", mac, common.tojson());
            }
        }

        return State.OK.getCode();
    }
}
