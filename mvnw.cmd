@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.3.2

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties

@IF NOT EXIST "%MAVEN_WRAPPER_PROPERTIES%" (
    @ECHO ERROR: %MAVEN_WRAPPER_PROPERTIES% not found
    @EXIT /B 1
)

@FOR /F "tokens=2 delims==" %%A IN ('FINDSTR /B "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') DO @SET "DISTRIBUTION_URL=%%A"

@SET "MAVEN_USER_HOME=%USERPROFILE%\.m2"
@IF NOT "%MAVEN_USER_HOME%"=="" SET "MAVEN_USER_HOME=%MAVEN_USER_HOME%"

@FOR %%A IN ("%DISTRIBUTION_URL%") DO @SET "DIST_FILENAME=%%~nxA"
@SET "DIST_NAME=%DIST_FILENAME:-bin.zip=%"
@SET "DIST_DIR=%MAVEN_USER_HOME%\wrapper\dists\%DIST_NAME%"
@SET "MAVEN_HOME=%DIST_DIR%\%DIST_NAME%"

@IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    @IF NOT EXIST "%DIST_DIR%" MKDIR "%DIST_DIR%"
    @SET "DIST_ZIP=%DIST_DIR%\%DIST_FILENAME%"
    @IF NOT EXIST "%DIST_ZIP%" (
        @ECHO Downloading %DISTRIBUTION_URL%
        @powershell -Command "(New-Object Net.WebClient).DownloadFile('%DISTRIBUTION_URL%','%DIST_ZIP%')"
    )
    @ECHO Unzipping %DIST_ZIP%
    @powershell -Command "Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%DIST_DIR%'"
)

@"%MAVEN_HOME%\bin\mvn.cmd" %*
