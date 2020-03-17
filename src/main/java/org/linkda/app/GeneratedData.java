package org.linkda.app;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

public class GeneratedData {
    private static final Logger logger = LoggerFactory.getLogger(GeneratedData.class.getName());

    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("c", "company", true, "公司名称（生成不同格式数据）");
        options.addOption("o", "output", true, "输出文件");
        options.addOption("l", "line", true, "随机数据条数");
        options.addOption("h", "help", false, "Usage");

        String type = null;
        String file = null;
        int number = 1000000;
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                return ;
            }

            if (!(line.hasOption("c") && line.hasOption("o"))) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("应用命令参数说明.", options, true);
                throw new IllegalArgumentException("those arguments must pass [co].");
            }


            for (Option option : line.getOptions()) {
                switch (option.getLongOpt()) {
                    case "company":
                        type = option.getValue();break;
                    case "output":
                        file = option.getValue();break;
                    case "line":
                        number = Integer.parseInt(option.getValue());break;
                    default:
                        logger.warn("当前不支持此参数 {}", option.toString());
                }
            }
        } catch(ParseException exp ) {
            logger.error("当前不支持此参数，" + exp.getMessage());
            return ;
        }

        switch (type) {
            case "iflytek": makeIflytek(number, file);break;
            case "gouzheng": makeGouzheng(number, file);break;
        }

    }

    //18:d7:17:b8:4d:f1 {"mac":"18:d7:17:b8:4d:f1","imei":["865856042515193"],"ottmac":["18:d7:17:b8:4d:f1"]}
    private static void makeGouzheng(int num, String file) throws IOException {
        File oFile = new File(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(oFile));

        for (int index = 0; index < num; index++) {
            String mac = randomMAC();
            String imei = randomIMEI();
            String ottmac = randomMAC();

            StringBuffer buff = new StringBuffer();
            buff.append(mac).append(" {\"mac\":\"").append(mac).append("\",\"imei\":[\"")
                    .append(imei).append("\"],\"ottmac\":[\"")
                    .append(ottmac).append("\"]}\n");

            writer.write(buff.toString());
        }

        writer.flush();
        writer.close();
    }

    //18:d7:17:b8:4d:f1 {"mac":"18:d7:17:b8:4d:f1","imei":["865856042515193"],"tags":{"city":"合肥","gender":"f"}}
    private static void makeIflytek(int num, String file) throws IOException {
        File oFile = new File(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(oFile));

        for (int index = 0; index < num; index++) {
            String mac = randomMAC();
            String imei = randomIMEI();
            char gender = randomGender();

            StringBuffer buff = new StringBuffer();
            buff.append(mac).append(" {\"mac\":\"").append(mac).append("\",\"imei\":[\"")
                    .append(imei).append("\"],\"tags\":{\"city\":\"hefei\",\"gender\":\"")
                    .append(gender).append("\"}}\n");

            writer.write(buff.toString());
        }

        writer.flush();
        writer.close();
    }

    public static Random rand = new Random();
    private static char[] macs = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static String randomMAC() {
        StringBuffer buff = new StringBuffer();
        buff.append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]).append(':')
            .append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]).append(':')
            .append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]).append(':')
            .append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]).append(':')
            .append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]).append(':')
            .append(macs[rand.nextInt(16)]).append(macs[rand.nextInt(16)]);
        return buff.toString();
    }

    private static char[] imeis = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static String randomIMEI() {
        StringBuffer buff = new StringBuffer();
        for (int index = 0; index < 15; index ++) {
            buff.append(imeis[rand.nextInt(10)]);
        }
        return buff.toString();
    }

    private static char[] gender = new char[] {'m', 'f'};
    private static char randomGender() {
        return gender[rand.nextInt(2)];
    }
}
