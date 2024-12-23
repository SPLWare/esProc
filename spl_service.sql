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

ALTER SERVICE spl_service
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
