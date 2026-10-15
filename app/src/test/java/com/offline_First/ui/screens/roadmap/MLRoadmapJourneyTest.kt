package com.offline_First.ui.screens.roadmap

import com.offline_First.data.staging.MLRoadmapStagingData
import com.offline_First.data.staging.PythonRoadmapStagingData
import org.junit.Assert.*
import org.junit.Test

class MLRoadmapJourneyTest {

    @Test
    fun testMLRoadmapNodesCompleteness() {
        val nodes = MLRoadmapStagingData.ALL_VISUAL_NODES
        assertTrue("Machine Learning roadmap should contain a comprehensive set of nodes (>= 60)", nodes.size >= 60)

        // Verify major milestone spine
        val majorNodes = nodes.filter { it.isMajor }
        assertTrue("Should have major milestones (>= 8)", majorNodes.size >= 8)

        val majorTitles = majorNodes.map { it.title }
        assertTrue("Contains Machine Learning root node", majorTitles.contains("Machine Learning"))
        assertTrue("Contains Introduction", majorTitles.contains("Introduction"))
        assertTrue("Contains Linear Algebra", majorTitles.contains("Linear Algebra"))
        assertTrue("Contains Calculus", majorTitles.contains("Calculus"))
        assertTrue("Contains Statistics", majorTitles.contains("Statistics"))
        assertTrue("Contains Probability", majorTitles.contains("Probability"))
        assertTrue("Contains Python", majorTitles.contains("Python"))
        assertTrue("Contains Essential Libraries", majorTitles.contains("Essential Libraries"))
        assertTrue("Contains Data Sources", majorTitles.contains("Data Sources"))
        assertTrue("Contains Data Formats", majorTitles.contains("Data Formats"))
        assertTrue("Contains Types of Machine Learning", majorTitles.contains("Types of Machine Learning"))
        assertTrue("Contains Scikit-learn", majorTitles.contains("Scikit-learn"))
        assertTrue("Contains Classification", majorTitles.contains("Classification"))
        assertTrue("Contains Regression", majorTitles.contains("Regression"))
        assertTrue("Contains Clustering", majorTitles.contains("Clustering"))
        assertTrue("Contains Deep Learning", majorTitles.contains("Deep Learning"))
        assertTrue("Contains Deep Learning Libraries", majorTitles.contains("Deep Learning Libraries"))
        assertTrue("Contains Convolutional Neural Network", majorTitles.contains("Convolutional Neural Network"))
        assertTrue("Contains Explainable AI", majorTitles.contains("Explainable AI"))

        val titles = nodes.map { it.title }

        // Section A: Mathematical Foundations
        assertTrue("Contains Derivatives, Partial Derivatives", titles.contains("Derivatives, Partial Derivatives"))
        assertTrue("Contains Chain rule of derivation", titles.contains("Chain rule of derivation"))
        assertTrue("Contains Gradient, Jacobian, Hessian", titles.contains("Gradient, Jacobian, Hessian"))
        assertTrue("Contains Matrix & Matrix Operations", titles.contains("Matrix & Matrix Operations"))
        assertTrue("Contains Scalars, Vectors, Tensors", titles.contains("Scalars, Vectors, Tensors"))
        assertTrue("Contains Singular Value Decomposition", titles.contains("Singular Value Decomposition"))
        assertTrue("Contains Determinants, Inverse of Matrix", titles.contains("Determinants, Inverse of Matrix"))
        assertTrue("Contains Eigenvalues, Diagonalization", titles.contains("Eigenvalues, Diagonalization"))
        assertTrue("Contains Basics of Probability", titles.contains("Basics of Probability"))
        assertTrue("Contains Bayes Theorem", titles.contains("Bayes Theorem"))
        assertTrue("Contains Descriptive Statistics", titles.contains("Descriptive Statistics"))
        assertTrue("Contains Inferential Statistics", titles.contains("Inferential Statistics"))

        // Section B: Programming & Data Preparation
        assertTrue("Contains Basic Syntax", titles.contains("Basic Syntax"))
        assertTrue("Contains Variables and Data Types", titles.contains("Variables and Data Types"))
        assertTrue("Contains Numpy", titles.contains("Numpy"))
        assertTrue("Contains Pandas", titles.contains("Pandas"))
        assertTrue("Contains Matplotlib", titles.contains("Matplotlib"))
        assertTrue("Contains Data Formats", titles.contains("Data Formats"))
        assertTrue("Contains Preprocessing Techniques", titles.contains("Preprocessing Techniques"))
        assertTrue("Contains Feature Engineering", titles.contains("Feature Engineering"))
        assertTrue("Contains Feature Scaling & Normalization", titles.contains("Feature Scaling & Normalization"))
        assertTrue("Contains Dimensionality Reduction", titles.contains("Dimensionality Reduction"))
        assertTrue("Contains Feature Selection", titles.contains("Feature Selection"))

        // Section C: Machine Learning Algorithms & Evaluation
        assertTrue("Contains Supervised Learning", titles.contains("Supervised Learning"))
        assertTrue("Contains Unsupervised Learning", titles.contains("Unsupervised Learning"))
        assertTrue("Contains Reinforcement Learning", titles.contains("Reinforcement Learning"))
        assertTrue("Contains K-Nearest Neighbors (KNN)", titles.contains("K-Nearest Neighbors (KNN)"))
        assertTrue("Contains Logistic Regression", titles.contains("Logistic Regression"))
        assertTrue("Contains Support Vector Machines", titles.contains("Support Vector Machines"))
        assertTrue("Contains Decision Trees, Random Forest", titles.contains("Decision Trees, Random Forest"))
        assertTrue("Contains Gradient Boosting Machines", titles.contains("Gradient Boosting Machines"))
        assertTrue("Contains Linear Regression", titles.contains("Linear Regression"))
        assertTrue("Contains Polynomial Regression", titles.contains("Polynomial Regression"))
        assertTrue("Contains Model Evaluation", titles.contains("Model Evaluation"))
        assertTrue("Contains Metrics to Evaluate", titles.contains("Metrics to Evaluate"))
        assertTrue("Contains Validation Techniques", titles.contains("Validation Techniques"))

        // Section D: Deep Learning & Advanced Concepts
        assertTrue("Contains Neural Network (NN) Basics", titles.contains("Neural Network (NN) Basics"))
        assertTrue("Contains Deep Learning Libraries", titles.contains("Deep Learning Libraries"))
        assertTrue("Contains Deep Learning Architectures", titles.contains("Deep Learning Architectures"))
        assertTrue("Contains Convolutional Neural Network", titles.contains("Convolutional Neural Network"))
        assertTrue("Contains Recurrent Neural Networks", titles.contains("Recurrent Neural Networks"))
        assertTrue("Contains Attention Mechanisms", titles.contains("Attention Mechanisms"))
        assertTrue("Contains Attention Models", titles.contains("Attention Models"))
        assertTrue("Contains Generative Adversarial Networks", titles.contains("Generative Adversarial Networks"))
        assertTrue("Contains Natural Language Processing", titles.contains("Natural Language Processing"))
        assertTrue("Contains Tokenization", titles.contains("Tokenization"))
        assertTrue("Contains Embeddings", titles.contains("Embeddings"))
    }

    @Test
    fun testMLFaqsCountAndContent() {
        val faqs = MLRoadmapStagingData.ML_FAQS
        assertEquals("Should have exactly 5 FAQs", 5, faqs.size)

        val questions = faqs.map { it.question }
        assertTrue("Contains 'What is a Machine Learning Engineer?'", questions.contains("What is a Machine Learning Engineer?"))
        assertTrue("Contains 'What mathematics do I need to learn first?'", questions.contains("What mathematics do I need to learn first?"))
        assertTrue("Contains 'Which programming language should I learn?'", questions.contains("Which programming language should I learn?"))
        assertTrue("Contains 'What is the difference between Supervised and Unsupervised learning?'", questions.contains("What is the difference between Supervised and Unsupervised learning?"))
        assertTrue("Contains 'When should I transition from classical ML to Deep Learning?'", questions.contains("When should I transition from classical ML to Deep Learning?"))

        faqs.forEach { faq ->
            assertTrue("FAQ answer must not be blank for ${faq.question}", faq.answer.isNotBlank())
        }
    }

    @Test
    fun testMLProjectsRequirements() {
        val projects = MLRoadmapStagingData.ML_PROJECTS
        assertEquals("Should have exactly 4 projects", 4, projects.size)

        val titles = projects.map { it.title }
        assertTrue("Contains Customer Churn Prediction", titles.contains("Customer Churn Prediction"))
        assertTrue("Contains House Price Regression Model", titles.contains("House Price Regression Model"))
        assertTrue("Contains Image Classification with CNN", titles.contains("Image Classification with CNN"))
        assertTrue("Contains Sentiment Analysis with Transformers", titles.contains("Sentiment Analysis with Transformers"))

        projects.forEach { project ->
            assertTrue("Project ${project.title} must have description", project.description.isNotBlank())
            assertTrue("Project ${project.title} must have skills", project.relevantSkills.isNotEmpty())
            assertTrue("Project ${project.title} must have deliverables", project.deliverables.isNotEmpty())
            assertTrue("Project ${project.title} must have instructions", project.instructions.isNotBlank())
        }
    }

    @Test
    fun testMLProgressCalculation() {
        val allNodes = MLRoadmapStagingData.ALL_VISUAL_NODES
        val totalCount = allNodes.size

        // Empty completion
        val initialProgress = 0 / totalCount.toFloat()
        assertEquals(0f, initialProgress, 0.01f)

        // 10 completed topics
        val completed10 = allNodes.take(10).map { it.id }.toSet()
        val progress10 = (completed10.size.toFloat() / totalCount).coerceIn(0f, 1f)
        assertTrue("Progress should be between 0.10 and 0.20", progress10 in 0.10f..0.20f)

        // All completed
        val allCompleted = allNodes.map { it.id }.toSet()
        val fullProgress = (allCompleted.size.toFloat() / totalCount).coerceIn(0f, 1f)
        assertEquals(1f, fullProgress, 0.01f)
    }

    @Test
    fun testMLRoadmapIsMarkedImplementedInCatalog() {
        val mlPath = PythonRoadmapStagingData.LEARNING_PATHS.first { it.id == "machine-learning" }
        assertTrue("Machine Learning path must be marked as implemented", mlPath.isImplemented)
    }
}
