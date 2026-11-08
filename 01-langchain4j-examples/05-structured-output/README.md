# 模块 05：结构化输出（Structured Outputs）

## 模块目标

让模型直接返回**强类型 Java 对象**，而不是让你手动解析字符串。

## 核心概念讲解

### 1. 原理

你在 AiService 接口里声明返回类型（POJO / record / enum / List），LangChain4j 会：
1. 根据返回类型生成 **JSON Schema**；
2. 要求模型**只输出符合 Schema 的 JSON**；
3. 用 Jackson 把 JSON **反序列化**成你的对象。

这样模型输出就直接变成了可编程的数据结构。

### 2. @Description 注解

`@Description`（`dev.langchain4j.model.output.structured.Description`）给字段加说明，让模型更准确地填值。

### 3. 适用场景

信息抽取、分类、打标签、生成结构化报告……凡是"从一段文本里提炼结构化信息"的需求都适用。

## 代码拆解

```java
record Person(
        @Description("姓名") String name,
        @Description("年龄") int age,
        @Description("所在城市") String city) {}

enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

interface Extractor {
    Person extractPerson(String text);
    Sentiment classify(String text);
    List<String> extractKeywords(String text);
}
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 05-structured-output exec:java -Dexec.mainClass=org.pyliu.learn.structured.Main
```

## 延伸思考

1. 返回类型越复杂，模型出错概率越高。字段加 `@Description`、类型尽量简单，能显著提升准确率。
2. 结构化输出本质是"提示词 + JSON 解析"的封装，模块 13 里打开 `logResponses(true)` 能看到模型实际返回的 JSON。
3. 复杂嵌套结构建议拆成多个简单方法，比一次返回超复杂对象更稳。
