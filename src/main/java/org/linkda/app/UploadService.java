package org.linkda.app;

import com.alibaba.fastjson.JSON;
import org.apache.commons.cli.*;
import org.hyperledger.fabric.gateway.*;
import org.linkda.chaincode.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class UploadService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class.getName());
    private static Contract contract = null;

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(null, "wallet-directory", true, "身份钱包目录");
        options.addOption(null, "user-label", true, "身份钱包中用户标签");
        options.addOption(null, "connection-profile", true, "fabric网络配置文件");
        options.addOption(null, "channel-name", true, "应用通道名字（网络管理员配置文件指定）");
        options.addOption(null, "chaincode-name", true, "链码名字（安装链码时指定）");
        options.addOption(null, "contract-package", true, "合约命名空间（编写链码时@Contract指定）");
        options.addOption(null, "transaction-name", true, "交易名字");
        options.addOption("h", "help", false, "Usage");

        String walletDir = null;
        String userLabel = null;
        String connectionProfileDir = null;
        String channelName = null;
        String chaincodeName = null;
        String contractPackage = null;
        String func = null;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                return;
            }

            if (!(line.hasOption("wallet-directory") && line.hasOption("user-label")
                    && line.hasOption("connection-profile") && line.hasOption("channel-name")
                    && line.hasOption("chaincode-name") && line.hasOption("contract-package")
                    && line.hasOption("transaction-name"))) {

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                throw new IllegalArgumentException("those arguments must pass " +
                        "[wallet-directory/user-label/connection-profile/channel-name/chaincode-name/contract-package/transaction-name]");
            }

            for (Option option : line.getOptions()) {
                switch (option.getLongOpt()) {
                    case "wallet-directory":
                        walletDir = option.getValue();
                        break;
                    case "user-label":
                        userLabel = option.getValue();
                        break;
                    case "connection-profile":
                        connectionProfileDir = option.getValue();
                        break;
                    case "channel-name":
                        channelName = option.getValue();
                        break;
                    case "chaincode-name":
                        chaincodeName = option.getValue();
                        break;
                    case "contract-package":
                        contractPackage = option.getValue();
                        break;
                    case "transaction-name":
                        func = option.getValue();
                        break;
                    default:
                        logger.warn("当前不支持此参数 %s", option.toString());
                }
            }
        } catch (ParseException exp) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            return;
        }

        try {
            Gateway.Builder builder = Gateway.createBuilder();
            Path walletPath = Paths.get(walletDir);

            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            Path connectionProfile = Paths.get(connectionProfileDir);
            builder.identity(wallet, userLabel).networkConfig(connectionProfile);

            Gateway gateway = builder.connect();
            Network network = gateway.getNetwork(channelName);
            contract = network.getContract(chaincodeName, contractPackage);
        } catch (IOException e) {
            logger.error("", e);
        }

        RandomAccessFile RAFile = new RandomAccessFile("/tmp/fabric_upload.dat", "rw");
        FileChannel fc = RAFile.getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024);
        byte[] blockData = new byte[1024 * 1024];
        byte[] zeros = new byte[1024 * 1024];
        FileLock flock;
        while (true) {
            flock = fc.lock();
            mbb.clear();
            mbb.get(blockData);
            flock.release();
            String str = new String(blockData).trim();
            if (str.length() > 0) {
                logger.info(str);
                Map<String, byte[]> transientData = new HashMap<>();
                for (Map.Entry<String, Object> entry : JSON.parseObject(str).entrySet()) {
                    transientData.put(entry.getKey(), entry.getValue().toString().getBytes());
                }
                Transaction transaction = contract.createTransaction(func);
                transaction.setTransient(transientData);
                int rslt = Integer.parseInt(new String(transaction.submit()));
                if (rslt != State.OK.getCode()) {
                    logger.error("{}, {} failed", func, str);
                }

                flock = fc.lock();
                mbb.clear();
                mbb.put(zeros);
                flock.release();
            }
        }

    }
}