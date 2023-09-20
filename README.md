##	About esProc SPL


To put it simply, esProc SPL is an intriguing programming language and a powerful data computing middleware, and can also work as a high-efficiency data warehouse: find more in [Understand SPL in three aspects](https://blog.scudata.com/understand-spl-in-three-aspects/).

Different from the text-based programming language, SPL writes code in gridlines: find more in [A programming language coding in a grid](https://blog.scudata.com/a-programming-language-coding-in-a-grid/); as a data computing engine, SPL can generate high efficiency at much lower cost:  [esProc SPL, a data analysis engine reducing application cost by N times](https://blog.scudata.com/esproc-spl-a-data-analysis-engine-reducing-application-cost-by-n-times/).

esProc SPL is a JVM-based data computing class library: [SPL: The Open-source Java Library to Process Structured Data](https://blog.scudata.com/spl-the-open-source-java-library-to-process-structured-data/). It has much more and better functionalities than the other data processing languages based on JVM (Such as Kotlin and Scala): [Competition of data processing languages on JVM: Kotlin, Scala and SPL](https://blog.scudata.com/competition-of-data-processing-languages-on-jvm-kotlin-scala-and-spl/). It can perform SQL-style computations without databases:  [Open-source SPL that can execute SQL without RDB](https://blog.scudata.com/open-source-spl-that-can-execute-sql-without-rdb/), provides multi/diverse-source, mixed computing capability:  [Which Tool Is Ideal for Diverse Source Mixed Computations](https://blog.scudata.com/which-tool-is-ideal-for-diverse-source-mixed-computations/), and supports direct computations on files:  [Computing engine on open-format files](https://blog.scudata.com/computing-engine-on-open-format-files/) and on WEB: [Computing engine on WEB](https://blog.scudata.com/computing-engine-on-web/). Particularly, esProc SPL enables more flexible microservices: [Open-source SPL Makes Microservices More "Micro"](https://blog.scudata.com/open-source-spl-makes-microservices-more-micro/) and convenient data preparation processing for report queries:  [The Open-source SPL Optimizes Report Application and Handles Endless Report Development Needs](https://blog.scudata.com/the-open-source-spl-optimizes-report-application-and-handles-endless-report-development-needs/). esProc can also be embedded into an application to act as a built-in database:  [esProc SPL, the challenger of SQLite](https://blog.scudata.com/esproc-spl-the-challenger-of-sqlite/).

esProc SPL enriches the concept of middleware:  [DCM: A New Member of Middleware Family](https://blog.scudata.com/dcm-a-new-member-of-middleware-family/).

When working as a data warehouse, esProc SPL does not adopt the relational algebra-based SQL syntax. It invents an algebraic system called discrete data set instead:  [SPL: a database language featuring easy writing and fast running](https://blog.scudata.com/spl-a-database-language-featuring-easy-writing-and-fast-running/)  (Documentation: [Paper of Discrete Data Set](https://c.scudata.com/article/1694595486828)) to solve the problems of hard-to-code complex SQL ([Why a SQL Statement Often Consists of Hundreds of Lines, Measured by KBs？](https://blog.scudata.com/why-a-sql-statement-often-consists-of-hundreds-of-lines-measured-by-kbs%ef%bc%9f/)).

SPL makes it convenient to achieve high-performance algorithms and thus obtains much higher computing performance than the traditional relational data warehouse: [How the performance improvement by orders of magnitude happened](https://blog.scudata.com/how-the-performance-improvement-by-orders-of-magnitude-happened/). Find test reports in [SPL Performance Testing](https://blog.scudata.com/spl-technology-evaluation/). It can make the most use of the hardware resources by using creative algorithms. According to many practical instances, esProc can achieve, even exceed, the performance that the distributed databases have on a single machine.

As a data warehouse, esProc abandons the concept of “house” , breaks the closedness featured by the conventional databases and creates an open computing system: [Data warehouse with “no house” performs better than the one with “the house”](https://blog.scudata.com/data-warehouse-with-no-house-performs-better-than-the-one-with-the-house/), making it qualified to replace most MPP data warehouses at lower resource-cost and with lighter framework: [With lightweight SPL available, how necessary is MPP?](https://blog.scudata.com/with-lightweight-spl-available-how-necessary-is-mpp/) .

##	To Learn esProc SPL

This book: [SPL Programming](http://c.scudata.com/article/1634722432114) is a good start for learning SPL syntax. The book intends for beginners who do not have any programming experiences. Look it through quickly if you are a veteran, but the object understanding explained in section 4.4 is worth a study. Chapter 5 is important, too. It explains SPL’s set-oriented way of thinking, which is quite different from the other languages. But once you understand and master SPL, you can write elegant code. Chapters 8-10 are staple of SPL learning. It regards the structured data computations in a different perspective from SQL. This is significant even for the professional programmers! From the SPL point of view, SQL is a little simple in understanding the structured data as the world is complex. **The knowledges you obtained in various database courses are not broad and profound enough! You need a review and brush-up！**

Find basic SPL concepts in this post: [SPL concepts for beginners](https://blog.scudata.com/spl-concepts-for-beginners/). For beginners, you can find characteristic basic computations of SPL in [SPL Operations for Beginners](https://blog.scudata.com/spl-operations-for-beginners/). Experienced programmers can quickly understand the differences between SPL and SQL. A software architect can understand the differences between SPL and traditional databases after reading  [Q&A of esProc Architecture](https://blog.scudata.com/qa-of-esproc-architecture/).

Find comprehensive SPL documentation in  [SPL Learning materials](https://blog.scudata.com/spl-learning-materials/). Generally, an application programmer can get started in handling basic operations from database connection:  [SPL: Connecting to Databases](https://blog.scudata.com/spl-connecting-to-databases/) and database read/write  [SPL: Reading and Writing Database Data](https://blog.scudata.com/spl-reading-and-writing-database-data/) or file access and computation  [SPL: Reading and Writing Structured Text Files](https://blog.scudata.com/spl-reading-and-writing-structured-text-files/). Then you can learn how to integrate SPL in a Java application [How to Call an SPL Script in Java](https://blog.scudata.com/how-to-call-an-spl-script-in-java/). Those make a simple learning loop.

High-performance computations are relatively difficult, but there is a systematic book on algorithms: [Performance Optimization](https://c.scudata.com/article/1641367696194). Performance optimization algorithms are not unique to SPL. You can implement high-performance computations using another programming language (except for SQL) after you learn these algorithms. The key lies in algorithm instead of syntax. Yet, you need to grasp SPL concept and syntax well in order to better understand the algorithms.

The SPL learning posts above also contain applications of the performance optimization algorithms.

Storage forms the cornerstone of high-performance computing. The following post introduces the proprietary storage schema commonly used in SPL for beginners: [How to use SPL storage for beginners](https://blog.scudata.com/how-to-use-spl-storage-for-beginners/). Usually, the first step of performance optimization is designing an appropriate storage schema.

You are welcome to post your troubles and problems when trying to achieve high performance computing and discuss with us to find a solution: [Wanted! Unbearably slow query and batch job](http://www.scudata.com/html/Unbearably-slow-query-and-batch-job.html).



## Useful Links

*   esProc Official WebSite: http://www.scudata.com  Forum: http://c.scudata.com/
*   [Tutorial](http://doc.scudata.com/esproc/tutorial/) esProc download, installation, as well as principles and applications
*   [Function Reference](http://doc.scudata.com/esproc/func/) esProc syntax, applications and examples
*   [User Reference](http://doc.scudata.com/esproc/manual/) esProc programming by examples
*   [External Library Guide](http://doc.scudata.com/esproc/ext/) Deployment of and connection to esProc external libraries
*   Please head to [Download esProc SPL] (http://c.scudata.com/article/1595817756260) to download esProc executable files
*   [How to Get Open-source esProc for Eclipse through Git](http://c.scudata.com/article/1677815008127) 

## License

esProc is under the Apache 2.0 license. See the [LICENSE](./LICENSE) file for details.
