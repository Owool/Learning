import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;

public class MilvusDemo {
    private static final int VECTOR_DIM = 4096;
    private static final String COLLECTION_NAME = "customer_service_chunks";

    public static void main(String[] args) {
        //连接配置
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri("http://localhost:19530")
                .build();

        MilvusClientV2 client = new MilvusClientV2(connectConfig);

        //定义schema
        CreateCollectionReq.CollectionSchema schema = client.createSchema();

        //主键id
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(true)
                .build());

        //向量字段
        schema.addField(AddFieldReq.builder()
                .fieldName("vector")
                .dataType(DataType.FloatVector)
                .dimension(VECTOR_DIM)
                .build());

        //标量字段:Chunk
        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_text")
                .dataType(DataType.VarChar)
                .maxLength(8192)
                .build());
        //标量字段:文档ID
        schema.addField(AddFieldReq.builder()
                .fieldName("doc_id")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());
        //标量字段:分类
        schema.addField(AddFieldReq.builder()
                .fieldName("category")
                .dataType(DataType.VarChar)
                .maxLength(32)
                .build());

        //创建Collection
        CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
                .collectionName(COLLECTION_NAME)
                .collectionSchema(schema)
                .build();

        client.createCollection(createCollectionReq);
        System.out.println("Collection 创建成功：" + COLLECTION_NAME);
    }
}
