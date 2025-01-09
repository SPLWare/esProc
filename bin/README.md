## The application encapsulates esProc IDE designer, node service, ODBC service, HTTP service and the command line interface. Below lists sh files for starting these services:

To start esProc IDE：startup.sh.

To launch esProc server service: ServerConsole.sh.

To start the node service on a non-GUI interface: ServerConsole.sh -p.

To start ODBC service on a non-GUI interface: ServerConsole.sh -o.

To start HTTP service on a non-GUI interface: ServerConsole.sh -h.

To start node service, ODBC service and HTTP service on a non-GUI interface: ServerConsole.sh -a.

To close node service, ODBC service and HTTP service on a non-GUI interface: ServerConsole.sh -x.

Command line execution script: esprocx.sh.

To start sample database: startDataBase.sh.

To create a custom installation package, compile the source code as a jar file and put the file in a loadable directory, such as /lib; then make the startup file according to sh/bat file listed here. You can also download the installation package through [Zip installation package] in [Download esProc SPL](https://c.scudata.com/article/1595817756260) page.

Start the program directly through the startup class when source code development mode is used. Here are the commonly used startup classes:

esProc designer: com.scudata.ide.spl.SPL 

Node server：com.scudata.ide.spl.ServerConsole

Command line：com.scudata.ide.spl.Esprocx 
