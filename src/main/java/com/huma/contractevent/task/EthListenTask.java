package com.huma.contractevent.task;

import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Uint;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * @author hudenian
 * @date 2021/8/12
 * @description 功能描述
 */
@Slf4j
@Component
public class EthListenTask {

    /**
     * 代币合约地址列表,可以存放多个地址
     */
    public List<String> contracts;

    /**
     * token事件订阅对象
     */
    public Disposable tokenSubscription;

    /**
     * ETH交易空档事件订阅对象
     */
    public Subscription ethMissSubscription;

    /**
     * ETH交易事件订阅对象
     */
    public Subscription ethSubscription;


    public String htAddress = "0xjasdfasldjfaldfsasd";

    @Scheduled(cron = "*/1 * * * * ?")
    public void syncEthBlock() throws Exception {
        log.info("listen event>>>>>>>");
        BigInteger startBlock = new BigInteger("10000");

        WebSocketService ws = new WebSocketService("wss://rinkeby.infura.io/ws/v3/c4b3ad766de2415b8203256db554aa7b", true);
        ws.connect();
        Web3j web3j = Web3j.build(ws);

        // 要监听的合约事件
        Event event = new Event("Transfer",
                Arrays.asList(
                        new TypeReference<Address>() {
                        },
                        new TypeReference<Address>() {
                        },
                        new TypeReference<Uint>() {
                        }));

        //过滤器
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(startBlock),
                DefaultBlockParameterName.LATEST,
                contracts);

        filter.addSingleTopic(EventEncoder.encode(event));

        //注册监听,解析日志中的事件
        tokenSubscription = web3j.ethLogFlowable(filter).subscribe(ethLog -> {
            int block_TokenSub = ethLog.getBlockNumber().intValue();
            String token = ethLog.getAddress();  //这是Token合约地址
            String txHash = ethLog.getTransactionHash();
            List<String> topics = ethLog.getTopics();  // 提取转账记录

            String fromAddress = "0x" + topics.get(0);
            String toAddress = "0x" + topics.get(2);
            System.out.println("  ---token =" + token + ",  txHash =" + txHash);

            //检查发送地址、接收地址是否属于系统用户， 不是系统用户就不予处理
            if (htAddress.equalsIgnoreCase(fromAddress) || htAddress.equalsIgnoreCase(toAddress)) {

                String value1 = ethLog.getData();
                BigInteger big = new BigInteger(value1.substring(2), 16);
                BigDecimal value = Convert.fromWei(big.toString(), Convert.Unit.ETHER);
                String timestamp = "";

                try {
                    EthBlock ethBlock = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(ethLog.getBlockNumber()), false).send();
                    timestamp = String.valueOf(ethBlock.getBlock().getTimestamp());

                } catch (IOException e) {
                    System.out.println("Block timestamp get failure,block number is {}" + ethLog.getBlockNumber());
                    System.out.println("Block timestamp get failure,{}" + e.getMessage());

                }
            }

        }, error -> {
            System.out.println(" ### tokenSubscription   error= " + error);
            error.printStackTrace();
        });
        System.out.println("tokenSubscription =" + tokenSubscription);
    }
}
