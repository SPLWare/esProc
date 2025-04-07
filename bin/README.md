## The application encapsulates esProc IDE designer, ODBC service, HTTP service and the command line interface. Below lists sh files for starting these services:

To start esProc IDE：startup.sh.

To launch esProc server service: ServerConsole.sh.

To start ODBC service on a non-GUI interface: ServerConsole.sh -o.

To start HTTP service on a non-GUI interface: ServerConsole.sh -h.

To start ODBC service and HTTP service on a non-GUI interface: ServerConsole.sh -a.

To close ODBC service and HTTP service on a non-GUI interface: ServerConsole.sh -x.

Command line execution script: esprocx.sh.

To start sample database: startDataBase.sh.

To create a custom installation package, compile the source code as a jar file and put the file in a loadable directory, such as /lib; then make the startup file according to sh/bat file listed here. You can also download the installation package through [Zip installation package] in [Download esProc SPL](https://c.scudata.com/article/1595817756260) page.

Start the program directly through the startup class when source code development mode is used. Here are the commonly used startup classes:

#### Start IDE designer

Startup class: com.scudata.ide.spl.SPL

In SPL.java window, click [Run] to start the designer interface:
 ![image](https://www.esproc.com/wp-content/themes/scudata-en/github_esproc_img/bin-readme1.png)


#### Use the command line tool

Startup class: com.scudata.ide.spl.Esprocx

Configure [Program arguments] and [VM arguments] under [Debug Configurations]. Find more rules about command lines in [Tutorial - Command Line](https://doc.esproc.com/esproc/tutorial/minglinghang.html).
 ![image](https://www.esproc.com/wp-content/themes/scudata-en/github_esproc_img/bin-readme2.png) 

#### Start HTTP/ODBC service

Startup class: com.scudata.ide.spl.ServerConsole

Configure [VM arguments] under [Debug Configurations]:
  ![image](https://www.esproc.com/wp-content/themes/scudata-en/github_esproc_img/bin-readme3.png)
Click [Debug] to pop up the HTTP/ODBC service interface.

Configure options in [Program arguments] - [Program arguments] to start the non-GUI service:

-o, for starting non-GUI ODBC service;

-h, for starting non-GUI HTTP service;

-a, for starting non-GUI ODBC and HTTP services;

-x, for closing non-GUI ODBC and HTTP services.

Note: To start esProc in a non-GUI environment, add -Djava.awt.headless=true in ServerConsole.sh.