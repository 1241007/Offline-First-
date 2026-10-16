package com.offline_First.data.local

import com.offline_First.domain.model.*

/**
 * Structured static Front-end Web Development roadmap data provider.
 * Modeled after roadmap.sh visual reference with core milestones and connected branch nodes.
 */
object WebDevRoadmapDataProvider {

    fun getNodes(): List<WebDevNode> = listOf(
        // CORE MILESTONES (Central yellow spine)
        WebDevNode(
            id = "core_internet",
            label = "Internet",
            subtitle = "Foundations of networking & the web",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "The global system of interconnected computer networks that communicate using TCP/IP protocols to transmit data across the world.",
            learningObjectives = listOf(
                "Understand IP addressing and packet routing",
                "Understand DNS resolution flow",
                "Learn the HTTP/HTTPS request-response lifecycle"
            ),
            subtopics = listOf(
                WebDevSubtopic("The Global Network", "How physical cables, routers, and ISPs connect servers and clients around the world."),
                WebDevSubtopic("Protocols: TCP/IP & UDP", "TCP guarantees ordered, reliable packet delivery; UDP prioritizes low-latency streaming.")
            ),
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Basic HTTP Request Structure",
                    language = "http",
                    code = "GET /api/v1/courses HTTP/1.1\nHost: api.edunova.com\nUser-Agent: Mozilla/5.0\nAccept: application/json",
                    expectedOutput = "HTTP/1.1 200 OK\nContent-Type: application/json\nContent-Length: 124\n\n{\"status\": \"success\", \"courses\": [\"Web Development\"]}",
                    explanation = "An HTTP request consists of a verb, path, headers, and an optional body payload."
                )
            ),
            practiceQuestions = listOf(
                PracticeQuestion(
                    id = "q_net_1",
                    question = "What protocol ensures that web packets arrive reliably and in the correct order?",
                    options = listOf("UDP", "TCP", "DNS", "ICMP"),
                    correctIndex = 1,
                    explanation = "TCP (Transmission Control Protocol) establishes a connection and handles acknowledgments, retransmission, and packet ordering."
                )
            )
        ),

        // INTERNET BRANCH TOPICS (Right side)
        WebDevNode(
            id = "branch_how_internet_works",
            label = "How does the internet work?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "Data travels over the internet in packets routed through switches, optical cables, and internet exchange points (IXPs).",
            learningObjectives = listOf("Differentiate clients and servers", "Trace the journey of a data packet across routers"),
            subtopics = listOf(
                WebDevSubtopic("Client-Server Model", "Clients (browsers) request resources; servers process requests and serve responses.")
            ),
            practiceQuestions = listOf(
                PracticeQuestion(
                    id = "q_int_how_1",
                    question = "What device directs network packets between different IP networks?",
                    options = listOf("Router", "Monitor", "RAM", "Compiler"),
                    correctIndex = 0,
                    explanation = "Routers inspect destination IP addresses and direct network packets across networks."
                )
            )
        ),
        WebDevNode(
            id = "branch_what_is_http",
            label = "What is HTTP?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "Hypertext Transfer Protocol is the application layer protocol governing communication between web browsers and servers.",
            learningObjectives = listOf("Master HTTP verbs: GET, POST, PUT, PATCH, DELETE", "Understand HTTP status codes (2xx, 3xx, 4xx, 5xx)"),
            subtopics = listOf(
                WebDevSubtopic("HTTP Status Codes", "200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found, 500 Server Error.")
            ),
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Modern Fetch API in JavaScript",
                    language = "javascript",
                    code = "const response = await fetch('https://api.example.com/data');\nconst data = await response.json();\nconsole.log(data);",
                    expectedOutput = "{ id: 1, message: 'Connected successfully' }",
                    explanation = "fetch() issues an asynchronous HTTP GET request and returns a Promise resolving to the Response."
                )
            )
        ),
        WebDevNode(
            id = "branch_domain_name",
            label = "What is Domain Name?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "A human-friendly alias (like edunova.org) mapping to a computer's numerical IP address (like 192.0.2.1).",
            learningObjectives = listOf("Understand TLDs (top-level domains), registrars, and DNS records")
        ),
        WebDevNode(
            id = "branch_what_is_hosting",
            label = "What is hosting?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "Web hosting is the service of allocating storage space and compute resources on a web server so websites can be accessed via the internet.",
            learningObjectives = listOf("Compare shared hosting, VPS, cloud computing, and static edge platforms (Vercel, Netlify)")
        ),
        WebDevNode(
            id = "branch_dns_how_works",
            label = "DNS and how it works?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "The Domain Name System acts as the phonebook of the internet, resolving hostname lookups into IP addresses.",
            learningObjectives = listOf("Understand recursive resolvers, root servers, TLD servers, and authoritative nameservers")
        ),
        WebDevNode(
            id = "branch_browsers_how_work",
            label = "Browsers and how they work?",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_internet",
            overview = "Modern browser rendering engines (Blink, Gecko, WebKit) parse HTML/CSS, build the DOM and CSSOM, perform layout, and paint pixels on screen.",
            learningObjectives = listOf("Understand the Critical Rendering Path: DOM -> CSSOM -> Render Tree -> Layout -> Paint")
        ),

        // CORE WEB TRIO
        WebDevNode(
            id = "core_html",
            label = "HTML",
            subtitle = "Hypertext Markup Language (Structure)",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "The standard markup language used to structure web documents using elements, semantic tags, forms, and accessibility attributes.",
            learningObjectives = listOf("Semantic HTML5: header, nav, main, article, section, footer", "Forms, input validation, and accessible ARIA attributes"),
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Semantic HTML5 Template",
                    language = "html",
                    code = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>EduNova Web</title>\n</head>\n<body>\n  <header><h1>Welcome to EduNova</h1></header>\n  <main><p>Learn modern web development!</p></main>\n</body>\n</html>",
                    expectedOutput = "[Rendered document with semantic header and main content]",
                    explanation = "Semantic tags improve SEO, screen reader accessibility, and maintainability."
                )
            )
        ),
        WebDevNode(
            id = "core_css",
            label = "CSS",
            subtitle = "Cascading Style Sheets (Styling & Layout)",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Controls presentation, colors, typography, responsiveness, and layouts with Flexbox and CSS Grid.",
            learningObjectives = listOf("Master the Box Model: margin, border, padding, content", "Build responsive layouts with Flexbox and Grid", "Use Media Queries for mobile-first styling"),
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Flexbox Responsive Center Card",
                    language = "css",
                    code = ".card-container {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  gap: 1.5rem;\n  padding: 2rem;\n}",
                    expectedOutput = "[Elements aligned centrally with even spacing]",
                    explanation = "Flexbox enables one-dimensional flexible alignment and spacing."
                )
            )
        ),
        WebDevNode(
            id = "core_javascript",
            label = "JavaScript",
            subtitle = "Programming logic & DOM interactivity",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "The programming language of the web. Manages client-side logic, event handling, DOM manipulation, and asynchronous APIs.",
            learningObjectives = listOf("Modern ES6+: let/const, arrow functions, destructuring, modules", "Async JavaScript: Promises, async/await, fetch API", "DOM manipulation and event listeners"),
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Async/Await with Array Transformation",
                    language = "javascript",
                    code = "const fetchTopics = async () => {\n  const list = ['HTML', 'CSS', 'JavaScript'];\n  const formatted = list.map((item, idx) => `${'$'}{idx + 1}. ${'$'}{item}`);\n  return formatted;\n};\n\nfetchTopics().then(console.log);",
                    expectedOutput = "['1. HTML', '2. CSS', '3. JavaScript']",
                    explanation = "Array.map transforms items cleanly while async/await manages asynchronous flows."
                )
            )
        ),

        // VERSION CONTROL & HOSTING
        WebDevNode(
            id = "core_version_control",
            label = "Version Control",
            subtitle = "Tracking changes and team collaboration",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Tracks revisions of source code over time, enabling branching, merging, and collaboration without data loss."
        ),
        WebDevNode(
            id = "branch_git",
            label = "Git",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_version_control",
            overview = "The world's most widely used distributed version control system.",
            learningObjectives = listOf("git init, add, commit, branch, checkout, merge, rebase, status, diff")
        ),
        WebDevNode(
            id = "core_vcs_hosting",
            label = "VCS Hosting",
            subtitle = "Remote git platforms",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Cloud-hosted git repositories with pull requests, CI/CD pipelines, and project management tools."
        ),
        WebDevNode(
            id = "branch_github",
            label = "GitHub",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_vcs_hosting",
            overview = "The premier developer platform hosting open-source and private repositories with GitHub Actions CI/CD."
        ),
        WebDevNode(
            id = "branch_gitlab",
            label = "GitLab",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_vcs_hosting",
            overview = "A complete DevOps platform with built-in CI/CD pipelines, issue tracking, and self-hosted capabilities."
        ),

        // PACKAGE MANAGERS
        WebDevNode(
            id = "core_package_managers",
            label = "Package Managers",
            subtitle = "Managing project dependencies",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Tools for installing, updating, and managing third-party JavaScript libraries and dependencies."
        ),
        WebDevNode(
            id = "branch_npm",
            label = "npm",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_package_managers",
            overview = "Node Package Manager, the default package manager bundled with Node.js."
        ),
        WebDevNode(
            id = "branch_pnpm",
            label = "pnpm",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_package_managers",
            overview = "Fast, disk space efficient package manager using hard links and content-addressable storage."
        ),
        WebDevNode(
            id = "branch_yarn",
            label = "yarn",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_package_managers",
            overview = "Popular alternative package manager created by Facebook with fast caching and workspaces."
        ),
        WebDevNode(
            id = "branch_bun",
            label = "Bun",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_package_managers",
            overview = "All-in-one JavaScript runtime, bundler, and ultra-fast package manager written in Zig."
        ),

        // CSS FRAMEWORKS
        WebDevNode(
            id = "core_css_frameworks",
            label = "CSS Frameworks",
            subtitle = "Accelerated UI styling",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Utility-first and component libraries for rapid, consistent interface development."
        ),
        WebDevNode(
            id = "branch_tailwind",
            label = "Tailwind",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_css_frameworks",
            overview = "A utility-first CSS framework packed with classes like flex, pt-4, text-center that can be composed to build custom designs.",
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "Tailwind CSS Button",
                    language = "html",
                    code = "<button class=\"px-4 py-2 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700 transition\">\n  Click Me\n</button>",
                    expectedOutput = "[Styled interactive modern indigo button]",
                    explanation = "Utility classes eliminate the need to write separate CSS stylesheets for common elements."
                )
            )
        ),

        // FRAMEWORKS
        WebDevNode(
            id = "core_framework",
            label = "Learn a Framework",
            subtitle = "Component-driven web applications",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Modern web apps are structured with component trees, declarative state, and reactive UI updates."
        ),
        WebDevNode(
            id = "branch_react",
            label = "React",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_framework",
            overview = "The most popular library for building user interfaces using components, JSX, and Hooks (useState, useEffect, useMemo).",
            codeExamples = listOf(
                WebDevCodeExample(
                    title = "React Functional Component with Hook",
                    language = "javascript",
                    code = "import { useState } from 'react';\n\nexport function Counter() {\n  const [count, setCount] = useState(0);\n  return (\n    <button onClick={() => setCount(count + 1)}>\n      Count: {count}\n    </button>\n  );\n}",
                    expectedOutput = "[Button incrementing on click]",
                    explanation = "React updates the virtual DOM and re-renders components whenever state variables change."
                )
            )
        ),
        WebDevNode(
            id = "branch_vue",
            label = "Vue.js",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_framework",
            overview = "Progressive framework known for its intuitive template syntax and Composition API."
        ),
        WebDevNode(
            id = "branch_angular",
            label = "Angular",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_framework",
            overview = "Enterprise-grade TypeScript web application framework by Google with dependency injection and CLI."
        ),
        WebDevNode(
            id = "branch_svelte",
            label = "Svelte",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_framework",
            overview = "Compiler that converts components into tiny, highly optimized vanilla JavaScript without a virtual DOM."
        ),
        WebDevNode(
            id = "branch_solid",
            label = "Solid JS",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_framework",
            overview = "Fine-grained reactive UI library with JSX syntax and top-tier runtime performance."
        ),

        // AI ASSISTED CODING
        WebDevNode(
            id = "core_ai_assisted",
            label = "AI Assisted Coding",
            subtitle = "Modern AI developer workflows",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Integrating AI code assistants and agents into everyday software engineering workflows."
        ),
        WebDevNode(
            id = "branch_cursor",
            label = "Cursor",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_ai_assisted",
            overview = "AI-first code editor fork of VS Code with multi-file edits, codebase chat, and terminal integration."
        ),
        WebDevNode(
            id = "branch_claude_code",
            label = "Claude Code",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_ai_assisted",
            overview = "Anthropic's agentic command-line tool for multi-file refactoring, debugging, and terminal automation."
        ),
        WebDevNode(
            id = "branch_copilot",
            label = "Copilot",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_ai_assisted",
            overview = "GitHub's AI pair programmer offering inline code completions and IDE chat."
        ),
        WebDevNode(
            id = "branch_antigravity",
            label = "Antigravity",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_ai_assisted",
            overview = "Advanced agentic coding environment with specialized subagents, terminal execution, and knowledge tools."
        ),

        // PROMPTING & AGENTS
        WebDevNode(
            id = "core_prompting",
            label = "Prompting Techniques",
            subtitle = "Instruction design & context steering",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Techniques including few-shot prompting, chain-of-thought, role specification, and structured XML/JSON outputs."
        ),
        WebDevNode(
            id = "core_agents_mcp",
            label = "Agents, MCP & Skills",
            subtitle = "Model Context Protocol & autonomous tools",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Model Context Protocol (MCP) standardized protocols for connecting LLMs to external databases, APIs, and tools."
        ),

        // IMPLEMENTING AI
        WebDevNode(
            id = "core_implementing_ai",
            label = "Implementing AI",
            subtitle = "Integrating LLMs into web apps",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Connecting frontend applications to LLM APIs with streaming responses and function calling."
        ),
        WebDevNode(
            id = "branch_gemini",
            label = "Gemini",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_implementing_ai",
            overview = "Google's multimodal AI model with massive context windows and fast inference."
        ),
        WebDevNode(
            id = "branch_openai",
            label = "OpenAI",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_implementing_ai",
            overview = "GPT-4o and o-series reasoning models powering chat and tool integration."
        ),
        WebDevNode(
            id = "branch_anthropic",
            label = "Anthropic",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_implementing_ai",
            overview = "Claude 3.5 Sonnet and Haiku models known for state-of-the-art coding and reasoning."
        ),

        // BUILD TOOLS & BUNDLERS
        WebDevNode(
            id = "core_bundlers",
            label = "Module Bundlers",
            subtitle = "Packaging web assets for production",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Transpiles, bundles, minifies, and hot-reloads web application code for developer ergonomics and high production speed."
        ),
        WebDevNode(
            id = "branch_vite",
            label = "Vite",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_bundlers",
            overview = "Next-generation frontend tool offering instantaneous development server start via native ESM and Rollup-based production builds."
        ),
        WebDevNode(
            id = "branch_esbuild",
            label = "esbuild",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_bundlers",
            overview = "An extremely fast JavaScript/TypeScript bundler and minifier written in Go."
        ),
        WebDevNode(
            id = "branch_rollup",
            label = "Rollup",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_bundlers",
            overview = "Module bundler for JavaScript which compiles small pieces of code into something larger and more complex."
        ),

        // LINTERS & FORMATTERS
        WebDevNode(
            id = "core_linters",
            label = "Linters & Formatters",
            subtitle = "Code quality & uniform conventions",
            nodeType = WebDevNodeType.CORE_MILESTONE,
            recommendation = RecommendationType.CORE_PATH,
            overview = "Automated static analysis tools preventing bugs and ensuring code style uniformity across teams."
        ),
        WebDevNode(
            id = "branch_eslint",
            label = "ESLint",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_linters",
            overview = "Pluggable and configurable linter tool for identifying and reporting on patterns in JavaScript."
        ),
        WebDevNode(
            id = "branch_prettier",
            label = "Prettier",
            nodeType = WebDevNodeType.BRANCH_TOPIC,
            recommendation = RecommendationType.PERSONAL_RECOMMENDATION,
            parentId = "core_linters",
            overview = "An opinionated code formatter supporting JavaScript, TypeScript, CSS, HTML, and JSON."
        ),
        WebDevNode(
            id = "branch_biome",
            label = "Biome",
            nodeType = WebDevNodeType.ALTERNATIVE_NODE,
            recommendation = RecommendationType.ALTERNATIVE_OPTION,
            parentId = "core_linters",
            overview = "Fast all-in-one toolchain for web projects written in Rust, combining formatting and linting."
        )
    )

    fun getNodeById(id: String): WebDevNode? = getNodes().firstOrNull { it.id == id }
}
