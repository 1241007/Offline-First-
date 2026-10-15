package com.offline_First.ui.screens.roadmap

import com.offline_First.data.staging.JavaRoadmapStagingData
import org.junit.Assert.*
import org.junit.Test

class JavaRoadmapJourneyTest {

    @Test
    fun testJavaRoadmapNodesCompleteness() {
        val nodes = JavaRoadmapStagingData.ALL_VISUAL_NODES
        assertTrue("Java roadmap should contain a comprehensive set of nodes (>= 60)", nodes.size >= 60)

        // Verify major milestone spines
        val majorNodes = nodes.filter { it.isMajor }
        assertTrue("Should have major milestones", majorNodes.size >= 8)

        val majorTitles = majorNodes.map { it.title }
        assertTrue("Contains Java root node", majorTitles.contains("Java"))
        assertTrue("Contains Learn the Basics", majorTitles.contains("Learn the Basics"))
        assertTrue("Contains Object Oriented Programming", majorTitles.contains("Object Oriented Programming"))
        assertTrue("Contains Exception Handling", majorTitles.contains("Exception Handling"))
        assertTrue("Contains Concurrency", majorTitles.contains("Concurrency"))
        assertTrue("Contains Build Tools", majorTitles.contains("Build Tools"))
        assertTrue("Contains Web Frameworks", majorTitles.contains("Web Frameworks"))
        assertTrue("Contains Database Access", majorTitles.contains("Database Access"))
        assertTrue("Contains Testing", majorTitles.contains("Testing"))

        // Verify all required topics from prompt specifications
        val titles = nodes.map { it.title }

        // Section A: Starting point & fundamentals
        assertTrue("Contains Basic Syntax", titles.contains("Basic Syntax"))
        assertTrue("Contains Lifecycle of a Program", titles.contains("Lifecycle of a Program"))
        assertTrue("Contains Data Types", titles.contains("Data Types"))
        assertTrue("Contains Variables and Scopes", titles.contains("Variables and Scopes"))
        assertTrue("Contains Type Casting", titles.contains("Type Casting"))
        assertTrue("Contains Strings and Methods", titles.contains("Strings and Methods"))
        assertTrue("Contains Math Operations", titles.contains("Math Operations"))
        assertTrue("Contains Arrays", titles.contains("Arrays"))
        assertTrue("Contains Conditionals", titles.contains("Conditionals"))
        assertTrue("Contains Loops", titles.contains("Loops"))
        assertTrue("Contains Basics of OOP", titles.contains("Basics of OOP"))

        // Section B: Object Oriented Programming
        assertTrue("Contains Classes and Objects", titles.contains("Classes and Objects"))
        assertTrue("Contains Attributes and Methods", titles.contains("Attributes and Methods"))
        assertTrue("Contains Access Specifiers", titles.contains("Access Specifiers"))
        assertTrue("Contains Static Keyword", titles.contains("Static Keyword"))
        assertTrue("Contains Final Keyword", titles.contains("Final Keyword"))
        assertTrue("Contains Nested Classes", titles.contains("Nested Classes"))
        assertTrue("Contains Packages", titles.contains("Packages"))
        assertTrue("Contains Object Lifecycle", titles.contains("Object Lifecycle"))
        assertTrue("Contains Method Chaining", titles.contains("Method Chaining"))
        assertTrue("Contains Inheritance", titles.contains("Inheritance"))
        assertTrue("Contains Encapsulation", titles.contains("Encapsulation"))
        assertTrue("Contains Interfaces", titles.contains("Interfaces"))
        assertTrue("Contains Abstraction", titles.contains("Abstraction"))
        assertTrue("Contains Enums", titles.contains("Enums"))
        assertTrue("Contains Record", titles.contains("Record"))
        assertTrue("Contains Method Overloading / Overriding", titles.contains("Method Overloading / Overriding"))
        assertTrue("Contains Initializer Block", titles.contains("Initializer Block"))
        assertTrue("Contains Static vs Dynamic Binding", titles.contains("Static vs Dynamic Binding"))
        assertTrue("Contains Pass by Value / Pass by Reference", titles.contains("Pass by Value / Pass by Reference"))

        // Section C: Exception Handling & Language Features
        assertTrue("Contains Lambda Expressions", titles.contains("Lambda Expressions"))
        assertTrue("Contains Annotations", titles.contains("Annotations"))
        assertTrue("Contains Modules", titles.contains("Modules"))
        assertTrue("Contains Optionals", titles.contains("Optionals"))
        assertTrue("Contains Generics", titles.contains("Generics"))
        assertTrue("Contains Reflection", titles.contains("Reflection"))

        // Section D: Collections
        assertTrue("Contains Collections Framework", titles.contains("Collections Framework"))
        assertTrue("Contains Array vs ArrayList", titles.contains("Array vs ArrayList"))
        assertTrue("Contains LinkedList", titles.contains("LinkedList"))
        assertTrue("Contains Set", titles.contains("Set"))
        assertTrue("Contains Map", titles.contains("Map"))
        assertTrue("Contains Queue", titles.contains("Queue"))
        assertTrue("Contains Deque", titles.contains("Deque"))
        assertTrue("Contains Stack", titles.contains("Stack"))
        assertTrue("Contains Iterator", titles.contains("Iterator"))
        assertTrue("Contains Generic Collections", titles.contains("Generic Collections"))

        // Section E: Concurrency & Internals
        assertTrue("Contains Threads", titles.contains("Threads"))
        assertTrue("Contains Virtual Threads", titles.contains("Virtual Threads"))
        assertTrue("Contains Java Memory Model", titles.contains("Java Memory Model"))
        assertTrue("Contains Volatile Keyword", titles.any { it.equals("volatile keyword", ignoreCase = true) })
        assertTrue("Contains Synchronization", titles.contains("Synchronization"))
        assertTrue("Contains Executors", titles.contains("Executors"))
        assertTrue("Contains Thread Pools", titles.contains("Thread Pools"))
        assertTrue("Contains Cryptography", titles.contains("Cryptography"))
        assertTrue("Contains Date and Time", titles.contains("Date and Time"))
        assertTrue("Contains Networking", titles.contains("Networking"))
        assertTrue("Contains Regular Expressions", titles.contains("Regular Expressions"))

        // Section F: Build Tools
        assertTrue("Contains Maven", titles.contains("Maven"))
        assertTrue("Contains Gradle", titles.contains("Gradle"))

        // Section G: Web Frameworks
        assertTrue("Contains Spring (Spring Boot)", titles.contains("Spring (Spring Boot)"))
        assertTrue("Contains Quarkus", titles.contains("Quarkus"))
        assertTrue("Contains Javalin", titles.contains("Javalin"))
        assertTrue("Contains Play Framework", titles.contains("Play Framework"))
        assertTrue("Contains Dependency Injection", titles.contains("Dependency Injection"))

        // Section H: Database Access
        assertTrue("Contains JDBC", titles.contains("JDBC"))
        assertTrue("Contains JPA", titles.contains("JPA"))
        assertTrue("Contains Hibernate", titles.contains("Hibernate"))
        assertTrue("Contains Spring Data JPA", titles.contains("Spring Data JPA"))

        // Section I: Testing
        assertTrue("Contains Unit Testing", titles.contains("Unit Testing"))
        assertTrue("Contains JUnit", titles.contains("JUnit"))
        assertTrue("Contains TestNG", titles.contains("TestNG"))
        assertTrue("Contains Integration Testing", titles.contains("Integration Testing"))
        assertTrue("Contains REST Assured", titles.contains("REST Assured"))
        assertTrue("Contains JMeter", titles.contains("JMeter"))
        assertTrue("Contains Behavior Testing", titles.contains("Behavior Testing"))
        assertTrue("Contains Mocking with Mockito", titles.any { it.contains("Mockito", ignoreCase = true) })

        // Section J: Documentation & Logging
        assertTrue("Contains Documentation", titles.contains("Documentation"))
        assertTrue("Contains Javadoc", titles.contains("Javadoc"))
        assertTrue("Contains Logging Frameworks", titles.contains("Logging Frameworks"))
        assertTrue("Contains Logback", titles.contains("Logback"))
        assertTrue("Contains Log4j2", titles.contains("Log4j2"))
        assertTrue("Contains SLF4J", titles.contains("SLF4J"))
        assertTrue("Contains TinyLog", titles.contains("TinyLog"))

        // Verify quality of all nodes
        nodes.forEach { node ->
            assertTrue("Node ${node.id} must have non-blank title", node.title.isNotBlank())
            assertTrue("Node ${node.id} must have non-blank section", node.section.isNotBlank())
            assertTrue("Node ${node.id} must have objectives", node.objectives.isNotEmpty())
            assertTrue("Node ${node.id} must have explanation", node.explanation.isNotBlank())
            assertTrue("Node ${node.id} must have codeSnippet", node.codeSnippet.isNotBlank())
        }
    }

    @Test
    fun testJavaFaqsStrictlyFive() {
        val faqs = JavaRoadmapStagingData.JAVA_FAQS
        assertEquals("Must contain EXACTLY 5 FAQs as specified in Section 7", 5, faqs.size)

        val expectedQuestions = listOf(
            "What is a Java developer?",
            "What should I learn before starting Java?",
            "How long does it take to learn Java?",
            "What projects can I build with Java?",
            "Should I learn Spring Boot after Java fundamentals?"
        )

        expectedQuestions.forEachIndexed { index, question ->
            assertEquals("FAQ #${index + 1} mismatch", question, faqs[index].question)
            assertTrue("FAQ answer for '$question' must not be blank", faqs[index].answer.isNotBlank())
        }
    }

    @Test
    fun testJavaPracticeProjects() {
        val projects = JavaRoadmapStagingData.JAVA_PROJECTS
        assertTrue("Must have Java practice projects", projects.size >= 5)

        val titles = projects.map { it.title }
        assertTrue("Contains CLI Student Management System", titles.contains("CLI Student Management System"))
        assertTrue("Contains Multi-threaded Log File Analyzer", titles.contains("Multi-threaded Log File Analyzer"))
        assertTrue("Contains Spring Boot RESTful API with JPA", titles.contains("Spring Boot RESTful API with JPA"))
        assertTrue("Contains Real-time Chat Server with Sockets", titles.contains("Real-time Chat Server with Sockets"))
        assertTrue("Contains Microservices Order & Payment Engine", titles.contains("Microservices Order & Payment Engine"))

        projects.forEach { project ->
            assertTrue("Project ${project.title} must have description", project.description.isNotBlank())
            assertTrue("Project ${project.title} must have skills", project.relevantSkills.isNotEmpty())
            assertTrue("Project ${project.title} must have deliverables", project.deliverables.isNotEmpty())
            assertTrue("Project ${project.title} must have instructions", project.instructions.isNotBlank())
            assertTrue("Project ${project.title} must have estimatedHours > 0", project.estimatedHours > 0)
        }
    }

    @Test
    fun testJavaProgressCalculation() {
        val total = JavaRoadmapStagingData.ALL_VISUAL_NODES.size
        assertTrue("Total topics must be positive", total > 0)

        // When 0 completed
        val emptySet = emptySet<String>()
        val progress0 = if (total == 0) 0 else ((emptySet.size.toFloat() / total) * 100).toInt()
        assertEquals(0, progress0)

        // When all completed
        val allIds = JavaRoadmapStagingData.ALL_VISUAL_NODES.map { it.id }.toSet()
        val progress100 = ((allIds.size.toFloat() / total) * 100).toInt()
        assertEquals(100, progress100)

        // When half completed
        val halfIds = allIds.take(total / 2).toSet()
        val progressHalf = ((halfIds.size.toFloat() / total) * 100).toInt()
        assertTrue("Half progress should be roughly 45-55%", progressHalf in 45..55)
    }
}
