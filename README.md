section-sort
============

Sorting serial sections using a TSP-solver

## Dependencies

[concorde][concorde] (free for academic use)
[concorde]: http://www.math.uwaterloo.ca/tsp/concorde.html

[cplex][cplex] (free with academic initiative)
[cplex]: http://www-03.ibm.com/software/products/en/ibmilogcpleoptistud

[tsplib format][tsplib] (file format for feeding concorde with tsps)
[tsplib]: http://comopt.ifi.uni-heidelberg.de/software/TSPLIB95

## Installation (Linux)

Download cplex 12.5 and install into `<cplex-install-dir>`. 
```
$ chmod +x cplex_studio1251.linux-x86-64.bin
$ ./cplex_studio1251.linux-x86-64.bin
```
Note that building concorde will not work with cplex 12.6. Download latest [concorde source][concorde-source] and extract into `<concorde-src-dir>`. 
[concorde-source]: http://www.math.uwaterloo.ca/tsp/concorde/downloads/codes/src/co031219.tgz
```
$ mkdir -p <concorde-src-dir> && cd <concorde-src-dir>
$ tar xf ~/Downloads/co031219.tgz
```
Create <concorde-build-dir>, link cplex libraries and headers into <concorde-build-dir> and run the concorde build configuration.
```
$ mkdir -p <concorde-build-dir> && cd <concorde-build-dir>
$ ln -s <cplex-install-dir>/cplex/include/ilcplex/*.h .
$ ln -s <cplex-install-dir>/cplex/lib/x86-64_sles10_4.1/static_pic/*.a .
$ <concorde-src-dir>/configure --prefix=$PWD --with-cplex=$PWD
```
Modify
```
<concorde-build-dir>/Makefile
<concorde-build-dir>/TSP/Makefile
```
and replace
```
LIBFLAGS = -liberty -lm 
```
by
```
LIBFLAGS = -liberty -lm -pthread
```
in each file.

Run
```
<concorde-build-dir> && make
```
The TSP solver is now located at `<concorde-build-dir>/TSP/concorde`.

Copy/link the TSP solver into the bin directory of your Fiji distribution.
```
cp <concorde-build-dir>/TSP/concorde <fiji-root>/bin
```

Build section-sort and copy the jar into the jar directory of your Fiji distribution.
```
mvn clean install -Dimagej.app.directory=<fiji-root>/ -Ddelete.other.versions=true
```

Copy section_sort.bsh into the plugins directory of your Fiji distribution.
```
cp <section-sort>/src/main/bsh/tsp/section_sort.bsh <fiji-root>/plugins
```
The section sort plugin will then appear in the Plugins drop down menu after restarting Fiji or
refreshing the beanshell scripts (`Plugins -> Scripting -> Refresh BSH Scripts`).
