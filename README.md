
## Know about esProc

__Agile Data Computing Middleware__

Computing middleware: a programmable general software between application and data, which can perform computing independently. It is often used to solve problems such as loose coupling, high performance, special source computing, multi-source hybrid computing, complex logic, etc.

![image](https://user-images.githubusercontent.com/2794999/145353353-d74a5b54-751c-45be-8589-aa65a1978ae9.png)

Note： This diagram focuses on the mainstream embedded and Java application architecture, and esProc also supports the independent and non Java application architecture. 

For latest package and release notes, see [Download esProc Community Edition Package](http://c.raqsoft.com/article/1595817756260).

## Know about SPL

<b>S</b>tructured <b>P</b>rocess <b>L</b>anguage — <b>SPL</b> is the scripting language on which esProc is based. SPL script is the counterpart of the stored procedure in RDB. A SPL script will be passed to a Java program through JDBC interface to be executed or to achieve the structured computation.

   <img src="http://www.raqsoft.com/wp-content/themes/raqsoft2017-en/images/java-computing/3.png" width="800" height="300">

- __Combined the advantages of Java, Beyond SQL__

  [Comparison of SQL & SPL: Set-oriented Operations](http://c.raqsoft.com/article/1622598686173)

  [Comparison of SQL & SPL: Select Operation](http://c.raqsoft.com/article/1625729370376)

  [Comparison of SQL & SPL: Order-based Computations](http://c.raqsoft.com/article/1621587675540)

  [Comparison of SQL & SPL: Equi-grouping](http://c.raqsoft.com/article/1620289765361)

  [Comparison of SQL & SPL: Non-equi-grouping](http://c.raqsoft.com/article/1621233528244)

  [Comparison of SQL & SPL: Order-based Computations](http://c.raqsoft.com/article/1621232555689)

  [Comparison of SQL & SPL: Join Operations (Ⅰ)](http://c.raqsoft.com/article/1622615623417)

  [Comparison of SQL & SPL: Join Operations (Ⅱ)](http://c.raqsoft.com/article/1623046924829)

  [Comparison of SQL & SPL: Join Operations (Ⅲ)](http://c.raqsoft.com/article/1625738996195)

  [Comparison of SQL & SPL: Static Transposition](http://c.raqsoft.com/article/1621588427124)

  [Comparison of SQL & SPL: Complicated Static Transposition](http://c.raqsoft.com/article/1621588222144)

  [Comparison of SQL & SPL: Dynamic Transposition](http://c.raqsoft.com/article/1625729514671)

  [Comparison of SQL & SPL: Recursion Operation](http://c.raqsoft.com/article/1621234716519)

- __Well designed rich library functions and consistency syntax,Easier to master and better performance than Python.__

  Example: Find out the sales clerks whose sales are within top 8 for every moth in 1995.

  Python:
   
  ```python
  import pandas as pd
  sale_file = ‘E:\\txt\\SalesRecord.txt’
  sale_info = pd.read_csv(sale_file,sep = ‘\t’)
  sale_info[‘month’]=pd.to_datetime(sale_info[‘sale_date’]).dt.month
  sale_group = sale_info.groupby(by=[‘clerk_name’,‘month’],as_index=False).sum()
  sale_group_month = sale_group.groupby(by=‘month’)
  set_name = set(sale_info[‘clerk_name’])
  for index,sale_g_m in sale_group_month:
      sale_g_m = sale_g_m.sort_values(by=‘sale_amt’,ascending = False)
      sale_g_max_8 = sale_g_m.iloc[:8]
      sale_g_max_8_name = sale_g_max_8[‘clerk_name’]
      set_name = set_name.intersection(set(sale_g_max_8_name))
  print(set_name)
  ```
  
  SPL:
  
  |+|A|
  |:-|:-|
  |1|E:\\txt\\SalesRecord.txt|
  |2|=file(A1).import@t()|
  |3|=A2.groups(clerk_name:name,month(sale_date):month;sum(sale_amt):amount)|
  |4|=A3.group(month)|
  |5|=A4.(\~.sort(-amount).to(8))|
  |6|=A5.isect(\~.(name))|

- __Seamless integration into Java applications__
  
  For more details, see [Call SPL Script in Java](http://c.raqsoft.com/article/1544066331124).

  For other integrations, see [Call SPL in applications](http://c.raqsoft.com/article/1638429720790).


## SPL application scenarios

- __Got SQL__

    __SQL has certain computing power, but it is not available in many scenarios, so you will have to hard code in Java. 
    SPL provides lightweight computing power independent of database and can process data in any scenario:__
    
    - Structured text (txt/csv) calculation &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1630909963011" title="SPL: Reading and Writing Structured Text Files">[1]</a>*
    
    
    - Excel calculation &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1632902735334" title="SPL: Reading and Writing Excel Files">[1]</a>*
    
    - Perform SQL on files &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1603680860113" title="SQL Query over File Examples">[1]</a>*
    
    - Multi-layer json calculation &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1637121581613" title="How to Parse and Compute JSON in Java?">[1]</a> <a href="http://c.raqsoft.com/article/1634550595176" title="SPL: Reading and Writing JSON Data">[2]</a>*
    
    - Multi-layer xml calculation &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1637121972080" title="How to Parse and Compute XML in Java?">[1]</a> <a href="http://c.raqsoft.com/article/1634615560629" title="SPL: Reading and Writing XML Data">[2]</a>*
    
    - Java computing class library, surpass Stream/Kotlin/Scala
    
    - Replace ORM to implement business logic
    
    - SQL-like calculation on Mongodb, association calculation
    
    - Post calculation of WebService/Restful &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1637830939009" title="How Java Handles Data Returned from WebService/RESTful">[1]</a> <a href="http://c.raqsoft.com/article/1636534963649" title="SPL: Access to HTTP/WebService/Restful service">[2]</a>*
    
    - Post calculation of Salesforce, Post calculation of SAP
    
    - Post calculation of various data sources: HBase,Cassandra,Redis,ElasticSearch,Kafka,…

- __Beyond SQL__

    __SQL is difficult to deal with complex sets and ordered operations, and it is often read out and calculated in Java.
    SPL has complete set capability, especially supports ordered and step-by-step calculation, which can simplify these operations:__

    - Ordered set &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1621587675540" title="Comparison of SQL & SPL: Order-based Computations">[1]</a>*

    - Position reference
    
    - Grouping subsets
    
    - Non-equivalence grouping
    
    - Multi-level association operation
    
    - Static and dynamic pivot  &nbsp;&nbsp;&nbsp;&nbsp; *Ref. <a href="http://c.raqsoft.com/article/1621588427124" title="Comparison of SQL & SPL: Static Transposition">[1]</a> <a href="http://c.raqsoft.com/article/1621588222144" title="Comparison of SQL & SPL: Complicated Static Transposition">[2]</a> <a href="http://c.raqsoft.com/article/1625729514671" title="Comparison of SQL & SPL: Dynamic Transposition">[3]</a>*
    
    - Recursion and iteration
    
    - Step-by-step and loop operation
    
    - Text and date time operation

- __Cooperate DB__

    __The computing power of the database is closed and cannot process data outside the database. It is often necessary to perform ETL to import data into the same       database before processing.__
    
    __SPL provides open and simple computing power, which can directly read multiple databases, realize mixed data calculation, and assist the database to do           better calculation.__

    - Fetch data in parallel to accelerate JDBC
    
    - SQL migration among different types of databases
    
    - Cross database operations
    
    - T+0 statistics and query
    
    - Replace stored procedure operation, improve code portability and reduce coupling
    
    - Avoid making ETL into ELT or even LET
    
    - Mixed calculation of multiple data sources
    
    - Reduce intermediate tables in the database
    
    - Report data source development, support hot switching, multiple data sources and improve development efficiency
    
    - Implement microservices, occupy less resources and support hot switching


- __Surpass DB__

    __SQL is difficult to implement high-performance algorithms. The performance of big data operations can only rely on the optimization engine of the database,       but it is often unreliable in complex situations.__
    
    __SPL provides a large number of basic high-performance algorithms (many of which are pioneered in the industry) and efficient storage formats. Under the same     hardware environment, it can obtain much better computing performance than the database, and can comprehensively replace the big data platform and data             warehouse.__

    - In-memory search：binary search, sequence number positioning, position index, hash index, multi-layer sequence number positioning

    - Dataset in external storage：parallel computing of text file, binary storage, double increment segmentation, columnar 
    storage composite table, ordered storage and update

    - Search in external storage：binary search, hash index, sorting index, row-based storage and valued index, index preloading, batch search and set search, multi index merging, full-text searching

    - Traversing technique：post filter of cursor, multi-purpose traversal, parallel traversing and multi cursors, aggregation extension, ordered traversing, program cursor, partially ordered grouping and sorting, sequence number grouping and controllable segmentation

    - Association technique： foreign key addressing, foreign key serialization, index reuse, alignment sequence, large dimension table search, unilateral splitting, orderly merging, association positioning, schedule

    - Multidimensional analysis：pre summary and time period pre summary, alignment sequence, tag bit dimension 

    - Distributed：free computing and data distribution, cluster multi-zone composite table, cluster dimension table, redundant fault tolerance, spare tire fault tolerance, Fork-Reduce, multi job load balancing

- __For Excel__



- __For Industry__

    __There are a large number of time series data in industrial scenarios, and databases often only provide SQL. The ordered calculation capability of SQL is very     weak, resulting in that it can only be used for data retrieval and cannot assist in calculation.__

    __Many basic mathematical operations are often involved in industrial scenarios. SQL lacks these functions and the data can only be read out to process.__

    __SPL can well support ordered calculation, and provides rich mathematical functions, such as matrix and fitting, and can more conveniently meet the               calculation requirements of industrial scenes.__

    - Time series cursor: aggregation by granularity, translation, adjacence reference, association and merging

    - Historical data compression and solidification, transparent reference

    - Vector and matrix operations

    - Various linear fitting: least squares, partial least squares, Lasso, ridge …

    - …

    __Industrial algorithms often need repeated experiments. SPL development efficiency is very high, and you can try more
    within the same time period:__

    - Instrument anomaly discovery algorithm

    - Abnormal measurement sample locating

    - Curve lifting and oscillation pattern recognition

    - Constrained linear fitting

    - Pipeline transmission scheduling algorithm

    - …


## Useful Links


*   [Tutorial](http://doc.raqsoft.com.cn/esproc/tutorial/) esProc download, installation, as well as principles and applications
*   [Function Reference](http://doc.raqsoft.com.cn/esproc/func/) esProc syntax, applications and examples
*   [Sample Program](http://doc.raqsoft.com.cn/esproc/spd/) Guide to all functions under menus in esProc
*   [Code Reference](http://doc.raqsoft.com.cn/esproc/coderefer/ ) esProc grid-style code examples
*   [User Reference](http://doc.raqsoft.com.cn/esproc/manual/) esProc programming by examples
*   [External Library Guide](http://doc.raqsoft.com.cn/esproc/ext/) Deployment of and connection to esProc external libraries

*   esProc Official WebSite: http://www.scudata.com

*   Please head to http://c.raqsoft.com/article/1595817756260 to download esProc executable files

*   More detail materials can be found at http://c.raqsoft.com

## License

esProc is under the Apache 2.0 license. See the [LICENSE](./LICENSE) file for details.
