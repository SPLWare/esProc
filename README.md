
# esProc - Agile Data Computing Engine

## Know about esProc

- __Beyond the computing power of the database__


    esProc does not rely on the computing power of the database, and has an independent computing engine.
    

- __Open computing system__

    Universal computing can be implemented for any data source.   
    
- __Agile syntax__

    Full combination of discreteness and set-orientation.

    Supper ordered calculation.

    Good at complex operations.

    High performance basic algorithms and storage mechanisms.



## Know about SPL

<b>S</b>tructured <b>P</b>rocess <b>L</b>anguage — <b>SPL</b> is the programming language used by esProc.

- __Combined the common advantages of SQL and Java__

  Adapt to set batch computation (SQL advantage)
  
 
  
  Good at step-by-step procedure computing (Java advantage)

- __Well designed rich library functions and consistency syntax__

  __Easier to master and better performance than Python__
  
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

  <img src="http://www.scudata.com/wp-content/themes/scudata-en/images/agile-computing/agile-computing-6.png" width="600" height="250">

## SPL application scenarios

- __Got SQL__

    __SQL has certain computing power, but it is not available in many scenarios, so you will have to hard code in Java. 
    SPL provides lightweight computing power independent of database and can process data in any scenario:__
    
    - Structured text (txt/csv) calculation
    
    - Excel calculation
    
    - Perform SQL on files
    
    - Multi-layer json calculation
    
    - Multi-layer xml calculation
    
    - Java computing class library, surpass Stream/Kotlin/Scala
    
    - Replace ORM to implement business logic
    
    - SQL-like calculation on Mongodb, association calculation
    
    - Post calculation of WebService/Restful
    
    - Post calculation of Salesforce, Post calculation of SAP
    
    - Post calculation of various data sources: HBase,Cassandra,Redis,ElasticSearch,Kafka,…

- __Beyond SQL__

    __SQL is difficult to deal with complex sets and ordered operations, and it is often read out and calculated in Java.
    SPL has complete set capability, especially supports ordered and step-by-step calculation, which can simplify these operations:__

    - Ordered set

    - Position reference
    
    - Grouping subsets
    
    - Non-equivalence grouping
    
    - Multi-level association operation
    
    - Static and dynamic pivot
    
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









Useful Links
-----------------------------------------------------------------------------------------------------------------------

*   [Tutorial](http://doc.raqsoft.com.cn/esproc/tutorial/) esProc download, installation, as well as principles and applications
*   [Function Reference](http://doc.raqsoft.com.cn/esproc/func/) esProc syntax, applications and examples
*   [Sample Program](http://doc.raqsoft.com.cn/esproc/spd/) Guide to all functions under menus in esProc
*   [Code Reference](http://doc.raqsoft.com.cn/esproc/coderefer/ ) esProc grid-style code examples
*   [User Reference](http://doc.raqsoft.com.cn/esproc/manual/) esProc programming by examples
*   [External Library Guide](http://doc.raqsoft.com.cn/esproc/ext/) Deployment of and connection to esProc external libraries

*   esProc Official WebSite: http://www.scudata.com

*   Please head to http://c.raqsoft.com/article/1595817756260 to download esProc executable files

*   More detail materials can be found at http://c.raqsoft.com
