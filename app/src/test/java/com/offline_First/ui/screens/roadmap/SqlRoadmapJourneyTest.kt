package com.offline_First.ui.screens.roadmap

import com.offline_First.data.staging.PythonRoadmapStagingData
import com.offline_First.data.staging.SqlRoadmapStagingData
import org.junit.Assert.*
import org.junit.Test

class SqlRoadmapJourneyTest {

    @Test
    fun testSqlRoadmapNodesCompleteness() {
        val nodes = SqlRoadmapStagingData.ALL_VISUAL_NODES
        assertTrue("SQL roadmap should contain a rich set of nodes (>= 50)", nodes.size >= 50)

        // Verify major milestone spine
        val majorNodes = nodes.filter { it.isMajor }
        assertTrue("Should have major milestones (>= 8)", majorNodes.size >= 8)

        val majorTitles = majorNodes.map { it.title }
        assertTrue("Contains SQL root node", majorTitles.contains("SQL"))
        assertTrue("Contains Learn the Basics", majorTitles.contains("Learn the Basics"))
        assertTrue("Contains Basic SQL Syntax", majorTitles.contains("Basic SQL Syntax"))
        assertTrue("Contains Data Definition Language (DDL)", majorTitles.contains("Data Definition Language (DDL)"))
        assertTrue("Contains Data Manipulation Language (DML)", majorTitles.contains("Data Manipulation Language (DML)"))
        assertTrue("Contains Aggregate Queries", majorTitles.contains("Aggregate Queries"))
        assertTrue("Contains Data Constraints", majorTitles.contains("Data Constraints"))
        assertTrue("Contains Subqueries", majorTitles.contains("Subqueries"))
        assertTrue("Contains JOIN Queries", majorTitles.contains("JOIN Queries"))
        assertTrue("Contains Advanced Functions", majorTitles.contains("Advanced Functions"))
        assertTrue("Contains Views", majorTitles.contains("Views"))
        assertTrue("Contains Indexes", majorTitles.contains("Indexes"))
        assertTrue("Contains Stored Procedures & Functions", majorTitles.contains("Stored Procedures & Functions"))
        assertTrue("Contains Data Integrity & Security", majorTitles.contains("Data Integrity & Security"))
        assertTrue("Contains Transactions", majorTitles.contains("Transactions"))
        assertTrue("Contains Performance Optimization", majorTitles.contains("Performance Optimization"))
        assertTrue("Contains Advanced SQL", majorTitles.contains("Advanced SQL"))

        val titles = nodes.map { it.title }

        // Section A: Fundamentals & Syntax
        assertTrue("Contains What are Relational Databases?", titles.contains("What are Relational Databases?"))
        assertTrue("Contains RDBMS Benefits and Limitations", titles.contains("RDBMS Benefits and Limitations"))
        assertTrue("Contains SQL vs NoSQL Databases", titles.contains("SQL vs NoSQL Databases"))
        assertTrue("Contains SQL Keywords", titles.contains("SQL Keywords"))
        assertTrue("Contains Data Types", titles.contains("Data Types"))
        assertTrue("Contains Operators", titles.contains("Operators"))
        assertTrue("Contains SELECT", titles.contains("SELECT"))
        assertTrue("Contains INSERT", titles.contains("INSERT"))
        assertTrue("Contains UPDATE", titles.contains("UPDATE"))
        assertTrue("Contains DELETE", titles.contains("DELETE"))

        // Section B: DDL & DML
        assertTrue("Contains Create Table", titles.contains("Create Table"))
        assertTrue("Contains Alter Table", titles.contains("Alter Table"))
        assertTrue("Contains Drop Table", titles.contains("Drop Table"))
        assertTrue("Contains Truncate Table", titles.contains("Truncate Table"))

        // Section C: Constraints & Joins
        assertTrue("Contains Primary Key", titles.contains("Primary Key"))
        assertTrue("Contains Foreign Key", titles.contains("Foreign Key"))
        assertTrue("Contains Unique", titles.contains("Unique"))
        assertTrue("Contains NOT NULL", titles.contains("NOT NULL"))
        assertTrue("Contains CHECK", titles.contains("CHECK"))
        assertTrue("Contains INNER JOIN", titles.contains("INNER JOIN"))
        assertTrue("Contains LEFT JOIN", titles.contains("LEFT JOIN"))
        assertTrue("Contains RIGHT JOIN", titles.contains("RIGHT JOIN"))
        assertTrue("Contains FULL OUTER JOIN", titles.contains("FULL OUTER JOIN"))
        assertTrue("Contains Self Join", titles.contains("Self Join"))
        assertTrue("Contains Cross Join", titles.contains("Cross Join"))

        // Section D: Advanced Functions, Views, Transactions & Optimization
        assertTrue("Contains String Functions", titles.contains("String Functions"))
        assertTrue("Contains Numeric Functions", titles.contains("Numeric Functions"))
        assertTrue("Contains Date and Time", titles.contains("Date and Time"))
        assertTrue("Contains COALESCE", titles.contains("COALESCE"))
        assertTrue("Contains Indexes", titles.contains("Indexes"))
        assertTrue("Contains Transactions", titles.contains("Transactions"))
        assertTrue("Contains Data Integrity & Security", titles.contains("Data Integrity & Security"))
        assertTrue("Contains Stored Procedures & Functions", titles.contains("Stored Procedures & Functions"))
        assertTrue("Contains Performance Optimization", titles.contains("Performance Optimization"))
        assertTrue("Contains Advanced SQL", titles.contains("Advanced SQL"))
    }

    @Test
    fun testSqlFaqsCountAndContent() {
        val faqs = SqlRoadmapStagingData.SQL_FAQS
        assertEquals("Should have exactly 5 FAQs", 5, faqs.size)

        val questions = faqs.map { it.question }
        assertTrue("Contains 'What is SQL?'", questions.contains("What is SQL?"))
        assertTrue("Contains 'What is the difference between SQL and NoSQL?'", questions.contains("What is the difference between SQL and NoSQL?"))
        assertTrue("Contains 'What is the difference between WHERE and HAVING?'", questions.contains("What is the difference between WHERE and HAVING?"))
        assertTrue("Contains 'When should I use an INDEX in SQL?'", questions.contains("When should I use an INDEX in SQL?"))
        assertTrue("Contains 'What are Window Functions?'", questions.contains("What are Window Functions?"))

        faqs.forEach { faq ->
            assertTrue("FAQ answer must not be blank for ${faq.question}", faq.answer.isNotBlank())
        }
    }

    @Test
    fun testSqlProjectsRequirements() {
        val projects = SqlRoadmapStagingData.SQL_PROJECTS
        assertEquals("Should have exactly 4 projects", 4, projects.size)

        val titles = projects.map { it.title }
        assertTrue("Contains E-Commerce Database Schema", titles.contains("E-Commerce Database Schema"))
        assertTrue("Contains Sales Analytics & Aggregation Engine", titles.contains("Sales Analytics & Aggregation Engine"))
        assertTrue("Contains Financial Transaction Audit with Window Functions", titles.contains("Financial Transaction Audit with Window Functions"))
        assertTrue("Contains Query Optimizer & Index Benchmarking", titles.contains("Query Optimizer & Index Benchmarking"))

        projects.forEach { project ->
            assertTrue("Project ${project.title} must have description", project.description.isNotBlank())
            assertTrue("Project ${project.title} must have skills", project.relevantSkills.isNotEmpty())
            assertTrue("Project ${project.title} must have deliverables", project.deliverables.isNotEmpty())
            assertTrue("Project ${project.title} must have instructions", project.instructions.isNotBlank())
        }
    }

    @Test
    fun testSqlProgressCalculation() {
        val allNodes = SqlRoadmapStagingData.ALL_VISUAL_NODES
        val totalCount = allNodes.size

        // Empty completion
        val initialProgress = 0 / totalCount.toFloat()
        assertEquals(0f, initialProgress, 0.01f)

        // 10 completed topics
        val completed10 = allNodes.take(10).map { it.id }.toSet()
        val progress10 = (completed10.size.toFloat() / totalCount).coerceIn(0f, 1f)
        assertTrue("Progress should be between 0.15 and 0.25", progress10 in 0.15f..0.25f)

        // All completed
        val allCompleted = allNodes.map { it.id }.toSet()
        val fullProgress = (allCompleted.size.toFloat() / totalCount).coerceIn(0f, 1f)
        assertEquals(1f, fullProgress, 0.01f)
    }

    @Test
    fun testSqlRoadmapIsMarkedImplementedInCatalog() {
        val sqlPath = PythonRoadmapStagingData.LEARNING_PATHS.first { it.id == "sql-databases" }
        assertTrue("SQL path must be marked as implemented", sqlPath.isImplemented)
    }
}
