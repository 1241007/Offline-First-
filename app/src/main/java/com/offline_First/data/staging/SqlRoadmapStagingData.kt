package com.offline_First.data.staging

/**
 * Complete Staging Data for EduNova SQL Roadmap.
 * Strictly aligned with Roadmap.sh SQL structure (Screenshot 2)
 * and unified with EduNova's Java styling.
 */
object SqlRoadmapStagingData {

    // --- FREQUENTLY ASKED QUESTIONS ---
    val SQL_FAQS = listOf(
        RoadmapFaqItem(
            id = "sql_faq_1",
            question = "What is SQL?",
            answer = "SQL (Structured Query Language) is the standard declarative programming language used to manage, query, manipulate, and define relational databases like PostgreSQL, MySQL, SQLite, and SQL Server."
        ),
        RoadmapFaqItem(
            id = "sql_faq_2",
            question = "What is the difference between SQL and NoSQL?",
            answer = "SQL databases are relational, table-based, enforce strict schemas, and prioritize ACID compliance for data integrity. NoSQL databases (document, key-value, graph) offer flexible dynamic schemas designed for horizontal scale and high-velocity semi-structured data."
        ),
        RoadmapFaqItem(
            id = "sql_faq_3",
            question = "What is the difference between WHERE and HAVING?",
            answer = "WHERE filters individual rows BEFORE aggregate functions are calculated. HAVING filters grouped rows AFTER aggregate grouping (GROUP BY) has been executed."
        ),
        RoadmapFaqItem(
            id = "sql_faq_4",
            question = "When should I use an INDEX in SQL?",
            answer = "Indexes (like B-Tree) speed up SELECT queries involving WHERE filters, JOIN conditions, and ORDER BY clauses. However, they add overhead to INSERT, UPDATE, and DELETE operations, so index columns selectively."
        ),
        RoadmapFaqItem(
            id = "sql_faq_5",
            question = "What are Window Functions?",
            answer = "Window functions (such as ROW_NUMBER, RANK, DENSE_RANK, LEAD, LAG) perform calculations across a set of rows related to the current row without collapsing the rows into a single summary like GROUP BY does."
        )
    )

    // --- PRACTICE PROJECTS ---
    val SQL_PROJECTS = listOf(
        RoadmapProjectItem(
            id = "sql_proj_1",
            title = "E-Commerce Database Schema",
            difficulty = "Beginner",
            estimatedHours = 10,
            description = "Design and build a normalized 3NF relational database schema for an e-commerce platform with customers, orders, products, and order_items.",
            relevantSkills = listOf("DDL", "Primary & Foreign Keys", "Constraints", "Normalization", "SQLite / PostgreSQL"),
            deliverables = listOf(
                "Entity Relationship Diagram (ERD)",
                "Complete DDL scripts creating tables with CHECK and FOREIGN KEY constraints",
                "Seed dataset inserting sample customers, orders, and items",
                "Verification query validating foreign key cascade integrity"
            ),
            instructions = "1. Write CREATE TABLE scripts. 2. Enforce NOT NULL and UNIQUE email. 3. Insert 20 sample rows. 4. Test referential integrity."
        ),
        RoadmapProjectItem(
            id = "sql_proj_2",
            title = "Sales Analytics & Aggregation Engine",
            difficulty = "Intermediate",
            estimatedHours = 14,
            description = "Write complex analytical SQL queries joining multiple tables to report monthly revenue, customer retention cohorts, and top-selling categories.",
            relevantSkills = listOf("JOINs", "GROUP BY", "HAVING", "CASE WHEN", "Aggregate Functions"),
            deliverables = listOf(
                "Monthly revenue aggregate report by product department",
                "Top 10 highest lifetime value (LTV) customer rankings",
                "Inventory threshold alerting query using HAVING count < 5",
                "Customer segmentation query using CASE WHEN spending brackets"
            ),
            instructions = "1. Join orders and line items. 2. Group by order month and category. 3. Filter using HAVING and conditional aggregates."
        ),
        RoadmapProjectItem(
            id = "sql_proj_3",
            title = "Financial Transaction Audit with Window Functions",
            difficulty = "Advanced",
            estimatedHours = 20,
            description = "Develop audit trail queries calculating running bank balances, month-over-month growth, and detecting rapid consecutive withdrawals.",
            relevantSkills = listOf("Window Functions", "OVER / PARTITION BY", "LEAD / LAG", "Common Table Expressions (CTEs)", "Transactions"),
            deliverables = listOf(
                "Running balance calculation using SUM(amount) OVER (PARTITION BY account ORDER BY date)",
                "Fraud detection query identifying withdrawals within 60 seconds using LAG()",
                "Recursive CTE modeling organizational management hierarchy",
                "Transaction block demonstrating ACID rollback on overdraft"
            ),
            instructions = "1. Use CTEs for data preparation. 2. Partition by account ID with LEAD/LAG. 3. Execute atomic transaction with ROLLBACK test."
        ),
        RoadmapProjectItem(
            id = "sql_proj_4",
            title = "Query Optimizer & Index Benchmarking",
            difficulty = "Advanced",
            estimatedHours = 22,
            description = "Analyze slow query execution plans using EXPLAIN ANALYZE, design composite B-Tree indexes, and eliminate costly full table scans.",
            relevantSkills = listOf("EXPLAIN ANALYZE", "B-Tree Indexes", "Query Optimization", "Execution Plans", "Database Tuning"),
            deliverables = listOf(
                "Benchmark report comparing query latency with and without indexes on 1M rows",
                "Composite index on (status, created_at) optimizing filter & sort",
                "Refactored non-SARGable WHERE clause into index-friendly predicate",
                "Partial index reducing index storage footprint by 75%"
            ),
            instructions = "1. Generate 1 million mock records. 2. Run EXPLAIN ANALYZE. 3. Add composite index. 4. Verify Index Scan replaces Seq Scan."
        )
    )

    // --- VISUAL NODES FOR SQL ROADMAP ---
    // Milestone 1: SQL Start & Basics
    val NODE_ROOT = VisualRoadmapNode(
        id = "sql_root",
        title = "SQL",
        section = "Start",
        isMajor = true,
        badge = "Start",
        explanation = "Structured Query Language: The industry standard for relational database management.",
        objectives = listOf("Understand relational model", "Master database querying and data modeling"),
        codeSnippet = "-- Welcome to SQL\nSELECT 'Welcome to SQL Roadmap' AS message;"
    )

    val NODE_BASICS = VisualRoadmapNode(
        id = "sql_basics",
        title = "Learn the Basics",
        section = "SQL Fundamentals",
        isMajor = true,
        badge = "Milestone 1",
        explanation = "Core concepts of relational databases, table structures, and comparing SQL with NoSQL engines.",
        objectives = listOf("Understand relations, rows, and columns", "Learn benefits and limits of RDBMS"),
        codeSnippet = "-- Relational Tables & Records\nSELECT * FROM information_schema.tables;"
    )

    val NODE_WHAT_ARE_RDBMS = VisualRoadmapNode(id = "sql_what_are_rdbms", title = "What are Relational Databases?", section = "SQL Fundamentals", explanation = "Databases organized into formal tables connected by primary and foreign keys.", objectives = listOf("Tabular structure", "Relations"), codeSnippet = "-- Tables consist of schema-defined columns and row tuples")
    val NODE_RDBMS_BENEFITS = VisualRoadmapNode(id = "sql_rdbms_benefits", title = "RDBMS Benefits and Limitations", section = "SQL Fundamentals", explanation = "Strong consistency, ACID guarantees, schema validation, vs horizontal sharding limits.", objectives = listOf("Data integrity", "Scaling considerations"), codeSnippet = "-- ACID: Atomicity, Consistency, Isolation, Durability")
    val NODE_SQL_VS_NOSQL = VisualRoadmapNode(id = "sql_vs_nosql", title = "SQL vs NoSQL Databases", section = "SQL Fundamentals", explanation = "Structured relational tables vs unstructured document/key-value storage.", objectives = listOf("Schema rigidity vs flexibility", "CAP theorem"), codeSnippet = "-- SQL: Structured & normalized\n-- NoSQL: Denormalized & distributed")

    // Milestone 2: Syntax & Statements
    val NODE_BASIC_SYNTAX = VisualRoadmapNode(
        id = "sql_basic_syntax",
        title = "Basic SQL Syntax",
        section = "Syntax & Statements",
        isMajor = true,
        badge = "Milestone 2",
        explanation = "Writing standard SQL queries, keywords, data types, and filtering expressions.",
        objectives = listOf("Keywords (SELECT, FROM, WHERE)", "Data types (VARCHAR, INT, DATE)", "Comparison operators"),
        codeSnippet = "SELECT first_name, email FROM users WHERE is_active = TRUE;"
    )

    val NODE_KEYWORDS = VisualRoadmapNode(id = "sql_keywords", title = "SQL Keywords", section = "Syntax & Statements", explanation = "Reserved terms like SELECT, FROM, WHERE, GROUP BY, ORDER BY, HAVING.", objectives = listOf("Clauses execution order"), codeSnippet = "SELECT, FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT")
    val NODE_DATA_TYPES = VisualRoadmapNode(id = "sql_data_types", title = "Data Types", section = "Syntax & Statements", explanation = "INT, BIGINT, VARCHAR, TEXT, BOOLEAN, DECIMAL, TIMESTAMP, JSON.", objectives = listOf("Storage precision", "Temporal vs numeric"), codeSnippet = "CREATE TABLE example (id INT, price DECIMAL(10,2), created_at TIMESTAMP);")
    val NODE_OPERATORS = VisualRoadmapNode(id = "sql_operators", title = "Operators", section = "Syntax & Statements", explanation = "Comparison (=, !=, <, >), Logical (AND, OR, NOT), and Pattern (LIKE, IN, BETWEEN).", objectives = listOf("Pattern matching LIKE '%foo%'", "Range checks BETWEEN"), codeSnippet = "WHERE price BETWEEN 10 AND 50 AND category IN ('Books', 'Tech');")
    val NODE_SELECT = VisualRoadmapNode(id = "sql_select", title = "SELECT", section = "Syntax & Statements", explanation = "Retrieve specific columns or all fields from tables.", objectives = listOf("Projection", "DISTINCT clause"), codeSnippet = "SELECT DISTINCT department FROM employees;")
    val NODE_INSERT = VisualRoadmapNode(id = "sql_insert", title = "INSERT", section = "Syntax & Statements", explanation = "Insert new rows of data into a relational table.", objectives = listOf("Single & bulk insert"), codeSnippet = "INSERT INTO users (name, email) VALUES ('Alice', 'alice@edunova.com');")
    val NODE_UPDATE = VisualRoadmapNode(id = "sql_update", title = "UPDATE", section = "Syntax & Statements", explanation = "Modify existing row values matching conditional criteria.", objectives = listOf("Targeted WHERE clause", "Batch updates"), codeSnippet = "UPDATE accounts SET balance = balance + 100 WHERE id = 42;")
    val NODE_DELETE = VisualRoadmapNode(id = "sql_delete", title = "DELETE", section = "Syntax & Statements", explanation = "Remove specific rows from a table.", objectives = listOf("Safe deletion with WHERE", "TRUNCATE difference"), codeSnippet = "DELETE FROM sessions WHERE expired_at < NOW();")

    // Milestone 3: DDL
    val NODE_DDL = VisualRoadmapNode(
        id = "sql_ddl",
        title = "Data Definition Language (DDL)",
        section = "Data Definition",
        isMajor = true,
        badge = "Milestone 3",
        explanation = "Commands that define and modify database schema structures and objects.",
        objectives = listOf("CREATE, ALTER, DROP, TRUNCATE tables"),
        codeSnippet = "CREATE TABLE products (\n    id SERIAL PRIMARY KEY,\n    name VARCHAR(100) NOT NULL\n);"
    )

    val NODE_CREATE_TABLE = VisualRoadmapNode(id = "sql_create_table", title = "Create Table", section = "Data Definition", explanation = "Define new table schema with column constraints.", objectives = listOf("Column definitions", "Defaults"), codeSnippet = "CREATE TABLE courses (id INT PRIMARY KEY, title TEXT);")
    val NODE_ALTER_TABLE = VisualRoadmapNode(id = "sql_alter_table", title = "Alter Table", section = "Data Definition", explanation = "Add, drop, or modify columns on an existing table.", objectives = listOf("ADD COLUMN", "MODIFY COLUMN"), codeSnippet = "ALTER TABLE users ADD COLUMN phone VARCHAR(20);")
    val NODE_DROP_TABLE = VisualRoadmapNode(id = "sql_drop_table", title = "Drop Table", section = "Data Definition", explanation = "Completely remove a table and all its metadata.", objectives = listOf("CASCADE drop"), codeSnippet = "DROP TABLE IF EXISTS old_logs CASCADE;")
    val NODE_TRUNCATE_TABLE = VisualRoadmapNode(id = "sql_truncate_table", title = "Truncate Table", section = "Data Definition", explanation = "Fast wipe of all table rows without logging individual row deletes.", objectives = listOf("Resetting auto-increment", "Speed vs DELETE"), codeSnippet = "TRUNCATE TABLE audit_cache;")

    // Milestone 4: DML & SELECT Queries
    val NODE_DML = VisualRoadmapNode(
        id = "sql_dml",
        title = "Data Manipulation Language (DML)",
        section = "Data Manipulation",
        isMajor = true,
        badge = "Milestone 4",
        explanation = "Querying and mutating table data rows using SQL DML statements.",
        objectives = listOf("Master SELECT clauses", "Multi-table joins", "Grouping and filtering"),
        codeSnippet = "SELECT dept, COUNT(*) FROM emp WHERE active = 1 GROUP BY dept HAVING COUNT(*) > 5;"
    )

    // Milestone 5: Aggregate Queries
    val NODE_AGGREGATES = VisualRoadmapNode(
        id = "sql_aggregates",
        title = "Aggregate Queries",
        section = "Aggregations",
        isMajor = true,
        badge = "Milestone 5",
        explanation = "Summarizing multiple row values into single scalar metrics.",
        objectives = listOf("SUM, COUNT, AVG, MIN, MAX", "GROUP BY grouping", "HAVING post-aggregation filters"),
        codeSnippet = "SELECT category, AVG(price) FROM products GROUP BY category;"
    )

    val NODE_SUM = VisualRoadmapNode(id = "sql_sum", title = "SUM", section = "Aggregations", explanation = "Total sum of numeric column values.", objectives = listOf("Total revenue"), codeSnippet = "SELECT SUM(amount) FROM payments;")
    val NODE_COUNT = VisualRoadmapNode(id = "sql_count", title = "COUNT", section = "Aggregations", explanation = "Count of non-null records or rows.", objectives = listOf("COUNT(*)", "COUNT(DISTINCT)"), codeSnippet = "SELECT COUNT(DISTINCT user_id) FROM visits;")
    val NODE_AVG = VisualRoadmapNode(id = "sql_avg", title = "AVG", section = "Aggregations", explanation = "Arithmetic mean of numeric values.", objectives = listOf("Mean calculations"), codeSnippet = "SELECT AVG(grade) FROM exams;")
    val NODE_MIN_MAX = VisualRoadmapNode(id = "sql_min_max", title = "MIN / MAX", section = "Aggregations", explanation = "Smallest and largest values in a column.", objectives = listOf("Range boundaries"), codeSnippet = "SELECT MIN(salary), MAX(salary) FROM employees;")
    val NODE_GROUP_BY = VisualRoadmapNode(id = "sql_group_by", title = "GROUP BY", section = "Aggregations", explanation = "Splits rows into summary subsets based on column values.", objectives = listOf("Grouping columns"), codeSnippet = "GROUP BY department, role")
    val NODE_HAVING = VisualRoadmapNode(id = "sql_having", title = "HAVING", section = "Aggregations", explanation = "Filters aggregated groups after GROUP BY evaluation.", objectives = listOf("Group condition"), codeSnippet = "HAVING SUM(sales) > 100000")

    // Milestone 6: Constraints
    val NODE_CONSTRAINTS = VisualRoadmapNode(
        id = "sql_constraints",
        title = "Data Constraints",
        section = "Integrity & Keys",
        isMajor = true,
        badge = "Milestone 6",
        explanation = "Rules enforced on data columns to ensure relational integrity and accuracy.",
        objectives = listOf("PRIMARY KEY", "FOREIGN KEY", "UNIQUE", "NOT NULL", "CHECK"),
        codeSnippet = "ALTER TABLE users ADD CONSTRAINT check_age CHECK (age >= 18);"
    )

    val NODE_PRIMARY_KEY = VisualRoadmapNode(id = "sql_primary_key", title = "Primary Key", section = "Integrity & Keys", explanation = "Uniquely identifies each record; non-null and indexed by default.", objectives = listOf("Entity identity", "Clustered index"), codeSnippet = "id INT PRIMARY KEY")
    val NODE_FOREIGN_KEY = VisualRoadmapNode(id = "sql_foreign_key", title = "Foreign Key", section = "Integrity & Keys", explanation = "Enforces referential relationship pointing to a primary key in another table.", objectives = listOf("Referential integrity", "ON DELETE CASCADE"), codeSnippet = "FOREIGN KEY (user_id) REFERENCES users(id)")
    val NODE_UNIQUE = VisualRoadmapNode(id = "sql_unique", title = "Unique", section = "Integrity & Keys", explanation = "Guarantees all values in a column are distinct.", objectives = listOf("Duplicate prevention"), codeSnippet = "email VARCHAR(255) UNIQUE")
    val NODE_NOT_NULL = VisualRoadmapNode(id = "sql_not_null", title = "NOT NULL", section = "Integrity & Keys", explanation = "Prohibits missing or null values in this column.", objectives = listOf("Mandatory attributes"), codeSnippet = "username VARCHAR(50) NOT NULL")
    val NODE_CHECK = VisualRoadmapNode(id = "sql_check", title = "CHECK", section = "Integrity & Keys", explanation = "Ensures values satisfy custom boolean conditions.", objectives = listOf("Range validations"), codeSnippet = "CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))")

    // Milestone 7: Subqueries
    val NODE_SUBQUERIES = VisualRoadmapNode(
        id = "sql_subqueries",
        title = "Subqueries",
        section = "Subqueries",
        isMajor = true,
        badge = "Milestone 7",
        explanation = "Queries nested inside another SELECT, INSERT, UPDATE, or DELETE statement.",
        objectives = listOf("Scalar subqueries", "Correlated subqueries", "EXISTS and IN clauses"),
        codeSnippet = "SELECT name FROM emp WHERE salary > (SELECT AVG(salary) FROM emp);"
    )

    val NODE_SCALAR_SUBQUERY = VisualRoadmapNode(id = "sql_scalar_subquery", title = "Scalar, Column, Row, Table", section = "Subqueries", explanation = "Different subquery shapes returning single values, single columns, or full tables.", objectives = listOf("Subquery dimensionality"), codeSnippet = "WHERE id IN (SELECT id FROM active_users)")
    val NODE_NESTED_SUBQUERY = VisualRoadmapNode(id = "sql_nested_subquery", title = "Nested Subqueries", section = "Subqueries", explanation = "Subqueries nested multiple levels deep inside WHERE or FROM clauses.", objectives = listOf("Derived tables"), codeSnippet = "SELECT * FROM (SELECT * FROM a) AS b")
    val NODE_CORRELATED_SUBQUERY = VisualRoadmapNode(id = "sql_correlated_subquery", title = "Correlated Subqueries", section = "Subqueries", explanation = "Subquery that references column values from the outer query row-by-row.", objectives = listOf("Row-by-row re-evaluation", "EXISTS predicate"), codeSnippet = "WHERE EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)")

    // Milestone 8: JOINs
    val NODE_JOINS = VisualRoadmapNode(
        id = "sql_joins",
        title = "JOIN Queries",
        section = "Joins",
        isMajor = true,
        badge = "Milestone 8",
        explanation = "Combining rows from two or more tables based on a related column between them.",
        objectives = listOf("INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL OUTER JOIN", "Self Join"),
        codeSnippet = "SELECT u.name, o.id FROM users u INNER JOIN orders o ON u.id = o.user_id;"
    )

    val NODE_INNER_JOIN = VisualRoadmapNode(id = "sql_inner_join", title = "INNER JOIN", section = "Joins", explanation = "Returns rows that have matching values in both tables.", objectives = listOf("Intersection match"), codeSnippet = "FROM A INNER JOIN B ON A.id = B.a_id")
    val NODE_LEFT_JOIN = VisualRoadmapNode(id = "sql_left_join", title = "LEFT JOIN", section = "Joins", explanation = "Returns all rows from left table, and matched rows from right table (or NULL).", objectives = listOf("Preserving left records"), codeSnippet = "FROM users u LEFT JOIN orders o ON u.id = o.user_id")
    val NODE_RIGHT_JOIN = VisualRoadmapNode(id = "sql_right_join", title = "RIGHT JOIN", section = "Joins", explanation = "Returns all rows from right table, and matched rows from left table.", objectives = listOf("Preserving right records"), codeSnippet = "FROM A RIGHT JOIN B ON A.id = B.a_id")
    val NODE_FULL_OUTER_JOIN = VisualRoadmapNode(id = "sql_full_outer_join", title = "FULL OUTER JOIN", section = "Joins", explanation = "Returns all rows when there is a match in either left or right table.", objectives = listOf("Full union with nulls"), codeSnippet = "FROM A FULL OUTER JOIN B ON A.id = B.a_id")
    val NODE_SELF_JOIN = VisualRoadmapNode(id = "sql_self_join", title = "Self Join", section = "Joins", explanation = "Joining a table to itself, commonly used for hierarchical structures.", objectives = listOf("Manager-employee hierarchy"), codeSnippet = "FROM employees e JOIN employees m ON e.manager_id = m.id")
    val NODE_CROSS_JOIN = VisualRoadmapNode(id = "sql_cross_join", title = "Cross Join", section = "Joins", explanation = "Cartesian product producing all possible combinations of rows.", objectives = listOf("N x M combinations"), codeSnippet = "FROM sizes CROSS JOIN colors")

    // Milestone 9: Advanced Functions
    val NODE_ADV_FUNCTIONS = VisualRoadmapNode(
        id = "sql_adv_functions",
        title = "Advanced Functions",
        section = "Built-in Functions",
        isMajor = true,
        badge = "Milestone 9",
        explanation = "String manipulation, date/time math, numerical operators, and conditional logic.",
        objectives = listOf("String functions", "Date & Time arithmetic", "COALESCE & CASE WHEN"),
        codeSnippet = "SELECT CONCAT(first_name, ' ', last_name), COALESCE(nickname, 'N/A') FROM users;"
    )

    val NODE_STRING_FUNCS = VisualRoadmapNode(id = "sql_string_funcs", title = "String Functions", section = "Built-in Functions", explanation = "CONCAT, LENGTH, SUBSTRING, REPLACE, UPPER, LOWER, TRIM.", objectives = listOf("Text manipulation"), codeSnippet = "CONCAT(UPPER(SUBSTRING(name, 1, 1)), LOWER(SUBSTRING(name, 2)))")
    val NODE_DATETIME_FUNCS = VisualRoadmapNode(id = "sql_datetime_funcs", title = "Date and Time", section = "Built-in Functions", explanation = "DATE, TIME, TIMESTAMP, DATEPART, DATEADD, NOW, INTERVAL.", objectives = listOf("Timezone calculations", "Date math"), codeSnippet = "WHERE created_at >= NOW() - INTERVAL '7 days'")
    val NODE_NUMERIC_FUNCS = VisualRoadmapNode(id = "sql_numeric_funcs", title = "Numeric Functions", section = "Built-in Functions", explanation = "FLOOR, ABS, MOD, ROUND, CEILING, POWER.", objectives = listOf("Mathematical functions"), codeSnippet = "ROUND(price * 1.08, 2)")
    val NODE_COALESCE = VisualRoadmapNode(id = "sql_coalesce", title = "COALESCE", section = "Built-in Functions", explanation = "Returns the first non-null argument in a given list.", objectives = listOf("Null fallback defaults"), codeSnippet = "SELECT COALESCE(phone, email, 'No contact')")

    // Milestone 10: Views & Indexes
    val NODE_VIEWS = VisualRoadmapNode(
        id = "sql_views",
        title = "Views",
        section = "Database Objects",
        isMajor = true,
        badge = "Milestone 10",
        explanation = "Virtual tables defined by an underlying query providing security abstraction and clean modular queries.",
        objectives = listOf("CREATE VIEW", "Materialized Views", "Security encapsulation"),
        codeSnippet = "CREATE VIEW active_users_view AS\nSELECT id, name, email FROM users WHERE is_active = TRUE;"
    )

    val NODE_INDEXES = VisualRoadmapNode(
        id = "sql_indexes",
        title = "Indexes",
        section = "Database Objects",
        isMajor = true,
        badge = "Milestone 11",
        explanation = "Data structures (e.g. B-Tree) that accelerate query lookup speeds on specified columns.",
        objectives = listOf("B-Tree index", "Composite indexes", "Index selectivity"),
        codeSnippet = "CREATE INDEX idx_orders_customer_date ON orders (customer_id, order_date DESC);"
    )

    // Milestone 11: Transactions & Security
    val NODE_TRANSACTIONS = VisualRoadmapNode(
        id = "sql_transactions",
        title = "Transactions",
        section = "Transactions & Security",
        isMajor = true,
        badge = "Milestone 12",
        explanation = "ACID-compliant atomic blocks of operations guaranteed to all succeed or all roll back.",
        objectives = listOf("BEGIN, COMMIT, ROLLBACK", "Isolation levels", "Deadlock prevention"),
        codeSnippet = "BEGIN;\nUPDATE accounts SET bal = bal - 50 WHERE id = 1;\nUPDATE accounts SET bal = bal + 50 WHERE id = 2;\nCOMMIT;"
    )

    val NODE_DATA_INTEGRITY_SECURITY = VisualRoadmapNode(
        id = "sql_data_integrity_sec",
        title = "Data Integrity & Security",
        section = "Transactions & Security",
        isMajor = true,
        explanation = "GRANT and REVOKE permissions, role-based access, and row-level security.",
        objectives = listOf("GRANT / REVOKE", "Role-based access", "SQL injection defense"),
        codeSnippet = "GRANT SELECT, INSERT ON courses TO app_user;\nREVOKE DELETE ON courses FROM app_user;"
    )

    val NODE_STORED_PROCS = VisualRoadmapNode(
        id = "sql_stored_procs",
        title = "Stored Procedures & Functions",
        section = "Programmability",
        isMajor = true,
        explanation = "Precompiled SQL routines executed directly on the database engine server.",
        objectives = listOf("Custom functions", "Stored procedures", "Parameters and returns"),
        codeSnippet = "CREATE OR REPLACE FUNCTION get_user_total(uid INT) RETURNS DECIMAL AS $$ ... $$ LANGUAGE plpgsql;"
    )

    // Milestone 12: Optimization & Advanced SQL
    val NODE_PERFORMANCE_OPT = VisualRoadmapNode(
        id = "sql_performance_opt",
        title = "Performance Optimization",
        section = "Performance & Advanced",
        isMajor = true,
        badge = "Milestone 13",
        explanation = "Techniques to optimize SQL query execution time and server memory utilization.",
        objectives = listOf("EXPLAIN ANALYZE", "Query plans", "Index scans vs Seq scans", "Selective projections"),
        codeSnippet = "EXPLAIN ANALYZE SELECT * FROM orders WHERE status = 'PENDING';"
    )

    val NODE_ADVANCED_SQL = VisualRoadmapNode(
        id = "sql_advanced",
        title = "Advanced SQL",
        section = "Performance & Advanced",
        isMajor = true,
        badge = "Milestone 14",
        explanation = "Window functions, Common Table Expressions (CTEs), recursive queries, and dynamic SQL.",
        objectives = listOf("Window functions (ROW_NUMBER, RANK, LEAD, LAG)", "Common Table Expressions (WITH)", "Recursive CTEs"),
        codeSnippet = "WITH RankedOrders AS (\n    SELECT id, amount, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY amount DESC) as rnk\n    FROM orders\n)\nSELECT * FROM RankedOrders WHERE rnk <= 3;"
    )

    val NODE_WINDOW_FUNCTIONS = VisualRoadmapNode(id = "sql_window_funcs", title = "Window Functions", section = "Performance & Advanced", explanation = "ROW_NUMBER, RANK, DENSE_RANK, LEAD, LAG, FIRST_VALUE over partitioned windows.", objectives = listOf("OVER (PARTITION BY ... ORDER BY ...)"), codeSnippet = "SELECT name, dept, RANK() OVER (PARTITION BY dept ORDER BY salary DESC) FROM emp;")
    val NODE_RECURSIVE_QUERIES = VisualRoadmapNode(id = "sql_recursive_queries", title = "Recursive Queries", section = "Performance & Advanced", explanation = "WITH RECURSIVE queries traversing graphs, trees, and hierarchical menus.", objectives = listOf("Base case + recursive member"), codeSnippet = "WITH RECURSIVE org_tree AS (...)")
    val NODE_PIVOT_UNPIVOT = VisualRoadmapNode(id = "sql_pivot_unpivot", title = "Pivot / Unpivot Operations", section = "Performance & Advanced", explanation = "Rotating rows into columns (pivot) and columns into rows (unpivot).", objectives = listOf("Cross-tabulation", "FILTER clause"), codeSnippet = "SELECT dept, SUM(sales) FILTER (WHERE year = 2025) AS sales_2025")
    val NODE_CTES = VisualRoadmapNode(id = "sql_ctes", title = "Common Table Expressions", section = "Performance & Advanced", explanation = "WITH temporary named result sets improving readability and modular query structuring.", objectives = listOf("WITH clause", "Modular query composition"), codeSnippet = "WITH HighSpenders AS (\n    SELECT user_id FROM orders GROUP BY user_id HAVING SUM(total) > 1000\n)\nSELECT * FROM users WHERE id IN (SELECT user_id FROM HighSpenders);")

    // All Visual Nodes List for SQL Roadmap
    val ALL_VISUAL_NODES: List<VisualRoadmapNode> = listOf(
        NODE_ROOT, NODE_BASICS, NODE_WHAT_ARE_RDBMS, NODE_RDBMS_BENEFITS, NODE_SQL_VS_NOSQL,
        NODE_BASIC_SYNTAX, NODE_KEYWORDS, NODE_DATA_TYPES, NODE_OPERATORS,
        NODE_SELECT, NODE_INSERT, NODE_UPDATE, NODE_DELETE,
        NODE_DDL, NODE_CREATE_TABLE, NODE_ALTER_TABLE, NODE_DROP_TABLE, NODE_TRUNCATE_TABLE,
        NODE_DML, NODE_AGGREGATES, NODE_SUM, NODE_COUNT, NODE_AVG, NODE_MIN_MAX, NODE_GROUP_BY, NODE_HAVING,
        NODE_CONSTRAINTS, NODE_PRIMARY_KEY, NODE_FOREIGN_KEY, NODE_UNIQUE, NODE_NOT_NULL, NODE_CHECK,
        NODE_SUBQUERIES, NODE_SCALAR_SUBQUERY, NODE_NESTED_SUBQUERY, NODE_CORRELATED_SUBQUERY,
        NODE_JOINS, NODE_INNER_JOIN, NODE_LEFT_JOIN, NODE_RIGHT_JOIN, NODE_FULL_OUTER_JOIN, NODE_SELF_JOIN, NODE_CROSS_JOIN,
        NODE_ADV_FUNCTIONS, NODE_STRING_FUNCS, NODE_DATETIME_FUNCS, NODE_NUMERIC_FUNCS, NODE_COALESCE,
        NODE_VIEWS, NODE_INDEXES, NODE_TRANSACTIONS, NODE_DATA_INTEGRITY_SECURITY, NODE_STORED_PROCS,
        NODE_PERFORMANCE_OPT, NODE_ADVANCED_SQL, NODE_WINDOW_FUNCTIONS, NODE_RECURSIVE_QUERIES, NODE_PIVOT_UNPIVOT, NODE_CTES
    )
}
