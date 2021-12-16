
## esProc

esProc is the unique name for esProc SPL package. <b>esProc SPL</b> is  an <b>open-source programming language for data processing</b>, which can perform computing independently. For latest package and release notes, see [Download esProc Community Edition Package](http://c.raqsoft.com/article/1595817756260).

SPL focuses on the mainstream embedded and Java application architecture. SPL script is the counterpart of the stored procedure in RDB. A SPL script will be passed to a Java program through JDBC interface to be executed or to achieve the structured computation.

<img src="http://www.raqsoft.com/wp-content/themes/raqsoft2017-en/images/java-computing/3.png" width="800" height="300">




## SPL -  Structured Programming Language

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
    
    - Structured text (txt/csv) calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1616139619037" title="SPL general table operations">[1]</a> <a href="http://c.raqsoft.com/article/1600309450633" title="Samples of Merging and Splitting Files">[2]</a> <a href="http://c.raqsoft.com/article/1600309188122" title="Samples of Comparing Files">[3]</a> <a href="http://c.raqsoft.com/article/1600308846480" title="Sample Programs of Performing Distinct on a File">[4]</a> <a href="Samples of Processing Big Text File" title="Samples of Processing Big Text File">[5]</a>*</sub></sup>
    
    
    - Excel calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1599103426577" title="Sample Programs of Structuralizing Excel Files">[1]</a> <a href="http://c.raqsoft.com/article/1600312426331" title="Samples of Generating Various Excel Files">[2]</a>*</sub></sup>
    
    - Perform SQL on files &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1636624789262" title="Can We Execute SQL on TXT/CSV Files in Java?">[1]</a> <a href="http://c.raqsoft.com/article/1603680860113" title="SQL Query over File Examples">[2]</a>*</sub></sup>
    
    - Multi-layer json calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1637121581613" title="How to Parse and Compute JSON in Java?">[1]</a> <a href="http://c.raqsoft.com/article/1634550595176" title="SPL: Reading and Writing JSON Data">[2]</a>*</sub></sup>
    
    - Multi-layer xml calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1637121972080" title="How to Parse and Compute XML in Java?">[1]</a> <a href="http://c.raqsoft.com/article/1634615560629" title="SPL: Reading and Writing XML Data">[2]</a>*</sub></sup>
    
    - Java computing class library, surpass Stream/Kotlin/Scala &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1625618421728" title="Are You Trying to Replace SQL with Java 8 Stream?">[1]</a> <a href="http://c.raqsoft.com/article/1626426250010" title="Are You Trying to Replace SQL with Kotlin?">[2]</a>*</sub></sup>
    
    - Replace ORM to implement business logic &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1631184524246" title="Is ORM a Convenient Option for Data Migration between Databases?">[1]</a> <a href="http://c.raqsoft.com/article/1636957186884" title="How to Write Universal SQL That Can Be Executed in All Databases?">[2]</a>*</sub></sup>
    
    - SQL-like calculation on Mongodb, association calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1637915238984" title="How Java Executes SQL on MongoDB">[1]</a> <a href="http://c.raqsoft.com/article/1637831059095" title="How Java Queries or Analyzes MongoDB Data">[2]</a> <a href="http://c.raqsoft.com/article/1637914459491" title="How Java Performs JOINs on MongoDB">[3]</a>*</sub></sup>
    
    - Post calculation of WebService/Restful &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1637830939009" title="How Java Handles Data Returned from WebService/RESTful">[1]</a> <a href="http://c.raqsoft.com/article/1636534963649" title="SPL: Access to HTTP/WebService/Restful service">[2]</a>*</sub></sup>
    
    - Post calculation of Salesforce, Post calculation of SAP &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1638173742332" title="How to Achieve More Queries and Analyses on Salesforce Data">[1]</a> <a href="http://c.raqsoft.com/article/1638173973358" title="How to Achieve Complex Logic Queries and Analyses on Data from SAP BW">[2]</a>*</sub></sup>
    
    - Post calculation of various data sources: HBase,Cassandra,Redis,ElasticSearch,Kafka,… &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://doc.raqsoft.com/esproc/ext/" title=" esProc External Library Guide">[1]</a>*</sub></sup>

- __Beyond SQL__

    __SQL is difficult to deal with complex sets and ordered operations, and it is often read out and calculated in Java.
    SPL has complete set capability, especially supports ordered and step-by-step calculation, which can simplify these operations:__

    - Ordered set &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1622598686173" title="Comparison of SQL & SPL: Set-oriented Operations">[1]</a> <a href="http://c.raqsoft.com/article/1621587675540" title="Comparison of SQL & SPL: Order-based Computations">[2]</a> <a href="http://c.raqsoft.com/article/1635932635595" title="SPL: order-related grouping">[3]</a>*</sub></sup>

    - Position reference &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1635933200916" title="SPL: adjacent record reference">[1]</a> <a href="http://c.raqsoft.com/article/1637565000649" title="SPL: Recursively Search Referenced Records">[2]</a> <a href="http://c.raqsoft.com/article/1607498662414" title="Locate Operation on Ordered Sets">[3]</a>*</sub></sup>
    
    - Grouping subsets  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1583482802281" title="Grouped subsets">[1]</a>*</sub></sup>
    
    - Non-equivalence grouping  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1621233528244" title="Comparison of SQL & SPL: Non-equi-grouping">[1]</a>*</sub></sup>
    
    - Multi-level association operation  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1558938121942" title="SPL Simplified SQL - Multilevel Join">[1]</a> <a href="http://c.raqsoft.com/article/1622615623417" title="Comparison of SQL & SPL: Join Operations (Ⅰ)">[2]</a> <a href="http://c.raqsoft.com/article/1623046924829" title="Comparison of SQL & SPL: Join Operations (Ⅱ)">[3]</a> <a href="http://c.raqsoft.com/article/1625738996195" title="Comparison of SQL & SPL: Join Operations (Ⅲ)">[4]</a>*</sub></sup>
    
    - Static and dynamic pivot  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1621588427124" title="Comparison of SQL & SPL: Static Transposition">[1]</a> <a href="http://c.raqsoft.com/article/1621588222144" title="Comparison of SQL & SPL: Complicated Static Transposition">[2]</a> <a href="http://c.raqsoft.com/article/1625729514671" title="Comparison of SQL & SPL: Dynamic Transposition">[3]</a>*</sub></sup>
    
    - Recursion and iteration   &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1621234716519" title="Comparison of SQL & SPL: Recursion Operation">[1]</a>*</sub></sup>
    
    - Step-by-step and loop operation  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1619769733786" title="Loop Computations">[1]</a>*</sub></sup>
    
    - Text and date time operation  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1636542243583" title="SPL: Text Handling">[1]</a> <a href="http://c.raqsoft.com/article/1636538168186" title="SPL: Date, Time and Datetime Handling">[2]</a>*</sub></sup>

- __Cooperate DB__

    __The computing power of the database is closed and cannot process data outside the database. It is often necessary to perform ETL to import data into the same       database before processing.__
    
    __SPL provides open and simple computing power, which can directly read multiple databases, realize mixed data calculation, and assist the database to do           better calculation.__

    - Fetch data in parallel to accelerate JDBC &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1630377767343" title="Just How Slow is Data Retrieval via JDBC">[1]</a>*</sub></sup>
    
    - SQL migration among different types of databases &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1631184524246" title="Is ORM a Convenient Option for Data Migration between Databases?">[1]</a>*</sub></sup>
    
    - Cross database operations &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1619171409712" title="Cross-database Computing Methods">[1]</a>*</sub></sup>
    
    - T+0 statistics and query &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1637566914909" title="How to Achieve T+0 Query and Analysis?">[1]</a>*</sub></sup>
    
    - Replace stored procedure operation, improve code portability and reduce coupling &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1619664923686" title="Drawbacks of Using Stored Procedures to Compute Data">[1]</a>*</sub></sup>
    
    - Avoid making ETL into ELT or even LET
    
    - Mixed calculation of multiple data sources  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1581303780848" title="How to Perform Table Joins between MongoDB and MySQL">[1]</a> <a href="http://c.raqsoft.com/article/1586252047161" title="Perform Join Queries over Different Databases">[2]</a>*</sub></sup>
    
    - Reduce intermediate tables in the database
    
    - Report data source development, support hot switching, multiple data sources and improve development efficiency  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1630640358062" title="Looking for the Best Tool for Using Non-RDB Data Sources in Reporting Tools">[1]</a> <a href="http://c.raqsoft.com/article/1631529039471" title="Looking for the Best Tool for Handling Diverse/Multiple Data Sources for Report Building">[2]</a> <a href="http://c.raqsoft.com/article/1632301657455" title="Looking for the Best Method of Handling Multistep Data Preparation for Reporting Tools">[3]</a>*</sub></sup>
    
    - Implement microservices, occupy less resources and support hot switching &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1634549618946" title="Looking for the Best Technique of Processing Retrieved WebService/RESTful Data">[1]</a> <a href="http://c.raqsoft.com/article/1637567561373" title="How to Achieve Hot-swappable Reports?">[2]</a>*</sub></sup>


- __Surpass DB__

    __SQL is difficult to implement high-performance algorithms. The performance of big data operations can only rely on the optimization engine of the database,       but it is often unreliable in complex situations.__
    
    __SPL provides a large number of basic high-performance algorithms (many of which are pioneered in the industry) and efficient storage formats. Under the same     hardware environment, it can obtain much better computing performance than the database, and can comprehensively replace the big data platform and data             warehouse.__

    - In-memory search：binary search, sequence number positioning, position index, hash index, multi-layer sequence number positioning  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1568871695709" title="Performance optimization - Search">[1]</a>*</sub></sup>


    - Dataset in external storage：parallel computing of text file, binary storage, double increment segmentation, columnar 
    storage composite table, ordered storage and update

    - Search in external storage：binary search, hash index, sorting index, row-based storage and valued index, index preloading, batch search and set search, multi index merging, full-text searching  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1568871695709" title="Performance optimization - Search">[1]</a>*</sub></sup>

    - Traversing technique：post filter of cursor, multi-purpose traversal, parallel traversing and multi cursors, aggregation extension, ordered traversing, program cursor, partially ordered grouping and sorting, sequence number grouping and controllable segmentation  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1568870773966" title="Performance optimization - Traversal">[1]</a>*</sub></sup>


    - Association technique： foreign key addressing, foreign key serialization, index reuse, alignment sequence, large dimension table search, unilateral splitting, orderly merging, association positioning, schedule  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1568950208760" title="Performance optimization - Join">[1]</a>*</sub></sup>

    - Multidimensional analysis：pre summary and time period pre summary, alignment sequence, tag bit dimension  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1567994414051" title="Performance optimization - Multidimensional analysis">[1]</a>*</sub></sup>

    - Distributed：free computing and data distribution, cluster multi-zone composite table, cluster dimension table, redundant fault tolerance, spare tire fault tolerance, Fork-Reduce, multi job load balancing

- __For Excel__
    
    __The combination of SPL and Excel can enhance the calculation ability of Excel and reduce the difficulty of calculation implementation.__  &nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1603680361405" title="Enhance Excel Calculations by Clipboard">[1]</a>*</sub></sup>

    __Through SPL's Excel plug-in, you can use SPL functions in Excel, and you can also call SPL scripts in VBA.__  &nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1616138220586" title="Use esProc add-in in Excel">[1]</a>*</sub></sup>
    
    __SPL provides Excel-oriented set operations:__
    
    - Cell value and summary value calculation &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_4" title="Calculate cell and Summarize value">[1]</a>*</sub></sup>

    - Set operation and subordinate judgment  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_5" title="Set operation and Containing judgment">[1]</a>*</sub></sup>

    - Duplication judgment, count and deduplication &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_6" title="Find duplicates, counting and removal">[1]</a>*</sub></sup>

    - Sorting and ranking  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_7" title="Sorting and ranking">[1]</a>*</sub></sup>

    - Special grouping and aggregate methods  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_8" title="Special grouping and aggregation methods">[1]</a>*</sub></sup>

    - Association and comparison  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_9" title="Association and comparison">[1]</a>*</sub></sup>

    - Row-column transpose  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_10" title="Convert rows and columns">[1]</a>*</sub></sup>

    - Expansion and supplement  &nbsp;&nbsp;&nbsp;&nbsp; <sub><sup>*Ref. <a href="http://c.raqsoft.com/article/1617693922993#toc_h1_11" title="Expansion and Complement">[1]</a>*</sub></sup>





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
