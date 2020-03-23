package org.linkda.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.*;
import org.hyperledger.fabric.gateway.*;
import org.linkda.chaincode.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class DataComparisonApplication {
    private static final Logger logger = LoggerFactory.getLogger(DataComparisonApplication.class.getName());

    private static final int BATCH_SIZE = 16;
    private static boolean hasTransient = true;

    public static void main(String[] args) throws InterruptedException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(null, "wallet-directory", true, "身份钱包目录");
        options.addOption(null, "user-label", true, "身份钱包中用户标签");
        options.addOption(null, "connection-profile", true, "fabric网络配置文件");
        options.addOption(null, "channel-name", true, "应用通道名字（网络管理员配置文件指定）");
        options.addOption(null, "chaincode-name", true, "链码名字（安装链码时指定）");
        options.addOption(null, "contract-package", true, "合约命名空间（编写链码时@Contract指定）");
        options.addOption(null, "data-directory", true, "上链数据目录/文件");
        options.addOption(null, "crowd", true, "人群包ID");
        options.addOption("b", "batch-size", true, "一次交易上传的数据条数，增加网络吞吐能力");
        options.addOption("t", "transient", true, "指定该参数表示不传transientData");
        options.addOption("h", "help", false, "Usage");

        String walletDir = null;
        String userLabel = null;
        String connectionProfileDir = null;
        String channelName = null;
        String chaincodeName = null;
        String contractPackage = null;
        String dataDir = null;
        String crowd = null;
        int batchSize = BATCH_SIZE;
        Boolean dataStatus = Boolean.FALSE;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                return ;
            }

            if (!(line.hasOption("wallet-directory") && line.hasOption("user-label")
                && line.hasOption("connection-profile") && line.hasOption("channel-name")
                && line.hasOption("chaincode-name") && line.hasOption("contract-package")
                && line.hasOption("data-directory"))) {

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                throw new IllegalArgumentException("those arguments must pass " +
                        "[wallet-directory/user-label/connection-profile/channel-name/chaincode-name/contract-package/data-directory]");
            }

            for (Option option : line.getOptions()) {
                switch (option.getLongOpt()) {
                    case "wallet-directory":
                        walletDir = option.getValue(); break;
                    case "user-label":
                        userLabel = option.getValue(); break;
                    case "connection-profile":
                        connectionProfileDir = option.getValue();break;
                    case "channel-name":
                        channelName = option.getValue();break;
                    case "chaincode-name":
                        chaincodeName = option.getValue();break;
                    case "contract-package":
                        contractPackage = option.getValue();break;
                    case "data-directory":
                        dataDir = option.getValue();break;
                    case "batch-size":
                        batchSize = Integer.parseInt(option.getValue());break;
                    case "crowd":
                        crowd = option.getValue();break;
                    case "transient":
                        hasTransient = false;break;
                    default:
                        logger.warn("当前不支持此参数 %s", option.toString());
                }
            }
        } catch(ParseException exp ) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            exp.printStackTrace();
            return ;
        }


        try {
            Gateway.Builder builder = Gateway.createBuilder();
            Path walletPath = Paths.get(walletDir);

            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            Path connectionProfile = Paths.get(connectionProfileDir);
            builder.identity(wallet, userLabel).networkConfig(connectionProfile);

            Gateway gateway = builder.connect();
            Network network = gateway.getNetwork(channelName);

            Contract contract = network.getContract(chaincodeName, contractPackage);
            try{
                // 1.获取科大讯飞需要对应人群包对应的用户设备
                Map<String, byte[]> transientData = new HashMap<String, byte[]>();
                transientData.put("crowdID",crowd.getBytes());
                String hwDataComparison = queryCrowdData(transientData, contract, "HWDataComparison");

                //判断是否查到数据
                if("数据未找到".equalsIgnoreCase(hwDataComparison)){
                    dataStatus = Boolean.TRUE;
                }

                // 处理返回数据，封装成transientData
                String[] dataDetail = string2Array(hwDataComparison);
                if (!dataStatus && null != dataDetail && dataDetail.length > 0){
                    Arrays.asList(dataDetail).forEach(
                        data -> {
                           transientData.put(data.trim(),"".getBytes());
                        }
                    );
                    // 调用更新欢网数据方法，并传输参数
                    int hwDataComparisonDetail = updateCrowdData(transientData, contract, "HWDataComparisonDetail");
                    if (hwDataComparisonDetail != State.OK.getCode()){
                        logger.error("匹配失败！！！");
                    }else{
                        logger.info("匹配完成！！！");
                    }
                }else{
                    logger.info("未匹配到对应的人群包数据！！！");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            gateway.close();
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    /**
     * @description //TODO 查询讯飞数据集人群包对应的数据
     * @author malei
     * @date 2020/3/19
     * @param transientData
     * @param contract
     * @param func
     * @return java.lang.String
     */
    private static String queryCrowdData(Map<String, byte[]> transientData, Contract contract, String func) throws InterruptedException, TimeoutException, ContractException {
        Transaction transaction = contract.createTransaction(func);
        transaction.setTransient(transientData);
        byte[] submit = transaction.submit();
        String data = new String(submit);
        return data;
    }

    /**
     * @description //TODO 匹配及更新欢网数据
     * @author malei
     * @date 2020/3/19
     * @param transientData
     * @param contract
     * @param func
     * @return int
     */
    private static int updateCrowdData(Map<String, byte[]> transientData, Contract contract, String func) throws InterruptedException, TimeoutException, ContractException {
        Transaction transaction = contract.createTransaction(func);
        transaction.setTransient(transientData);
        byte[] submit = transaction.submit();
        Integer data = Integer.parseInt(new String(submit));
        return data;
    }


    public static String[] string2Array(String content) {
        if (content == null || content.length() == 0) {
            return null;
        }
        return content.replace("[","").replace("]","").split(",");
    }


}
