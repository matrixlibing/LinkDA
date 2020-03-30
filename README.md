# LinkDA
联接企业数据资产，打破数据孤岛现象

1,讯飞钱包生成
java -cp  javaclient.jar org.linkda.app.AddToWallet --ca-file ../crypto/crypto-config/peerOrganizations/voiceads.cn/users/User1@voiceads.cn/msp/signcerts/User1@voiceads.cn-cert.pem --label User1@voiceads.cn --mspid XFMSP --pk-file ../crypto/crypto-config/peerOrganizations/voiceads.cn/users/User1@voiceads.cn/msp/keystore/priv_sk -wallet-directory ./wallet

2,欢网钱包生成
java -cp javaclient.jar  org.linkda.app.AddToWallet --ca-file ../crypto/crypto-config/peerOrganizations/huanwang.com/users/Admin@huanwang.com/msp/signcerts/Admin@huanwang.com-cert.pem --label Admin@huanwang.com --mspid HWMSP --pk-file ../crypto/crypto-config/peerOrganizations/huanwang.com/users/Admin@huanwang.com/msp/keystore/priv_sk -wallet-directory ./wallet

3,欢网上传全量数据至私有数据
java -cp javaclient.jar org.linkda.app.UploadApplication --chaincode-name mycc1 --channel-name firstchannel --connection-profile ./networkConnection.yaml --contract-package org.linkda.chaincode --data-directory ./huanwang --user-label Admin@huanwang.com --wallet-directory ./wallet --transaction-name HWUpload --batch-size 1

4,欢网监听XFUpload
java -cp javaclient.jar org.linkda.app.BlockGeneratedListening --chaincode-name mycc1 --channel-name firstchannel --connection-profile ./networkConnection.yaml --contract-package org.linkda.chaincode --user-label Admin@huanwang.com --wallet-directory ./wallet --transaction-name XFUpload

      peers:
         peer2.voiceads.cn:
            endorsingPeer: false
            chaincodeQuery: true
            ledgerQuery: true
            eventSource: false
         peer2.huanwang.com:
            endorsingPeer: true
            chaincodeQuery: true
            ledgerQuery: true
            eventSource: true

5,新的块产生时欢网调用HWUpdate更新欢网私有数据
java -cp javaclient.jar org.linkda.app.UploadService --chaincode-name mycc1 --channel-name firstchannel --connection-profile ./networkConnection.yaml --contract-package org.linkda.chaincode --user-label Admin@huanwang.com --wallet-directory ./wallet --transaction-name HWUpdate

      peers:
         peer2.voiceads.cn:
            endorsingPeer: false
            chaincodeQuery: true
            ledgerQuery: true
            eventSource: false
         peer2.huanwang.com:
            endorsingPeer: true
            chaincodeQuery: true
            ledgerQuery: true
            eventSource: false

6,讯飞上传数据到公共账本
java -cp javaclient.jar org.linkda.app.UploadApplication --chaincode-name mycc1 --channel-name firstchannel --connection-profile ./networkConnection.yaml --contract-package org.linkda.chaincode --data-directory ./voiceads --user-label User1@voiceads.cn --wallet-directory ./wallet --transaction-name XFUpload --batch-size 1
