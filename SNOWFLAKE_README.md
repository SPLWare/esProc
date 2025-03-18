# Snowflake Container Service Setup

## Role Setup

This section sets up a new role named `test_role` in Snowflake. Roles are used to manage permissions and control access to different resources.

```sql
USE ROLE ACCOUNTADMIN;

CREATE ROLE test_role;
```

## Database and Warehouse Setup

In this section, we create a new database (`tutorial_db`) and a virtual warehouse (`tutorial_warehouse`). The warehouse is used to run SQL queries. Ownership of the database is granted to `test_role`, and usage permissions for the warehouse are also granted to `test_role`.

```sql
CREATE DATABASE IF NOT EXISTS tutorial_db;
GRANT OWNERSHIP ON DATABASE tutorial_db TO ROLE test_role COPY CURRENT GRANTS;

CREATE OR REPLACE WAREHOUSE tutorial_warehouse WITH
WAREHOUSE_SIZE='X-SMALL';
GRANT USAGE ON WAREHOUSE tutorial_warehouse TO ROLE test_role;
```

## Service Endpoint and Compute Pool Setup

This section grants permission to bind service endpoints to the `test_role` and creates a compute pool (`tutorial_compute_pool`). A compute pool is used to manage resources for scaling and processing. The `test_role` is granted usage and monitoring permissions for the compute pool.

```sql
GRANT BIND SERVICE ENDPOINT ON ACCOUNT TO ROLE test_role;

CREATE COMPUTE POOL tutorial_compute_pool
MIN_NODES = 1
MAX_NODES = 1
INSTANCE_FAMILY = CPU_X64_XS;
GRANT USAGE, MONITOR ON COMPUTE POOL tutorial_compute_pool TO ROLE test_role;
```

## Role Assignment

This section assigns the `test_role` to the user `user`. This allows the user to use the permissions granted to `test_role`. You should change it to the user you would like to assign the test_role to.

```sql
GRANT ROLE test_role TO USER user;
```

## Setting Active Role, Database, and Warehouse

This section sets the active role, database, and warehouse for the current session. This is necessary to ensure that subsequent commands are executed with the correct context.

```sql
USE ROLE test_role;
USE DATABASE tutorial_db;
USE WAREHOUSE tutorial_warehouse;
```

## Schema, Repository, and Stage Setup

In this section, a schema (`data_schema`) is created within the database. The schema helps organize database objects. Additionally, an image repository (`tutorial_repository`) and a stage (`tutorial_stage`) are created. The stage is used to manage external data files, and the repository is for managing container images.

```sql
CREATE SCHEMA IF NOT EXISTS data_schema;
USE SCHEMA data_schema;
CREATE IMAGE REPOSITORY IF NOT EXISTS tutorial_repository;
CREATE STAGE IF NOT EXISTS tutorial_stage
DIRECTORY = ( ENABLE = true );
```

## Setting Up the Environment for Snowflake SPL Service

This guide will walk you through installing the required tools, building and uploading the project image, and creating the compute pool and SPL service in Snowflake.

### 1. Install JDK 1.8

The project requires **JDK 1.8**. Please download it from [Oracle's website](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html) and set the appropriate `JAVA_HOME` environment variable.

To verify that JDK is installed correctly, run:
```bash
java -version
```
Make sure the output shows the correct version (Java 1.8).

### 2. Install Maven

You also need to install **Maven**. On macOS, it can be easily installed via [Homebrew](https://brew.sh/):
```bash
brew install maven
```
To verify Maven installation, run:
```bash
mvn -version
```
Ensure that the version output matches your requirements.

### 3. Build

You need to build the code using the following command. Note that the build may fail at the last step, however, all the necessary asset will be generated. 
```
mvn clean package -Prelease 
```

### 4. Upload the Docker Image

We provide a script to **build** the project and **upload** the resulting Docker image to a repository managed by Snowflake. You can customize the repository location if needed.

Before running the script, set your Snowflake account as an environment variable:
```bash
export SNOWFLAKE_ACCOUNT=your-snowflake-account
```
Then run the build script:
```bash
./push_to_snowflake.sh
```
This script will build the Docker image and upload it to the Snowflake-managed Docker repository.

### 5. Create the Compute Pool and SPL Service in Snowflake

To create the SPL service in Snowflake, run the following SQL command in your Snowflake workbook. This command will create a service that provides endpoints to execute SPL files saved in the specified path.
```sql
CREATE SERVICE spl_service
  IN COMPUTE POOL tutorial_compute_pool
  FROM SPECIFICATION $$
    spec:
      container:
        - name: main
          image: /tutorial_db/data_schema/tutorial_repository/spl_service:latest
          env:
            SNOWFLAKE_WAREHOUSE: tutorial_warehouse
          volumeMounts:
            - name: data
              mountPath: /opt/data
      endpoints:
        - name: spl
          port: 8502
          public: true
      volumes:
        - name: data
          source: "@tutorial_stage"
      $$
   MIN_INSTANCES=1
   MAX_INSTANCES=1;
```

The service will provide endpoints to allow users to execute SPL files located in the main path, which is backed by the stage. As long as the SPL file is in the stage, it can be executed via REST APIs.

To **check the status** of the service or **retrieve logs**, use the following SQL commands:
```sql
SELECT SYSTEM$GET_SERVICE_STATUS('spl_service');
SELECT SYSTEM$GET_SERVICE_LOGS('spl_service', '0', 'main');
```

### 6. Execute SPL Using REST API

To execute SPL via REST API, first get the service endpoint:
```sql
SHOW ENDPOINTS IN SERVICE spl_service;
```

From the result, copy the `ingress_url` and paste it into your web browser (e.g., Chrome). You can then invoke the SPL using a REST API request like this:
```bash
https://your-ingress-url.snowflakecomputing.app/file_test.splx()
```
Replace `your-ingress-url` with the actual URL obtained from the `SHOW ENDPOINTS` command.

This assumes that both the `file_test.splx` and any relevant data are in the `mainPath` backed by the stage.

### 7. Upload Data and SPL Files to Snowflake Stage

To upload data or SPL files into the Snowflake stage, use **SnowSQL** (see [SnowSQL documentation](https://docs.snowflake.com/en/user-guide/snowsql)). For example, to upload the `employee.btx` file:
```sql
PUT file:////path-to-esproc/mainPath/employee.btx @tutorial_stage
AUTO_COMPRESS=FALSE
OVERWRITE=TRUE;
```
Change `path-to-esproc` to the actual path on your machine. This command will place the file into the `tutorial_stage`, making it accessible for SPL execution.

### Summary

In this guide, you've set up your environment, built the project Docker image, created a Snowflake SPL service, and learned how to execute SPL files. Make sure to verify each step, ensuring everything is correctly configured for smooth operation.
