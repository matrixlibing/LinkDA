package org.linkda.app;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.cli.*;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BlockGeneratedListening {
    private static final Logger logger = LoggerFactory.getLogger(BlockGeneratedListening.class.getName());

    static String walletDir = null;
    static String userLabel = null;
    static String connectionProfileDir = null;
    static String channelName = null;
    static String chaincodeName = null;
    static String contractPackage = null;
    static String func = null;

    public static void main(String[] args) throws InterruptedException {
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
                        logger.warn("当前不支持此参数 {}", option.toString());
                }
            }

        } catch (ParseException exp) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            return;
        }

        Gateway gateway = null;
        //增加BlockListener,接受network中的块生产信息
        try {
            Gateway.Builder builder = Gateway.createBuilder();
            Path walletPath = Paths.get(walletDir);

            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            Path connectionProfile = Paths.get(connectionProfileDir);
            builder.identity(wallet, userLabel).networkConfig(connectionProfile);
            gateway = builder.connect();

            Network network = gateway.getNetwork(channelName);

            //共享内存
            RandomAccessFile RAFile = new RandomAccessFile("/tmp/fabric_upload.dat", "rw");
            FileChannel fc = RAFile.getChannel();
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024);

            final String finalChannelName = channelName;
            final String finalChaincodeName = chaincodeName;
            final String chaincodeInputArg0 = contractPackage + ":" + func;
            network.addBlockListener(new Consumer<BlockEvent>() {
                @Override
                public void accept(BlockEvent blockEvent) {
                    // 对于已经写入块的消息进行监听
                    Iterator<BlockEvent.TransactionEvent> txEventIterator = blockEvent.getTransactionEvents().iterator();
                    while (txEventIterator.hasNext()) {
                        BlockEvent.TransactionEvent txEvent = txEventIterator.next();
                        //只处理channel name 的Tx
                        if (!finalChannelName.equals(txEvent.getChannelId())) {
                            continue;
                        }

                        BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo txActionInfo = txEvent.getTransactionActionInfo(0);
                        //只处理contract name & function name 的Tx
                        if (!new String(txActionInfo.getChaincodeInputArgs(0)).equals(chaincodeInputArg0)) {
                            continue;
                        }

                        //只处理chaincode name 的Tx
                        if (!txActionInfo.getChaincodeIDName().equals(finalChaincodeName)) {
                            continue;
                        }

                        txActionInfo.getTxReadWriteSet().getNsRwsetInfos().forEach(new Consumer<TxReadWriteSetInfo.NsRwsetInfo>() {
                            @Override
                            public void accept(TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo) {
                                if (nsRwsetInfo.getNamespace().equals(finalChaincodeName)) {
                                    List<KvRwset.KVWrite> kvWrites = null;
                                    try {
                                        kvWrites = nsRwsetInfo.getRwset().getWritesList();
                                        Map<String, String> transientData = new HashMap<>();
                                        for (KvRwset.KVWrite kvWrite : kvWrites) {
                                            String mac = kvWrite.getKey();
                                            String value = new String(kvWrite.getValue().toByteArray());
                                            transientData.put(mac, value);
                                            logger.info("BlockId {}, Key {}, Value {}", blockEvent.getBlockNumber(), mac, value);
                                        }
                                        try {
                                            byte[] zeros = new byte[1024 * 1024];
                                            FileLock flock = fc.lock();
                                            mbb.clear();
                                            mbb.put(zeros);

                                            mbb.clear();
                                            mbb.put(JSON.toJSONBytes(transientData));
                                            flock.release();

                                            byte[] blockData = new byte[1024 * 1024];
                                            while (true) {
                                                flock = fc.lock();
                                                mbb.clear();
                                                mbb.get(blockData);
                                                flock.release();

                                                String str = new String(blockData).trim();
                                                logger.info(str);
                                                if(str.length() == 0)
                                                    break;
                                            }
                                        } catch (Exception e) {
                                            logger.error(e.toString());
                                        }
                                    } catch (InvalidProtocolBufferException e) {
                                        logger.warn("BlockId {}, but error at {}", blockEvent.getBlockNumber(), e);
                                    }
                                }
                            }
                        });
                    }
                }
            });

            Thread.currentThread().join();
        } catch (IOException e) {
            logger.error("{}", e);
        } finally {
            gateway.close();
        }

        logger.info("main thread finished.");
    }
}
