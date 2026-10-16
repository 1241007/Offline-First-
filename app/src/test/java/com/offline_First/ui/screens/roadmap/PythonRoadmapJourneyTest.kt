package com.offline_First.ui.screens.roadmap

import com.offline_First.data.staging.PythonRoadmapStagingData
import org.junit.Assert.*
import org.junit.Test

class PythonRoadmapJourneyTest {

    @Test
    fun testLearningPathsCountAndNames() {
        val paths = PythonRoadmapStagingData.LEARNING_PATHS
        assertEquals("Should have exactly 7 learning paths", 7, paths.size)

        val titles = paths.map { it.title }
        assertTrue("Contains Python Programming", titles.contains("Python Programming"))
        assertTrue("Contains Full-Stack Web Development", titles.contains("Full-Stack Web Development"))
        assertTrue("Contains Machine Learning", titles.contains("Machine Learning"))
        assertTrue("Contains SQL & Database Management", titles.contains("SQL & Database Management"))
        assertTrue("Contains Cloud Computing & DevOps", titles.contains("Cloud Computing & DevOps"))
        assertTrue("Contains Cybersecurity", titles.contains("Cybersecurity"))
        assertTrue("Contains Java Programming", titles.contains("Java Programming"))

        val pythonPath = paths.first { it.id == "python-developer" }
        assertTrue("Python path must be marked as implemented", pythonPath.isImplemented)
    }

    @Test
    fun testPythonModulesStructure() {
        val modules = PythonRoadmapStagingData.PYTHON_MODULES
        assertEquals("Should have exactly 6 modules", 6, modules.size)

        assertEquals("Python Basics & Environment", modules[0].title)
        assertEquals("Data Structures & Collections", modules[1].title)
        assertEquals("Functions & Object-Oriented Programming", modules[2].title)
        assertEquals("Modules & File I/O", modules[3].title)
        assertEquals("Advanced Python Concepts", modules[4].title)
        assertEquals("Web Development & APIs with FastAPI", modules[5].title)

        // Verify each module contains non-empty topics with code snippets and explanations
        modules.forEach { module ->
            assertTrue("Module ${module.title} should have topics", module.topics.isNotEmpty())
            module.topics.forEach { topic ->
                assertTrue("Topic ${topic.title} must have explanation", topic.explanation.isNotBlank())
                assertTrue("Topic ${topic.title} must have code snippet", topic.codeSnippet.isNotBlank())
                assertTrue("Topic ${topic.title} must have objectives", topic.objectives.isNotEmpty())
            }
        }
    }

    @Test
    fun testPracticeProjectsRequirements() {
        val projects = PythonRoadmapStagingData.PYTHON_PROJECTS
        assertEquals("Should have exactly 6 projects", 6, projects.size)

        val titles = projects.map { it.title }
        assertTrue("Contains CLI Expense Tracker", titles.contains("CLI Expense Tracker"))
        assertTrue("Contains File Organizer", titles.contains("File Organizer"))
        assertTrue("Contains Web Scraper and Analyzer", titles.contains("Web Scraper and Analyzer"))
        assertTrue("Contains FastAPI REST API", titles.contains("FastAPI REST API"))
        assertTrue("Contains Database-Connected Application", titles.contains("Database-Connected Application"))
        assertTrue("Contains Full Python Capstone Project", titles.contains("Full Python Capstone Project"))

        projects.forEach { project ->
            assertTrue("Project ${project.title} must have description", project.description.isNotBlank())
            assertTrue("Project ${project.title} must have skills", project.relevantSkills.isNotEmpty())
            assertTrue("Project ${project.title} must have deliverables", project.deliverables.isNotEmpty())
            assertTrue("Project ${project.title} must have instructions", project.instructions.isNotBlank())
        }
    }

    @Test
    fun testFaqQuestions() {
        val faqs = PythonRoadmapStagingData.PYTHON_FAQS
        assertEquals("Should have exactly 6 FAQs", 6, faqs.size)

        val expectedQuestions = listOf(
            "What is a Python developer?",
            "What should I learn before Python?",
            "How long does it take to learn Python?",
            "What projects can I build with Python?",
            "How should I practice Python?",
            "What should I learn after Python basics?"
        )

        expectedQuestions.forEachIndexed { index, question ->
            assertEquals(question, faqs[index].question)
            assertTrue("Answer must not be blank for $question", faqs[index].answer.isNotBlank())
        }
    }

    @Test
    fun testVisualRoadmapNodes() {
        val nodes = PythonRoadmapStagingData.ALL_VISUAL_NODES
        assertTrue("Roadmap should contain rich set of nodes (> 60)", nodes.size >= 60)

        // Verify Major Milestones
        val majorNodes = nodes.filter { it.isMajor }
        assertTrue("Should have major milestones", majorNodes.size >= 5)
        val majorTitles = majorNodes.map { it.title }
        assertTrue("Contains Learn the Basics", majorTitles.contains("Learn the Basics"))
        assertTrue("Contains Data Structures & Algorithms", majorTitles.contains("Data Structures & Algorithms"))
        assertTrue("Contains Object Oriented Programming", majorTitles.contains("Object Oriented Programming"))
        assertTrue("Contains Concurrency", majorTitles.contains("Concurrency"))
        assertTrue("Contains Testing", majorTitles.contains("Testing") || majorTitles.contains("Testing & Code Quality"))

        // Verify each node has non-empty fields
        nodes.forEach { node ->
            assertTrue("Node ${node.id} must have non-blank title", node.title.isNotBlank())
            assertTrue("Node ${node.id} must have non-blank section", node.section.isNotBlank())
            assertTrue("Node ${node.id} must have objectives", node.objectives.isNotEmpty())
            assertTrue("Node ${node.id} must have explanation", node.explanation.isNotBlank())
            assertTrue("Node ${node.id} must have codeSnippet", node.codeSnippet.isNotBlank())
        }
    }

    @Test
    fun testRoleBasedCoursesList() {
        val courses = PythonRoadmapStagingData.ROLE_BASED_COURSES
        assertEquals("Should have exactly 18 Role-based courses", 18, courses.size)

        val expected = listOf(
            "Frontend Development",
            "Backend Development",
            "Full-Stack Development",
            "Android Development",
            "DevOps",
            "Cybersecurity",
            "Data Analyst",
            "AI Engineer",
            "Machine Learning Engineer",
            "Data Engineer",
            "Software Architect",
            "QA Engineer",
            "UI/UX Designer",
            "Game Developer",
            "Product Manager",
            "Engineering Manager",
            "Network Engineer",
            "Business Intelligence Analyst"
        )

        expected.forEach { course ->
            assertTrue("Role-based catalog must contain $course", courses.contains(course))
        }
    }

    @Test
    fun testSkillBasedCoursesList() {
        val courses = PythonRoadmapStagingData.SKILL_BASED_COURSES
        assertTrue("Should have at least 20 Skill-based courses", courses.size >= 20)

        // Verify top 4 courses in exact required order
        assertEquals("Python Programming", courses[0])
        assertEquals("Java", courses[1])
        assertEquals("Machine Learning", courses[2])
        assertEquals("SQL", courses[3])

        val expected = listOf(
            "Python Programming",
            "Java",
            "Machine Learning",
            "SQL",
            "JavaScript",
            "TypeScript",
            "C++",
            "React",
            "Node.js",
            "Angular",
            "Vue.js",
            "Data Structures & Algorithms",
            "Computer Science",
            "Python for Data Analysis",
            "R Programming",
            "Cloud Computing",
            "System Design",
            "Blockchain",
            "Generative AI",
            "Git & GitHub",
            "Linux"
        )

        expected.forEach { course ->
            assertTrue("Skill-based catalog must contain $course", courses.contains(course))
        }
    }

    @Test
    fun testLessonPacks() {
        val packs = PythonRoadmapStagingData.LESSON_PACKS
        assertTrue("Lesson packs should not be empty", packs.isNotEmpty())
        packs.forEach { pack ->
            assertTrue("Lesson pack title must not be blank", pack.title.isNotBlank())
            assertTrue("Lesson pack must have positive lesson count", pack.lessonCount > 0)
        }
    }
}
