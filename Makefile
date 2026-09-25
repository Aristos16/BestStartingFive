.PHONY: all build run clean clean-src

build: clean-src
	cmd /c "if not exist ..\output mkdir ..\output"
	javac -encoding UTF-8 -d ../output *.java

run:
	java -cp ../output LauncherGUI

clean-src:
	cmd /c "del /s /q *.class 2>nul || exit /b 0"

clean:
	cmd /c "if exist ..\output rmdir /s /q ..\output"
	cmd /c "del /s /q *.class 2>nul || exit /b 0"

all: clean build run