package com.huma.contractevent;

/**
 * @author hudenian
 * @date 2021/8/13
 * @description 功能描述
 */

import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.websocket.WebSocketService;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

/**
 * Event log 相关 监听合约event
 */
@Slf4j
public class ContractEvent {

    private static String contractAddress = "0xE016bAedb7AFFe1879a13694756721149069d45e";
    private static Web3j web3j;

    public static void main(String[] args) throws ConnectException {
        WebSocketService ws = new WebSocketService("wss://rinkeby.infura.io/ws/v3/c4b3ad766de2415b8203256db554aa7b", true);
        ws.connect();
        Web3j web3j = Web3j.build(ws);

        /**
         * 监听ERC20 token 交易
         */
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.EARLIEST,
                DefaultBlockParameterName.LATEST,
                contractAddress);
        Event event = new Event("Transfer",
                Arrays.asList(
                        new TypeReference<Address>(true) {
                        },
                        new TypeReference<Address>(true) {
                        }, new TypeReference<Uint256>(false) {
                        }
                )
        );

        String topicData = EventEncoder.encode(event);
        filter.addSingleTopic(topicData);
        log.info(topicData);

        Disposable subscription = web3j.ethLogFlowable(filter)
                .subscribe(ethLog -> {
                    log.info("收到监听事件所在块高:  " + ethLog.getBlockNumber());
                    log.info("block number:  " + ethLog.getBlockNumber());
                    log.info("transaction txHash: " + ethLog.getTransactionHash());
                    List<String> topics = ethLog.getTopics();
                    for (String topic : topics) {
                        log.info("topic: " + topic);
                    }
                });
    }
}
