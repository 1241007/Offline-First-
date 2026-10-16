package com.offline_First.domain.model

enum class WebDevNodeType {
    CORE_MILESTONE,   // Yellow box on central spine (e.g. Internet, HTML, CSS, JavaScript)
    BRANCH_TOPIC,     // Topic box branching off (e.g. What is HTTP?, Tailwind, React)
    ALTERNATIVE_NODE, // Alternative tool option (e.g. Bun, pnpm, Vue.js, Svelte)
    PROJECT_CALLOUT   // Info/project idea callout card
}

enum class RecommendationType {
    PERSONAL_RECOMMENDATION, // Purple check badge
    ALTERNATIVE_OPTION,      // Green check badge
    ORDER_NOT_STRICT,        // Grey check badge
    CORE_PATH                // Central yellow box
}

data class WebDevSubtopic(
    val title: String,
    val explanation: String,
    val keyPoints: List<String> = emptyList()
)

data class WebDevCodeExample(
    val title: String,
    val language: String,
    val code: String,
    val expectedOutput: String,
    val explanation: String
)

data class WebDevNode(
    val id: String,
    val label: String,
    val subtitle: String = "",
    val nodeType: WebDevNodeType,
    val recommendation: RecommendationType = RecommendationType.PERSONAL_RECOMMENDATION,
    val parentId: String? = null,
    val category: String = "Frontend",
    val overview: String = "",
    val learningObjectives: List<String> = emptyList(),
    val subtopics: List<WebDevSubtopic> = emptyList(),
    val codeExamples: List<WebDevCodeExample> = emptyList(),
    val practiceQuestions: List<PracticeQuestion> = emptyList(),
    val estimatedMinutes: Int = 20
)
