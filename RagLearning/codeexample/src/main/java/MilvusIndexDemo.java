import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.index.request.CreateIndexReq;

import java.util.*;

public class MilvusIndexDemo {
    public static void main(String[] args) {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri("http://localhost:19530")
                .build();

        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        IndexParam vectorIndex = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of(
                        "M",16,
                        "efConstruction",256
                ))
                .build();

        IndexParam categoryIndex = IndexParam.builder()
                .fieldName("category")
                .indexType(IndexParam.IndexType.TRIE)
                .build();

        CreateIndexReq createIndexReq = CreateIndexReq.builder()
                .collectionName("customer_service_chunks")
                .indexParams(List.of(vectorIndex,categoryIndex))
                .build();

        client.createIndex(createIndexReq);
        System.out.println("索引创建成功");
        client.close();
    }
}
