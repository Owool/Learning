package Milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;

public class MilvusClient {
    private static final String URI = "http://localhost:19530";
    static class Hold{
        private static final MilvusClientV2 INSTANCE = new MilvusClientV2(ConnectConfig.builder()
                .uri("http://localhost:19530")
                .build());
    }
    public static MilvusClientV2 getInstance(){
        return Hold.INSTANCE;
    }

}
