# 1

排查并修复如下bug，要求bug成因溯源和修复过程、修复后的测试过程都需要生成详细的专业文档：
BUG描述：
http://localhost:5173/admin/knowledge的知识库管理中，手动删除所有知识库和知识库下所有文档后，数据库中内容没有被同步删除，导致我再次建立同uk_collection_name知识库后会报错。
自我排查：
查看数据库发现，t_knowledge_base\t_knowledge_chunk\t_knowledge_document数据库中还存在我已经删除的内容。
发生场景如下，重启服务后，前端删除按钮删除所有知识库和文档，然后新建与已删除知识库同uk_collection_name的知识库就报错：
D:\envs\JDK\jdk-17.0.15\bin\java.exe -XX:TieredStopAtLevel=1 -Dspring.output.ansi.enabled=always -Dcom.sun.management.jmxremote -Dspring.jmx.enabled=true -Dspring.liveBeansView.mbeanDomain -Dspring.application.admin.enabled=true "-Dmanagement.endpoints.jmx.exposure.include=*" "-javaagent:D:\ProgramFiles\IT\IDEs\IntelliJ IDEA 2026.1\lib\idea_rt.jar=3554" -Dfile.encoding=UTF-8 -classpath C:\Users\Administrator\AppData\Local\Temp\classpath271966250.jar com.nageoffer.ai.ragent.RagentApplication

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.7)

2026-04-10T21:09:46.391+08:00  INFO 32404 --- [ragent-service] [           main] c.nageoffer.ai.ragent.RagentApplication  : Starting RagentApplication using Java 17.0.15 with PID 32404 (D:\Projects\Java_JS\Agent\ragent\bootstrap\target\classes started by Administrator in D:\Projects\Java_JS\Agent\ragent)
2026-04-10T21:09:46.394+08:00  INFO 32404 --- [ragent-service] [           main] c.nageoffer.ai.ragent.RagentApplication  : No active profile set, falling back to 1 default profile: "default"
2026-04-10T21:09:47.652+08:00  INFO 32404 --- [ragent-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Multiple Spring Data modules found, entering strict repository configuration mode
2026-04-10T21:09:47.656+08:00  INFO 32404 --- [ragent-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data Redis repositories in DEFAULT mode.
2026-04-10T21:09:47.737+08:00  INFO 32404 --- [ragent-service] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 62 ms. Found 0 Redis repository interfaces.
2026-04-10T21:09:48.900+08:00  INFO 32404 --- [ragent-service] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 9090 (http)
2026-04-10T21:09:48.917+08:00  INFO 32404 --- [ragent-service] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-04-10T21:09:48.918+08:00  INFO 32404 --- [ragent-service] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.48]
2026-04-10T21:09:48.959+08:00  INFO 32404 --- [ragent-service] [           main] o.a.c.c.C.[.[localhost].[/api/ragent]    : Initializing Spring embedded WebApplicationContext
2026-04-10T21:09:48.959+08:00  INFO 32404 --- [ragent-service] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2506 ms
2026-04-10T21:09:49.047+08:00  INFO 32404 --- [ragent-service] [           main] org.redisson.Version                     : Redisson 4.0.0
2026-04-10T21:09:49.526+08:00  INFO 32404 --- [ragent-service] [isson-netty-1-4] o.redisson.connection.ConnectionsHolder  : 1 connections initialized for 127.0.0.1/127.0.0.1:6379
2026-04-10T21:09:49.599+08:00  INFO 32404 --- [ragent-service] [sson-netty-1-19] o.redisson.connection.ConnectionsHolder  : 24 connections initialized for 127.0.0.1/127.0.0.1:6379
 _ _   |_  _ _|_. ___ _ |    _ 
| | |\/|_)(_| | |_\  |_)||_|_\ 
     /               |         
                        3.5.14 
2026-04-10T21:09:52.924+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.k.config.SemaphoreInitializer    : Initialized document upload semaphore: name=rag:document:upload, maxConcurrent=10
2026-04-10T21:09:54.745+08:00  INFO 32404 --- [ragent-service] [           main] o.a.r.s.a.RocketMQAutoConfiguration      : a producer (ragent-producer_pg) init on namesrv 127.0.0.1:9876
2026-04-10T21:09:57.957+08:00  INFO 32404 --- [ragent-service] [           main] ocketMQMessageListenerContainerRegistrar : Register the listener to container, listenerBeanName:knowledgeDocumentChunkConsumer, containerBeanName:org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer_1
2026-04-10T21:09:58.189+08:00  INFO 32404 --- [ragent-service] [           main] com.zaxxer.hikari.HikariDataSource       : RagentHikariPool - Starting...
2026-04-10T21:09:58.301+08:00  INFO 32404 --- [ragent-service] [           main] com.zaxxer.hikari.pool.HikariPool        : RagentHikariPool - Added connection org.postgresql.jdbc.PgConnection@5acbef42
2026-04-10T21:09:58.302+08:00  INFO 32404 --- [ragent-service] [           main] com.zaxxer.hikari.HikariDataSource       : RagentHikariPool - Start completed.
2026-04-10T21:09:58.325+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.r.QueryTermMappingService    : 查询归一化映射规则加载完成, 共加载 0 条规则
2026-04-10T21:09:58.374+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.mcp.DefaultMCPToolRegistry   : MCP 工具注册跳过, 未发现任何工具执行器
2026-04-10T21:09:58.374+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.mcp.DefaultMCPToolRegistry   : MCP 工具自动注册完成, 共注册 0 个工具
2026-04-10T21:09:58.438+08:00  INFO 32404 --- [ragent-service] [           main] n.a.r.r.c.m.c.MCPClientAutoConfiguration : 连接 MCP Server: name=default, url=http://localhost:9099
2026-04-10T21:09:58.532+08:00  INFO 32404 --- [ragent-service] [           main] n.a.r.r.c.m.c.MCPClientAutoConfiguration : MCP Server [default] 返回 3 个工具
2026-04-10T21:09:58.533+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.mcp.DefaultMCPToolRegistry   : MCP 工具注册成功, toolId: ticket_query
2026-04-10T21:09:58.533+08:00  INFO 32404 --- [ragent-service] [           main] n.a.r.r.c.m.c.MCPClientAutoConfiguration : 注册远程 MCP 工具: toolId=ticket_query, server=default
2026-04-10T21:09:58.533+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.mcp.DefaultMCPToolRegistry   : MCP 工具注册成功, toolId: sales_query
2026-04-10T21:09:58.534+08:00  INFO 32404 --- [ragent-service] [           main] n.a.r.r.c.m.c.MCPClientAutoConfiguration : 注册远程 MCP 工具: toolId=sales_query, server=default
2026-04-10T21:09:58.534+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.r.c.mcp.DefaultMCPToolRegistry   : MCP 工具注册成功, toolId: weather_query
2026-04-10T21:09:58.534+08:00  INFO 32404 --- [ragent-service] [           main] n.a.r.r.c.m.c.MCPClientAutoConfiguration : 注册远程 MCP 工具: toolId=weather_query, server=default
2026-04-10T21:09:59.269+08:00  INFO 32404 --- [ragent-service] [           main] ocketMQMessageListenerContainerRegistrar : Register the listener to container, listenerBeanName:messageFeedbackConsumer, containerBeanName:org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer_2
2026-04-10T21:09:59.330+08:00  INFO 32404 --- [ragent-service] [           main] c.n.a.r.f.d.SnowflakeIdInitializer       : 分布式Snowflake初始化完成, workerId: 6, datacenterId: 0
2026-04-10T21:09:59.657+08:00  INFO 32404 --- [ragent-service] [           main] m.e.s.MybatisPlusApplicationContextAware : Register ApplicationContext instances org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@640f11a1
2026-04-10T21:09:59.926+08:00  INFO 32404 --- [ragent-service] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 9090 (http) with context path '/api/ragent'
2026-04-10T21:10:04.086+08:00  INFO 32404 --- [ragent-service] [           main] a.r.s.s.DefaultRocketMQListenerContainer : running container: DefaultRocketMQListenerContainer{consumerGroup='knowledge-document-chunk_cg', namespace='', namespaceV2='', nameServer='127.0.0.1:9876', topic='knowledge-document-chunk_topic', consumeMode=CONCURRENTLY, selectorType=TAG, selectorExpression='*', messageModel=CLUSTERING', tlsEnable=false, instanceName=DEFAULT}
2026-04-10T21:10:07.146+08:00  INFO 32404 --- [ragent-service] [           main] a.r.s.s.DefaultRocketMQListenerContainer : running container: DefaultRocketMQListenerContainer{consumerGroup='message-feedback_cg', namespace='', namespaceV2='', nameServer='127.0.0.1:9876', topic='message-feedback_topic', consumeMode=CONCURRENTLY, selectorType=TAG, selectorExpression='*', messageModel=CLUSTERING', tlsEnable=false, instanceName=DEFAULT}
2026-04-10T21:10:07.158+08:00  INFO 32404 --- [ragent-service] [           main] c.nageoffer.ai.ragent.RagentApplication  : Started RagentApplication in 21.41 seconds (process running for 22.144)
2026-04-10T21:10:24.128+08:00  INFO 32404 --- [ragent-service] [nio-9090-exec-1] o.a.c.c.C.[.[localhost].[/api/ragent]    : Initializing Spring DispatcherServlet 'dispatcherServlet'
2026-04-10T21:10:24.128+08:00  INFO 32404 --- [ragent-service] [nio-9090-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2026-04-10T21:10:24.129+08:00  INFO 32404 --- [ragent-service] [nio-9090-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2026-04-10T21:10:27.966+08:00  INFO 32404 --- [ragent-service] [nio-9090-exec-1] c.n.a.r.r.c.vector.PgVectorStoreService  : 删除文档向量，collectionName=docs, docId=2042587161538088960, deleted=0
2026-04-10T21:10:37.292+08:00 ERROR 32404 --- [ragent-service] [nio-9090-exec-3] c.n.a.r.f.web.GlobalExceptionHandler     : [DELETE] http://localhost:9090/api/ragent/knowledge-base/2041378150029463552 [ex] ClientException{code='A000001',message='当前知识库下还有文档，请删除文档'} 

com.nageoffer.ai.ragent.framework.exception.ClientException: 当前知识库下还有文档，请删除文档
	at com.nageoffer.ai.ragent.knowledge.service.impl.KnowledgeBaseServiceImpl.delete(KnowledgeBaseServiceImpl.java:190)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)

2026-04-10T21:10:42.315+08:00  INFO 32404 --- [ragent-service] [io-9090-exec-10] c.n.a.r.r.c.vector.PgVectorStoreService  : 删除文档向量，collectionName=productdocs, docId=2041378743007580160, deleted=4
2026-04-10T21:11:05.538+08:00 ERROR 32404 --- [ragent-service] [nio-9090-exec-3] c.n.a.r.f.web.GlobalExceptionHandler     : [POST] http://localhost:9090/api/ragent/knowledge-base 

org.springframework.dao.DuplicateKeyException: 
### Error updating database.  Cause: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "uk_collection_name"
  详细：Key (collection_name)=(productdocs) already exists.
### The error may exist in com/nageoffer/ai/ragent/knowledge/dao/mapper/KnowledgeBaseMapper.java (best guess)
### The error may involve com.nageoffer.ai.ragent.knowledge.dao.mapper.KnowledgeBaseMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO t_knowledge_base  ( id, name, embedding_model, collection_name, created_by, updated_by, create_time, update_time, deleted )  VALUES (  ?, ?, ?, ?, ?, ?, ?, ?, ?  )
### Cause: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "uk_collection_name"
  详细：Key (collection_name)=(productdocs) already exists.
; ERROR: duplicate key value violates unique constraint "uk_collection_name"
  详细：Key (collection_name)=(productdocs) already exists.
	at org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator.doTranslate(SQLErrorCodeSQLExceptionTranslator.java:254) ~[spring-jdbc-6.2.12.jar:6.2.12]
	at org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator.translate(AbstractFallbackSQLExceptionTranslator.java:107) ~[spring-jdbc-6.2.12.jar:6.2.12]
	at org.mybatis.spring.MyBatisExceptionTranslator.translateExceptionIfPossible(MyBatisExceptionTranslator.java:95) ~[mybatis-spring-3.0.5.jar:3.0.5]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:347) ~[mybatis-spring-3.0.5.jar:3.0.5]
	at jdk.proxy2/jdk.proxy2.$Proxy95.insert(Unknown Source) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate.insert(SqlSessionTemplate.java:224) ~[mybatis-spring-3.0.5.jar:3.0.5]
	at com.baomidou.mybatisplus.core.override.MybatisMapperMethod.execute(MybatisMapperMethod.java:59) ~[mybatis-plus-core-3.5.14.jar:3.5.14]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy$PlainMethodInvoker.invoke(MybatisMapperProxy.java:156) ~[mybatis-plus-core-3.5.14.jar:3.5.14]
	at com.baomidou.mybatisplus.core.override.MybatisMapperProxy.invoke(MybatisMapperProxy.java:93) ~[mybatis-plus-core-3.5.14.jar:3.5.14]
	at jdk.proxy2/jdk.proxy2.$Proxy121.insert(Unknown Source) ~[na:na]
	at com.nageoffer.ai.ragent.knowledge.service.impl.KnowledgeBaseServiceImpl.create(KnowledgeBaseServiceImpl.java:89) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:360) ~[spring-aop-6.2.12.jar:6.2.12]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196) ~[spring-aop-6.2.12.jar:6.2.12]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-6.2.12.jar:6.2.12]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:380) ~[spring-tx-6.2.12.jar:6.2.12]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119) ~[spring-tx-6.2.12.jar:6.2.12]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184) ~[spring-aop-6.2.12.jar:6.2.12]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:728) ~[spring-aop-6.2.12.jar:6.2.12]
	at com.nageoffer.ai.ragent.knowledge.service.impl.KnowledgeBaseServiceImpl$$SpringCGLIB$$0.create(<generated>) ~[classes/:na]
	at com.nageoffer.ai.ragent.knowledge.controller.KnowledgeBaseController.createKnowledgeBase(KnowledgeBaseController.java:57) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:258) ~[spring-web-6.2.12.jar:6.2.12]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:191) ~[spring-web-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:991) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:896) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:914) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:547) ~[jakarta.servlet-api-6.0.0.jar:6.0.0]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.2.12.jar:6.2.12]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:614) ~[jakarta.servlet-api-6.0.0.jar:6.0.0]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at com.nageoffer.ai.ragent.rag.config.Utf8ResponseFilter.doFilter(Utf8ResponseFilter.java:66) ~[classes/:na]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at cn.dev33.satoken.filter.SaFirewallCheckFilterForJakartaServlet.doFilter(SaFirewallCheckFilterForJakartaServlet.java:69) ~[sa-token-spring-boot3-starter-1.43.0.jar:na]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at cn.dev33.satoken.filter.SaTokenCorsFilterForJakartaServlet.doFilter(SaTokenCorsFilterForJakartaServlet.java:52) ~[sa-token-spring-boot3-starter-1.43.0.jar:na]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet.doFilter(SaTokenContextFilterForJakartaServlet.java:40) ~[sa-token-spring-boot3-starter-1.43.0.jar:na]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.2.12.jar:6.2.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.12.jar:6.2.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-6.2.12.jar:6.2.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.12.jar:6.2.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-6.2.12.jar:6.2.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.12.jar:6.2.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at com.nageoffer.ai.ragent.knowledge.filter.UploadRateLimitFilter.doFilterInternal(UploadRateLimitFilter.java:57) ~[classes/:na]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.12.jar:6.2.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:162) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:138) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:165) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:88) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:482) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:113) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:83) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:72) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1774) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:973) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:491) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63) ~[tomcat-embed-core-10.1.48.jar:10.1.48]
	at java.base/java.lang.Thread.run(Thread.java:842) ~[na:na]
Caused by: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "uk_collection_name"
  详细：Key (collection_name)=(productdocs) already exists.
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2736) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2421) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:372) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.jdbc.PgStatement.executeInternal(PgStatement.java:525) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:435) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.jdbc.PgPreparedStatement.executeWithFlags(PgPreparedStatement.java:196) ~[postgresql-42.7.8.jar:42.7.8]
	at org.postgresql.jdbc.PgPreparedStatement.execute(PgPreparedStatement.java:182) ~[postgresql-42.7.8.jar:42.7.8]
	at com.zaxxer.hikari.pool.ProxyPreparedStatement.execute(ProxyPreparedStatement.java:44) ~[HikariCP-6.3.3.jar:na]
	at com.zaxxer.hikari.pool.HikariProxyPreparedStatement.execute(HikariProxyPreparedStatement.java) ~[HikariCP-6.3.3.jar:na]
	at org.apache.ibatis.executor.statement.PreparedStatementHandler.update(PreparedStatementHandler.java:48) ~[mybatis-3.5.19.jar:3.5.19]
	at org.apache.ibatis.executor.statement.RoutingStatementHandler.update(RoutingStatementHandler.java:75) ~[mybatis-3.5.19.jar:3.5.19]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.apache.ibatis.plugin.Plugin.invoke(Plugin.java:61) ~[mybatis-3.5.19.jar:3.5.19]
	at jdk.proxy2/jdk.proxy2.$Proxy141.update(Unknown Source) ~[na:na]
	at org.apache.ibatis.executor.SimpleExecutor.doUpdate(SimpleExecutor.java:50) ~[mybatis-3.5.19.jar:3.5.19]
	at org.apache.ibatis.executor.BaseExecutor.update(BaseExecutor.java:117) ~[mybatis-3.5.19.jar:3.5.19]
	at org.apache.ibatis.executor.CachingExecutor.update(CachingExecutor.java:76) ~[mybatis-3.5.19.jar:3.5.19]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.apache.ibatis.plugin.Invocation.proceed(Invocation.java:61) ~[mybatis-3.5.19.jar:3.5.19]
	at com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor.intercept(MybatisPlusInterceptor.java:106) ~[mybatis-plus-extension-3.5.14.jar:3.5.14]
	at org.apache.ibatis.plugin.Plugin.invoke(Plugin.java:59) ~[mybatis-3.5.19.jar:3.5.19]
	at jdk.proxy2/jdk.proxy2.$Proxy140.update(Unknown Source) ~[na:na]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.update(DefaultSqlSession.java:197) ~[mybatis-3.5.19.jar:3.5.19]
	at org.apache.ibatis.session.defaults.DefaultSqlSession.insert(DefaultSqlSession.java:184) ~[mybatis-3.5.19.jar:3.5.19]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:333) ~[mybatis-spring-3.0.5.jar:3.0.5]
	... 86 common frames omitted