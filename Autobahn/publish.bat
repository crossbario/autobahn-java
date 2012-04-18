ECHO OFF

ECHO.
ECHO Removing all JARs ..
DEL *.jar

ECHO.
ECHO Building JAR ..
CALL ant jar

ECHO.
ECHO Uploading JAR ..
CALL pscp Autobahn.jar oberstet@www.tavendo.de:/home/oberstet/static/android/Autobahn-0.5.0.jar
