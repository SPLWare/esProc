## The application encapsulates esProc IDE designer,HTTP service and the command line interface. Below lists sh files for starting these services:

To start esProc IDE：startup.sh.

Command line execution script: esprocx.sh.

To create a custom installation package, compile the source code as a jar file and put the file in a loadable directory, such as /lib; then make the startup file according to sh/bat file listed here. You can also download the installation package through [Zip installation package] in [Download esProc SPL](https://c.scudata.com/article/1595817756260) page.

Start the program directly through the startup class when source code development mode is used. Here are the commonly used startup classes:

esProc designer: com.scudata.ide.spl.SPL 

Command line：com.scudata.ide.spl.Esprocx 
