package org.linkda.app;

import org.apache.commons.cli.*;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.impl.identity.X509IdentityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class AddToWallet {
    private static final Logger logger = LoggerFactory.getLogger(AddToWallet.class.getName());

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(null, "wallet-directory", true, "钱包位置，即身份导入的钱包路径");
        options.addOption(null, "label", true, "用户钱包标签");
        options.addOption(null, "mspid", true, "用户关联的组织ID（mspid）");
        options.addOption(null, "ca-file", true, "证书存储文件（PEM）");
        options.addOption(null, "pk-file", true, "私钥存储文件（PEM）");
        options.addOption("h", "help", false, "Usage");

        String walletDir = null;
        String label = null;
        String mspid = null;
        String caFile = null;
        String pkFile = null;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                return ;
            }

            if (!(line.hasOption("wallet-directory") &&
                    line.hasOption("label") &&
                    line.hasOption("mspid") &&
                    line.hasOption("ca-file") &&
                    line.hasOption("pk-file"))) {

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                throw new IllegalArgumentException("those arguments must pass [wallet-directory/mspid/ca-file/pk-file].");
            }

            for (Option option : line.getOptions()) {
                switch (option.getLongOpt()) {
                    case "wallet-directory":
                        walletDir = option.getValue(); break;
                    case "label":
                        label = option.getValue(); break;
                    case "mspid":
                        mspid = option.getValue(); break;
                    case "ca-file":
                        caFile = option.getValue();break;
                    case "pk-file":
                        pkFile = option.getValue();break;
                    default:
                        logger.warn("当前不支持此参数 %s", option.toString());
                }
            }
        } catch(ParseException exp ) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            return ;
        }

        try (BufferedReader pemReader = Files.newBufferedReader(Paths.get(pkFile));
             BufferedReader certReader = Files.newBufferedReader(Paths.get(caFile))) {
            PrivateKey pk = Identities.readPrivateKey(pemReader);
            X509Certificate certificate = Identities.readX509Certificate(certReader);
            Identity identity = new X509IdentityImpl(mspid, certificate, pk);
            Wallet wallet = Wallets.newFileSystemWallet(Paths.get(walletDir));
            wallet.put(label, identity);
        } catch (Exception e) {
            logger.error("", e);
        }
    }
}
