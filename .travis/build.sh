#!/bin/sh
# Currently no way of supplying travis with the required concorde executable, thus only do a build check and skip tests!
mvn install -DskipTests=true
