@echo off
color 0F
title RegionBetter Compiler
echo.
echo ==========================================
echo    RegionBetter Plugin Compiler
echo ==========================================
echo.

echo Cleaning previous build...
call mvn clean

echo.
echo Compiling RegionBetter...
call mvn package

echo.
echo ==========================================
echo    Build Complete!
echo ==========================================
echo.
echo JAR file location: target/RegionBetter-1.0.0.jar
echo.
pause