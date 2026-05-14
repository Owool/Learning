package Milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

import java.io.IOException;
import java.util.*;
import static Milvus.MilvusInsertDemo.getEmbeddings;

public class UserQueryDemo {
    public static void main(String[] args) throws IOException {
        MilvusClientV2 client = MilvusClient.getInstance();
        // 用户的问题
        String query = "买了东西不想要了怎么退货？";
        // 把问题向量化（复用前面的 getEmbeddings 方法）
        List<List<Float>> queryVectors = getEmbeddings(List.of(query));
        List<BaseVector> milvusQueryVectors = queryVectors.stream()
                .map(FloatVec::new)   // FloatVec(List<Float>)
                .collect(java.util.stream.Collectors.toList());
        // 执行向量检索
        SearchReq searchReq = SearchReq.builder()
                .collectionName("customer_service_chunks")
                .data(milvusQueryVectors)           // 查询向量
                .topK(3)                      // 返回最相似的 3 个结果
                .outputFields(List.of("chunk_text", "doc_id", "category"))  // 需要返回的字段
                .annsField("vector")          // 指定在哪个向量字段上检索
                .searchParams(Map.of("ef", 128))  // HNSW 检索时的搜索宽度
                .filter("category == \"return_policy\"")
                .build();
        SearchResp searchResp = client.search(searchReq);
        // 输出检索结果
        List<List<SearchResp.SearchResult>> results = searchResp.getSearchResults();
        for (List<SearchResp.SearchResult> resultList : results) {
            System.out.println("=== 检索结果 ===");
            for (int i = 0; i < resultList.size(); i++) {
                SearchResp.SearchResult result = resultList.get(i);
                System.out.println("Top-" + (i + 1) + "：");
                System.out.println("  相似度分数：" + result.getScore());
                System.out.println("  分类：" + result.getEntity().get("category"));
                System.out.println("  文档ID：" + result.getEntity().get("doc_id"));
                System.out.println("  内容：" + result.getEntity().get("chunk_text"));
                System.out.println();
            }
        }

        client.close();
    }
}
