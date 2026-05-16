用户问题
   - 加载会话记忆
   - 查询重写/多问题拆分        
   - 意图识别
   - 歧义引导
   - 系统类问题快速回答
   - KB/MCP 检索与工具调用
   - 空结果处理
   - 组装 Prompt
   - LLM SSE 流式返回

对应代码：

```java
public void execute(StreamChatContext ctx) {
      loadMemory(ctx);

      rewriteQuery(ctx);

      resolveIntents(ctx);

      if (handleGuidance(ctx)) {
          return;
      }

      if (handleSystemOnly(ctx)) {
          return;
      }

      RetrievalContext retrievalCtx = retrieve(ctx);

      if (handleEmptyRetrieval(ctx, retrievalCtx)) {
          return;
      }

      streamRagResponse(ctx, retrievalCtx);
  }
```

**1. loadMemory(ctx)**

把当前会话的历史对话加载出来，放进 ctx.history。

作用是让后面的查询重写和最终回答能理解多轮上下文。比如用户问“那它怎么配置”，没有历史是看不懂的。

**2. rewriteQuery(ctx)**

对用户问题做查询重写。

这里主要做几件事：

术语归一化
结合最近历史对话理解当前问题
把省略/指代补全
把复杂问题拆成多个 subQuestions

结果会放到：

ctx.getRewriteResult()

里面主要有：

rewrittenQuestion：重写后的主问题，用来最终给 LLM 回答
subQuestions：拆出来的子问题，用来做意图识别和检索

**3. resolveIntents(ctx)**

对子问题做意图识别。

它会把每个 subQuestion 发给意图识别器，结合 Redis/DB 里的意图树，让 LLM 判断这个子问题属于哪些意图节点。

结果放到：

ctx.getSubIntents()

结构大概是：

SubQuestionIntent
subQuestion
List<NodeScore>

NodeScore
IntentNode
score

这里的 IntentNode 很关键，里面带了：

kind：KB / MCP / SYSTEM
collectionName：知识库向量集合
mcpToolId：工具 ID
promptTemplate：系统回答模板
topK：节点级检索数量

**4. handleGuidance(ctx)**

判断是否需要歧义引导。

比如用户问“OA 怎么用”，如果系统判断它可能对应多个系统/多个意图，而且分数接近，就不直接检索，而是返回一个澄清问题，让用
户选择。

但是这个项目这里有局限：它主要处理 单个子问题 的歧义。如果是多个子问题混合，它基本不会做很好的多任务澄清。

如果需要澄清：

callback.onContent(decision.getPrompt());
callback.onComplete();
return true;

然后流程提前结束。

**5. handleSystemOnly(ctx)**

判断是不是纯系统类问答。

比如：

你是谁？
你能做什么？
怎么使用这个助手？

如果所有子问题都是 SYSTEM 意图，就不走知识库和 MCP，直接调用系统回答链路，通过 SSE 返回。

注意：它要求 allSystemOnly。只要混了 KB/MCP，就不会在这里提前结束。

**6. retrieve(ctx)**

这是真正的 RAG 检索/工具阶段。

它会进入 RetrievalEngine，按每个子问题处理：

KB 意图 -> 多通道检索
MCP 意图 -> 参数提取 + 工具调用

KB 检索里面又有：

意图定向检索：根据 IntentNode.collectionName 查指定知识库
全局向量检索：意图不稳时查所有知识库兜底
结果融合：concat + dedup + rerank

最后产出：

RetrievalContext

里面主要有：

kbContext：知识库 chunks 格式化后的上下文
mcpContext：MCP 工具结果格式化后的上下文
intentChunks：意图 -> chunks 的映射，主要用于 prompt 规划/模板判断

**7. handleEmptyRetrieval(ctx, retrievalCtx)**

如果没有查到知识库内容，也没有 MCP 结果，就走空检索处理。

通常会返回类似：

没有找到相关资料
请换个问法
当前知识库暂无相关内容

具体文案看实现。

如果命中空结果处理，也提前结束。

**8. streamRagResponse(ctx, retrievalCtx)**

最后一步：生成回答。

它会把这些东西组装进 Prompt：

原问题/重写问题
历史对话
子问题
意图信息
kbContext
mcpContext
相关 promptSnippet / promptTemplate

然后调用 LLM：

llmService.streamChat(...)

通过 StreamCallback 把 token 一段段返回给前端，也就是 SSE 流式输出。

同时一般还会在完成后保存 assistant 回复到会话记忆里。

一句话总结

这个 execute 就是整个知识问答链路的编排器：

先用记忆和重写把问题变清楚，
再用意图识别决定走知识库、工具还是系统回答，
然后检索 KB / 调用 MCP 拿上下文，
最后把上下文塞进 Prompt，调用 LLM 流式生成答案。

> 项目的主链路由 StreamChatPipeline.execute 编排。请求进入后先加载会话记忆，再通过 LLM 做查询重写和多问题拆分；随后基
> 于 Redis 缓存的意图树对子问题做意图识别，并根据识别结果处理歧义引导、系统类问答、知识库检索和 MCP 工具调用。知识库检
> 索支持意图定向检索和全局向量检索，结果经过合并、去重和 Rerank 后形成 RetrievalContext，最后组装 Prompt 调用 LLM，并
> 通过 SSE 流式返回结果。