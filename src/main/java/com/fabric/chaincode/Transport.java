package com.fabric.chaincode;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import io.netty.handler.ssl.OpenSsl;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Transport extends ChaincodeBase {

    private static Log _logger = LogFactory.getLog(Transport.class);

    @Override
    public Response init(ChaincodeStub stub) {
        _logger.info("Init chaincode transport.");
        return newSuccessResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            _logger.info("Invoke chaincode transport.");
            String func = stub.getFunction();
            List<String> params = stub.getParameters();

            if (func.equals("save")) {
                return save(stub, params);
            } else if (func.equals("query")) {
                return query(stub, params);
            } else if (func.equals("queryByProductBatch")) {
                return queryByProductBatch(stub, params);
            } else if (func.equals("delete")) {
                return delete(stub, params);
            } else {
                return newErrorResponse("Invalid invoke function name!");
            }
        } catch (Throwable e){
            return newErrorResponse(e);
        }
    }

    // 保存记录
    private Response save(ChaincodeStub stub, List<String> args) {
        if (args.size() != 2) {
            return newErrorResponse("Incorrect number of arguments, Expecting 2.");
        }
        String key = args.get(0);
        String value = args.get(1);
        stub.putStringState(key, value);
        _logger.info(String.format("put state {%s:%s}.", key, value));
        return newSuccessResponse();
    }

    // 通过key值查询记录
    private Response query(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting id of the operate to query.");
        }
        String key = args.get(0);
        String value = stub.getStringState(key);
        if (StringUtil.isNullOrEmpty(value)) {
            return newErrorResponse(String.format("Error: state for %s is null.", key));
        }
        _logger.info(String.format("query result:\n {%s:%s}.", key, value));
        return newSuccessResponse(value, ByteString.copyFrom(value, UTF_8).toByteArray());
    }

    // 通过产品批次查询记录
    private Response queryByProductBatch(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting productBatch of the operate to query.");
        }
        String productBatch = args.get(0);
        String sql = String.format("{\"selector\":{\"productBatch\":\"%s\"}}", productBatch);
        _logger.info(sql);
        QueryResultsIterator<KeyValue> queryResultsIterator = stub.getQueryResult(sql);
        Iterator<KeyValue> it = queryResultsIterator.iterator();
        if (!it.hasNext()) {
            return newErrorResponse(String.format("Error: state for %s is null", productBatch));
        }
        List list = new ArrayList();
        while (it.hasNext()) {
            list.add(it.next().getStringValue());
        }
        String result = JSON.toJSONString(list);
        _logger.info(String.format("query result:\n %s", result));
        return newSuccessResponse(result, ByteString.copyFrom(result, UTF_8).toByteArray());
    }

    // 通过key值删除记录
    private Response delete(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting 1.");
        }
        String key = args.get(0);
        String value = stub.getStringState(key);
        if (StringUtil.isNullOrEmpty(value)) {
            return newErrorResponse(String.format("Error: state for %s is null.", key));
        }
        stub.delState(key);
        _logger.info(String .format("delete sate {%s:%s}.", key, value));
        return newSuccessResponse();
    }

    public static void main(String[] args) {
        System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());
        new Transport().start(args);
    }

}
