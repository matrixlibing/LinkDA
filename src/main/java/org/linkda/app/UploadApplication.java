package org.linkda.app;

import org.apache.commons.cli.*;
import org.hyperledger.fabric.gateway.*;
import org.linkda.chaincode.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class UploadApplication {
    private static final Logger logger = LoggerFactory.getLogger(UploadApplication.class.getName());

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
        options.addOption(null, "transaction-name", true, "交易名字");
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
        String func = null;
        int batchSize = BATCH_SIZE;

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
                && line.hasOption("data-directory") && line.hasOption("transaction-name"))) {

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                throw new IllegalArgumentException("those arguments must pass " +
                        "[wallet-directory/user-label/connection-profile/channel-name/chaincode-name/contract-package/data-directory/transaction-name]");
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
                    case "transaction-name":
                        func = option.getValue();break;
                    case "transient":
                        hasTransient = false;break;
                    default:
                        logger.warn("当前不支持此参数 %s", option.toString());
                }
            }
        } catch(ParseException exp ) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            return ;
        }

        List<File> files = listAllDataFile(dataDir);

        try {
            Gateway.Builder builder = Gateway.createBuilder();
            Path walletPath = Paths.get(walletDir);

            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            Path connectionProfile = Paths.get(connectionProfileDir);
            builder.identity(wallet, userLabel).networkConfig(connectionProfile);

            Gateway gateway = builder.connect();
            Network network = gateway.getNetwork(channelName);

            Contract contract = network.getContract(chaincodeName, contractPackage);

            Map<String, byte[]> transientData = new HashMap<String, byte[]>();
            for (File file : files) {
                int lineNumbCur = 0;
                int lineNumbPrev = 0;
                int currentSize = 0;
                String fileName = file.getName();

                try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        lineNumbCur ++;

                        try {
                            String[] pair = line.split(" ");
                            if (pair.length == 2) {
                                transientData.put(pair[0], pair[1].getBytes());
                                currentSize ++;
                            } else {
                                logger.error("data format error at file: {}, line: {}", fileName, lineNumbCur);
                            }

                            //达到最大BATCH，进行一次交易
                            if (currentSize != 0 && currentSize == batchSize) {
                                submitTransaction(transientData, contract, func);
                                logger.info("transaction success  from file: {} line: ({}, {}]", fileName, lineNumbPrev, lineNumbCur);
                                transientData.clear();
                                currentSize = 0;
                                lineNumbPrev = lineNumbCur;
                            }
                        } catch (Exception e) {
                            logger.error("data format error at file: {}, line: {}, at {}", fileName, lineNumbCur);
                            logger.error("", e);
                        }
                    }

                    try {
                        if (currentSize != 0 ) {
                            submitTransaction(transientData, contract, func);
                            logger.info("transaction success  from file: {} line: ({}, {}]", fileName, lineNumbPrev, lineNumbCur);
                            transientData.clear();
                        }
                    } catch (Exception e) {
                        logger.error("", e);
                    }

                } catch (IOException e) {
                    logger.error("", e);
                }
            }

            gateway.close();
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    //GZUpload, queryMac(mac: String)
    private static void submitTransaction(Map<String, byte[]> transientData, Contract contract, String func) throws InterruptedException, TimeoutException, ContractException {
        if (!hasTransient) {
            submitTransaction0(transientData, contract, func);
            return ;
        }

        Transaction transaction = contract.createTransaction(func);
        transaction.setTransient(transientData);
        Integer rslt = bytes2Int(transaction.submit());
        if (rslt != State.OK.getCode()) {
            throw new ContractException("transaction failed, return is " + rslt);
        }
    }

    private static void submitTransaction0(Map<String, byte[]> transientData, Contract contract, String func) throws InterruptedException, TimeoutException, ContractException {
        Transaction transaction = contract.createTransaction(func);

        String[] args = new String[transientData.size() * 2];
        int index = 0;
        for (Map.Entry<String, byte[]> entry : transientData.entrySet()) {
            args[2*index] = entry.getKey();
            args[2*index + 1] = new String(entry.getValue());
            index++;
        }
        Integer rslt = bytes2Int(transaction.submit(args));
        if (rslt != State.OK.getCode()) {
            throw new ContractException("transaction failed, return is " + rslt);
        }
    }

    public static int bytes2Int(byte[] bytes) {
        return Integer.parseInt(new String(bytes));
    }

    private static List<File> listAllDataFile(String dataDir) {
        File dir = new File(dataDir);
        List<File> files = new ArrayList<File>();
        if (dir.isFile()) {
            files.add(dir);
        } else {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    files.add(file);
                } else {
                    logger.error("do not support nesting directory.");
                }
            }
        }
        return files;
    }
}
