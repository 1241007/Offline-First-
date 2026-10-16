package com.offline_First.ui.screens.roadmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offline_First.data.staging.PythonRoadmapStagingData
import com.offline_First.data.staging.VisualRoadmapNode
import com.offline_First.ui.theme.*

/**
 * Connected Visual Diagram for Python Programming Roadmap.
 * Strictly reflects Screenshot 1 learning flow and connections while adopting
 * Screenshot 5's EduNova Java visual language (mint/teal palette, compact rectangular nodes, orthogonal lines).
 */
@Composable
fun EduNovaPythonVisualRoadmapDiagram(
    selectedNodeId: String?,
    completedTopicIds: Set<String>,
    onSelectNode: (VisualRoadmapNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val canvasWidth = 640.dp
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val nodeCoords = remember { mutableStateMapOf<String, LayoutCoordinates>() }

    val primaryConnector = EduNovaPrimaryConnectorColor
    val secondaryConnector = EduNovaSecondaryConnectorColor
    val secondaryDashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)

    Box(
        modifier = modifier
            .width(canvasWidth)
            .padding(horizontal = 12.dp)
            .onGloballyPositioned { rootCoordinates = it }
    ) {
        // --- 1. RECTILINEAR & FANNING CONNECTOR CANVAS DRAWN BEHIND NODES ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val root = rootCoordinates ?: return@Canvas
            if (!root.isAttached) return@Canvas

            fun getBounds(id: String): Rect? {
                val coords = nodeCoords[id] ?: return null
                if (!coords.isAttached) return null
                val pos = root.localPositionOf(coords, Offset.Zero)
                return Rect(pos, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
            }

            fun drawStraightLine(start: Offset, end: Offset, isDashed: Boolean = false) {
                if (isDashed) {
                    drawLine(
                        color = secondaryConnector,
                        start = start,
                        end = end,
                        strokeWidth = 1.8.dp.toPx(),
                        pathEffect = secondaryDashEffect
                    )
                } else {
                    drawLine(
                        color = primaryConnector,
                        start = start,
                        end = end,
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            }

            fun drawOrthogonalPath(start: Offset, end: Offset, isDashed: Boolean = false) {
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    val midY = (start.y + end.y) / 2f
                    lineTo(start.x, midY)
                    lineTo(end.x, midY)
                    lineTo(end.x, end.y)
                }
                if (isDashed) {
                    drawPath(
                        path = path,
                        color = secondaryConnector,
                        style = Stroke(width = 1.8.dp.toPx(), pathEffect = secondaryDashEffect)
                    )
                } else {
                    drawPath(
                        path = path,
                        color = primaryConnector,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            fun drawFanningDottedCurve(start: Offset, end: Offset, isLeft: Boolean) {
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    val controlOffsetX = 28.dp.toPx() * (if (isLeft) -1f else 1f)
                    cubicTo(
                        start.x + controlOffsetX, start.y,
                        end.x - controlOffsetX, end.y,
                        end.x, end.y
                    )
                }
                drawPath(
                    path = path,
                    color = secondaryConnector,
                    style = Stroke(width = 1.8.dp.toPx(), pathEffect = secondaryDashEffect)
                )
            }

            // Top Header: Root "Python" -> Learn the Basics
            val rootPillB = getBounds("python_root_pill")
            val basicsB = getBounds(PythonRoadmapStagingData.BASICS_CENTER_NODE.id)
            if (rootPillB != null && basicsB != null) {
                drawStraightLine(Offset(rootPillB.center.x, rootPillB.bottom), Offset(basicsB.center.x, basicsB.top))
            }

            // Basics -> Left Wing (Syntax, Variables, Operators, Strings, Conditionals, Loops)
            basicsB?.let { b ->
                val leftAnchor = Offset(b.left, b.center.y)
                listOf(
                    PythonRoadmapStagingData.NODE_SYNTAX.id,
                    PythonRoadmapStagingData.NODE_VARIABLES.id,
                    PythonRoadmapStagingData.NODE_OPERATORS.id,
                    PythonRoadmapStagingData.NODE_STRINGS.id,
                    PythonRoadmapStagingData.NODE_CONDITIONALS.id,
                    PythonRoadmapStagingData.NODE_LOOPS.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(leftAnchor, Offset(targetB.right, targetB.center.y), isLeft = true)
                    }
                }
            }

            // Operators -> Operators @
            val opB = getBounds(PythonRoadmapStagingData.NODE_OPERATORS.id)
            val opAtB = getBounds(PythonRoadmapStagingData.NODE_OPERATORS_AT.id)
            if (opB != null && opAtB != null) {
                drawStraightLine(Offset(opB.right, opB.center.y), Offset(opAtB.left, opAtB.center.y), isDashed = true)
            }

            // Basics -> Right Wing (Lists, Dicts, Casting, Functions, Exceptions, Comments)
            basicsB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    PythonRoadmapStagingData.NODE_LISTS.id,
                    PythonRoadmapStagingData.NODE_DICTS.id,
                    PythonRoadmapStagingData.NODE_CASTING.id,
                    PythonRoadmapStagingData.NODE_FUNCTIONS.id,
                    PythonRoadmapStagingData.NODE_EXCEPTIONS.id,
                    PythonRoadmapStagingData.NODE_COMMENTS.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }

            // Lists -> Tuples -> Sets
            val listsB = getBounds(PythonRoadmapStagingData.NODE_LISTS.id)
            val tuplesB = getBounds(PythonRoadmapStagingData.NODE_TUPLES.id)
            val setsB = getBounds(PythonRoadmapStagingData.NODE_SETS.id)
            if (listsB != null && tuplesB != null) {
                drawStraightLine(Offset(listsB.right, listsB.center.y), Offset(tuplesB.left, tuplesB.center.y), isDashed = true)
            }
            if (tuplesB != null && setsB != null) {
                drawStraightLine(Offset(tuplesB.right, tuplesB.center.y), Offset(setsB.left, setsB.center.y), isDashed = true)
            }

            // Comments -> Type Annot.
            val commentsB = getBounds(PythonRoadmapStagingData.NODE_COMMENTS.id)
            val annotB = getBounds(PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id)
            if (commentsB != null && annotB != null) {
                drawStraightLine(Offset(commentsB.right, commentsB.center.y), Offset(annotB.left, annotB.center.y), isDashed = true)
            }

            // Basics -> DSA Center
            val dsaB = getBounds(PythonRoadmapStagingData.DSA_CENTER_NODE.id)
            if (basicsB != null && dsaB != null) {
                drawStraightLine(Offset(basicsB.center.x, basicsB.bottom), Offset(dsaB.center.x, dsaB.top))
            }

            // DSA -> Subtopics Column (Arrays, HashMaps, Heaps/Stacks, BST, Recursion, Sorting)
            dsaB?.let { b ->
                val dsaSubtopics = listOf(
                    PythonRoadmapStagingData.NODE_ARRAYS_LISTS.id,
                    PythonRoadmapStagingData.NODE_HASH_MAPS.id,
                    PythonRoadmapStagingData.NODE_STACKS_QUEUES.id,
                    PythonRoadmapStagingData.NODE_BST.id,
                    PythonRoadmapStagingData.NODE_RECURSION.id,
                    PythonRoadmapStagingData.NODE_SORTING_SEARCHING.id
                )
                var prevAnchor = Offset(b.center.x, b.bottom)
                dsaSubtopics.forEach { id ->
                    getBounds(id)?.let { subB ->
                        drawStraightLine(prevAnchor, Offset(subB.center.x, subB.top), isDashed = true)
                        prevAnchor = Offset(subB.center.x, subB.bottom)
                    }
                }
            }

            // DSA -> Package Managers (Left Branch)
            val pkgMgrB = getBounds(PythonRoadmapStagingData.NODE_PKG_MANAGERS.id)
            if (dsaB != null && pkgMgrB != null) {
                drawOrthogonalPath(Offset(dsaB.left, dsaB.center.y), Offset(pkgMgrB.right, pkgMgrB.center.y))
            }

            // Package Managers -> Top Grid (PyPI, Pip, Conda, UV, Poetry, PDM)
            pkgMgrB?.let { b ->
                val topAnchor = Offset(b.center.x, b.top)
                getBounds(PythonRoadmapStagingData.NODE_POETRY.id)?.let { pB ->
                    drawStraightLine(topAnchor, Offset(pB.center.x, pB.bottom), isDashed = true)
                }
            }

            // Package Managers -> Bottom Chain (Common Packages, pyproject, Comprehensions, etc.)
            pkgMgrB?.let { b ->
                val bottomAnchor = Offset(b.center.x, b.bottom)
                getBounds(PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id)?.let { cB ->
                    drawStraightLine(bottomAnchor, Offset(cB.center.x, cB.top))
                }
            }
            val pkgSubChain = listOf(
                PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id,
                PythonRoadmapStagingData.NODE_PYPROJECT.id,
                PythonRoadmapStagingData.NODE_COMPREHENSIONS.id,
                PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS.id,
                PythonRoadmapStagingData.NODE_PARADIGMS.id,
                PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS.id
            )
            for (i in 0 until pkgSubChain.size - 1) {
                val curB = getBounds(pkgSubChain[i])
                val nextB = getBounds(pkgSubChain[i + 1])
                if (curB != null && nextB != null) {
                    drawStraightLine(Offset(curB.center.x, curB.bottom), Offset(nextB.center.x, nextB.top))
                }
            }

            // DSA -> Modules (Right Branch)
            val modulesB = getBounds(PythonRoadmapStagingData.NODE_MODULES.id)
            if (dsaB != null && modulesB != null) {
                drawOrthogonalPath(Offset(dsaB.right, dsaB.center.y), Offset(modulesB.left, modulesB.center.y))
            }

            // Builtin / Custom -> Modules
            val builtinB = getBounds(PythonRoadmapStagingData.NODE_BUILTIN.id)
            if (builtinB != null && modulesB != null) {
                drawStraightLine(Offset(builtinB.center.x, builtinB.bottom), Offset(builtinB.center.x, modulesB.top), isDashed = true)
            }
            val customB = getBounds(PythonRoadmapStagingData.NODE_CUSTOM.id)
            if (customB != null && modulesB != null) {
                drawStraightLine(Offset(customB.center.x, customB.bottom), Offset(customB.center.x, modulesB.top), isDashed = true)
            }

            // Modules Chain (Modules -> Error Handling -> Lambdas -> Decorators -> Iterators -> Regex -> Variable Scope)
            val modulesChain = listOf(
                PythonRoadmapStagingData.NODE_MODULES.id,
                PythonRoadmapStagingData.NODE_ERROR_HANDLING.id,
                PythonRoadmapStagingData.NODE_LAMBDAS.id,
                PythonRoadmapStagingData.NODE_DECORATORS.id,
                PythonRoadmapStagingData.NODE_ITERATORS.id,
                PythonRoadmapStagingData.NODE_REGEX.id,
                PythonRoadmapStagingData.NODE_VARIABLE_SCOPE.id
            )
            for (i in 0 until modulesChain.size - 1) {
                val curB = getBounds(modulesChain[i])
                val nextB = getBounds(modulesChain[i + 1])
                if (curB != null && nextB != null) {
                    drawStraightLine(Offset(curB.center.x, curB.bottom), Offset(nextB.center.x, nextB.top))
                }
            }

            // DSA Subtopics Bottom -> OOP Center
            val sortingB = getBounds(PythonRoadmapStagingData.NODE_SORTING_SEARCHING.id)
            val oopB = getBounds(PythonRoadmapStagingData.OOP_CENTER_NODE.id)
            if (sortingB != null && oopB != null) {
                drawStraightLine(Offset(sortingB.center.x, sortingB.bottom), Offset(oopB.center.x, oopB.top))
            }

            // OOP Center -> OOP Subtopics (Classes, Methods, Inheritance, Encapsulation)
            oopB?.let { b ->
                val oopSubAnchor = Offset(b.center.x, b.bottom)
                getBounds(PythonRoadmapStagingData.NODE_CLASSES.id)?.let { cB ->
                    drawStraightLine(oopSubAnchor, Offset(cB.center.x, cB.top), isDashed = true)
                }
            }

            // OOP -> Right Branch to Environments and Static Typing
            val staticTypingB = getBounds(PythonRoadmapStagingData.NODE_STATIC_TYPING.id)
            if (oopB != null && staticTypingB != null) {
                drawOrthogonalPath(Offset(oopB.right, oopB.center.y), Offset(staticTypingB.left, staticTypingB.center.y))
            }

            // Static Typing -> Subtopics (typing, mypy, pyright, pyre, pydantic)
            staticTypingB?.let { b ->
                val typingChain = listOf(
                    PythonRoadmapStagingData.NODE_STATIC_TYPING.id,
                    PythonRoadmapStagingData.NODE_TYPING.id,
                    PythonRoadmapStagingData.NODE_MYPY.id,
                    PythonRoadmapStagingData.NODE_PYRIGHT.id,
                    PythonRoadmapStagingData.NODE_PYRE.id,
                    PythonRoadmapStagingData.NODE_PYDANTIC.id
                )
                for (i in 0 until typingChain.size - 1) {
                    val curB = getBounds(typingChain[i])
                    val nextB = getBounds(typingChain[i + 1])
                    if (curB != null && nextB != null) {
                        drawStraightLine(Offset(curB.center.x, curB.bottom), Offset(nextB.center.x, nextB.top), isDashed = true)
                    }
                }
            }

            // OOP Subtopics Bottom -> Concurrency Center
            val inheritanceB = getBounds(PythonRoadmapStagingData.NODE_INHERITANCE.id)
            val concurrencyB = getBounds(PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id)
            if (inheritanceB != null && concurrencyB != null) {
                drawStraightLine(Offset(concurrencyB.center.x, inheritanceB.bottom + 8f), Offset(concurrencyB.center.x, concurrencyB.top))
            }

            // Concurrency -> Subtopics (Multiprocessing, Asynchrony, GIL, Threading, glob)
            concurrencyB?.let { b ->
                val concChain = listOf(
                    PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id,
                    PythonRoadmapStagingData.NODE_MULTIPROCESSING.id,
                    PythonRoadmapStagingData.NODE_ASYNCHRONY.id,
                    PythonRoadmapStagingData.NODE_GIL.id,
                    PythonRoadmapStagingData.NODE_THREADING.id,
                    PythonRoadmapStagingData.NODE_GLOB.id
                )
                for (i in 0 until concChain.size - 1) {
                    val curB = getBounds(concChain[i])
                    val nextB = getBounds(concChain[i + 1])
                    if (curB != null && nextB != null) {
                        drawStraightLine(Offset(curB.center.x, curB.bottom), Offset(nextB.center.x, nextB.top), isDashed = true)
                    }
                }
            }

            // Context Managers -> Learn a Framework (Left Branch)
            val ctxMgrB = getBounds(PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS.id)
            val frameworkB = getBounds(PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id)
            if (ctxMgrB != null && frameworkB != null) {
                drawStraightLine(Offset(ctxMgrB.center.x, ctxMgrB.bottom), Offset(frameworkB.center.x, frameworkB.top))
            }

            // Concurrency -> Code Formatting (Right Branch)
            val codeFormatB = getBounds(PythonRoadmapStagingData.NODE_CODE_FORMATTING.id)
            if (concurrencyB != null && codeFormatB != null) {
                drawOrthogonalPath(Offset(concurrencyB.right, concurrencyB.center.y), Offset(codeFormatB.left, codeFormatB.center.y))
            }

            // Code Formatting -> Subtopics (yapf, black, ruff)
            codeFormatB?.let { b ->
                val leftAnchor = Offset(b.left, b.center.y)
                listOf(
                    PythonRoadmapStagingData.NODE_YAPF.id,
                    PythonRoadmapStagingData.NODE_BLACK.id,
                    PythonRoadmapStagingData.NODE_RUFF.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(leftAnchor, Offset(targetB.right, targetB.center.y), isLeft = true)
                    }
                }
            }

            // Code Formatting -> File Handling -> Testing
            val fileHandlingB = getBounds(PythonRoadmapStagingData.NODE_FILE_HANDLING.id)
            if (codeFormatB != null && fileHandlingB != null) {
                drawStraightLine(Offset(codeFormatB.center.x, codeFormatB.bottom), Offset(fileHandlingB.center.x, fileHandlingB.top))
            }

            val testingB = getBounds(PythonRoadmapStagingData.TESTING_CENTER_NODE.id)
            if (fileHandlingB != null && testingB != null) {
                drawStraightLine(Offset(fileHandlingB.center.x, fileHandlingB.bottom), Offset(testingB.center.x, testingB.top))
            }

            // Testing -> Subtopics (tox, unittest, doctest, pytest)
            testingB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    PythonRoadmapStagingData.NODE_TOX.id,
                    PythonRoadmapStagingData.NODE_UNITTEST.id,
                    PythonRoadmapStagingData.NODE_DOCTEST.id,
                    PythonRoadmapStagingData.NODE_PYTEST.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }

            // Concurrency (glob) / Testing -> Related Roadmaps Card
            val relatedCardB = getBounds("related_roadmaps_card")
            if (testingB != null && relatedCardB != null) {
                drawOrthogonalPath(Offset(testingB.left, testingB.center.y), Offset(relatedCardB.right, relatedCardB.center.y), isDashed = true)
            }
        }

        // --- 2. FOREGROUND NODES & SECTIONS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Related Info Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Related Info Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD4E5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Related Roadmaps",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EduNovaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("Backend Roadmap", "DevOps Roadmap", "Django Roadmap", "Python for Data Analysis").forEach { title ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EduNovaSecondaryConnectorColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = EduNovaTextSecondary
                                )
                            }
                        }
                    }
                }

                // Right Roadmap.sh Info Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD4E5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Find the detailed version of this roadmap along with others",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = EduNovaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF0F5F3),
                            border = BorderStroke(1.dp, Color(0xFFD4E5E1))
                        ) {
                            Text(
                                text = "roadmap.sh",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Top Root Title Node
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduNovaMilestoneBoxBg,
                border = BorderStroke(1.5.dp, EduNovaMilestoneBoxBorder),
                modifier = Modifier
                    .onGloballyPositioned { nodeCoords["python_root_pill"] = it }
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Python",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            // MILESTONE 1: Learn the Basics with Left and Right Branches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Branch: Fundamentals
                Column(
                    modifier = Modifier.width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_SYNTAX,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SYNTAX.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SYNTAX.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SYNTAX) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SYNTAX.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_VARIABLES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_VARIABLES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_VARIABLES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_VARIABLES) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_VARIABLES.id] = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_OPERATORS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_OPERATORS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_OPERATORS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_OPERATORS) },
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_OPERATORS.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_OPERATORS_AT,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_OPERATORS_AT.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_OPERATORS_AT.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_OPERATORS_AT) },
                            compact = true,
                            modifier = Modifier
                                .width(36.dp)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_OPERATORS_AT.id] = it }
                        )
                    }
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_STRINGS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_STRINGS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_STRINGS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_STRINGS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_STRINGS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_CONDITIONALS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CONDITIONALS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CONDITIONALS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CONDITIONALS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CONDITIONALS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_LOOPS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LOOPS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LOOPS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LOOPS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_LOOPS.id] = it }
                    )
                }

                // Center Milestone: Learn the Basics
                EduNovaRoadmapNodeBox(
                    node = PythonRoadmapStagingData.BASICS_CENTER_NODE,
                    isSelected = selectedNodeId == PythonRoadmapStagingData.BASICS_CENTER_NODE.id,
                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.BASICS_CENTER_NODE.id),
                    onClick = { onSelectNode(PythonRoadmapStagingData.BASICS_CENTER_NODE) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.BASICS_CENTER_NODE.id] = it }
                )

                // Right Branch: Data Types & Control
                Column(
                    modifier = Modifier.width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_LISTS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LISTS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LISTS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LISTS) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_LISTS.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_TUPLES,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TUPLES.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TUPLES.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TUPLES) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_TUPLES.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_SETS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SETS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SETS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SETS) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SETS.id] = it }
                        )
                    }
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_DICTS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DICTS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DICTS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DICTS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_DICTS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_CASTING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CASTING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CASTING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CASTING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CASTING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_FUNCTIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FUNCTIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FUNCTIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FUNCTIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_FUNCTIONS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_EXCEPTIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_EXCEPTIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_EXCEPTIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_EXCEPTIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_EXCEPTIONS.id] = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_COMMENTS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_COMMENTS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_COMMENTS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_COMMENTS) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_COMMENTS.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_SCOPE_ANNOT,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SCOPE_ANNOT) },
                            compact = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SCOPE_ANNOT.id] = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MILESTONE 2: Data Structures & Algorithms
            EduNovaRoadmapNodeBox(
                node = PythonRoadmapStagingData.DSA_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.DSA_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.DSA_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.DSA_CENTER_NODE) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.DSA_CENTER_NODE.id] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Middle Section: Left Package Managers, Center DSA Subtopics, Right Modules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Package Managers & Environments
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Package Managers Tool Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_PYPI,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYPI.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYPI.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYPI) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYPI.id] = it }
                            )
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_PIP,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PIP.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PIP.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PIP) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PIP.id] = it }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_CONDA,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CONDA.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CONDA.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CONDA) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CONDA.id] = it }
                            )
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_UV,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_UV.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_UV.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_UV) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_UV.id] = it }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_POETRY,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_POETRY.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_POETRY.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_POETRY) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_POETRY.id] = it }
                            )
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_PDM,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PDM.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PDM.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PDM) },
                                compact = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PDM.id] = it }
                            )
                        }
                    }

                    // Package Managers Milestone Node
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PKG_MANAGERS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PKG_MANAGERS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PKG_MANAGERS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PKG_MANAGERS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PKG_MANAGERS.id] = it }
                    )

                    // Packaging & Language Features Sub-chain
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_COMMON_PACKAGES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_COMMON_PACKAGES) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_COMMON_PACKAGES.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PYPROJECT,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYPROJECT.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYPROJECT.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYPROJECT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYPROJECT.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_COMPREHENSIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_COMPREHENSIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_COMPREHENSIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_COMPREHENSIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_COMPREHENSIONS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_GENERATOR_EXPRESSIONS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PARADIGMS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PARADIGMS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PARADIGMS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PARADIGMS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PARADIGMS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CONTEXT_MANAGERS.id] = it }
                    )
                }

                // Center Column: DSA Core Subtopics
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_ARRAYS_LISTS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ARRAYS_LISTS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ARRAYS_LISTS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ARRAYS_LISTS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_ARRAYS_LISTS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_HASH_MAPS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_HASH_MAPS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_HASH_MAPS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_HASH_MAPS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_HASH_MAPS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_STACKS_QUEUES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_STACKS_QUEUES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_STACKS_QUEUES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_STACKS_QUEUES) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_STACKS_QUEUES.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_BST,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_BST.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_BST.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_BST) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_BST.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_RECURSION,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_RECURSION.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_RECURSION.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_RECURSION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_RECURSION.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_SORTING_SEARCHING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SORTING_SEARCHING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SORTING_SEARCHING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SORTING_SEARCHING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SORTING_SEARCHING.id] = it }
                    )
                }

                // Right Column: Modules & Advanced Python
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Builtin & Custom Modules
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_BUILTIN,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_BUILTIN.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_BUILTIN.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_BUILTIN) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_BUILTIN.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_CUSTOM,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CUSTOM.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CUSTOM.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CUSTOM) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CUSTOM.id] = it }
                        )
                    }
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_MODULES,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_MODULES.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_MODULES.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_MODULES) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_MODULES.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_ERROR_HANDLING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ERROR_HANDLING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ERROR_HANDLING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ERROR_HANDLING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_ERROR_HANDLING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_LAMBDAS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LAMBDAS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LAMBDAS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LAMBDAS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_LAMBDAS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_DECORATORS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DECORATORS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DECORATORS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DECORATORS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_DECORATORS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_ITERATORS,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ITERATORS.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ITERATORS.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ITERATORS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_ITERATORS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_REGEX,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_REGEX.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_REGEX.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_REGEX) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_REGEX.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_VARIABLE_SCOPE,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_VARIABLE_SCOPE.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_VARIABLE_SCOPE.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_VARIABLE_SCOPE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_VARIABLE_SCOPE.id] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MILESTONE 3: Object Oriented Programming
            EduNovaRoadmapNodeBox(
                node = PythonRoadmapStagingData.OOP_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.OOP_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.OOP_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.OOP_CENTER_NODE) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.OOP_CENTER_NODE.id] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // OOP Subtopics & Right-wing Environments + Static Typing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Spacer (matching 180dp column)
                Spacer(modifier = Modifier.width(180.dp))

                // Center Column: OOP Subtopics
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_CLASSES,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CLASSES.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CLASSES.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CLASSES) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CLASSES.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_METHODS,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_METHODS.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_METHODS.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_METHODS) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_METHODS.id] = it }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_INHERITANCE,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_INHERITANCE.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_INHERITANCE.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_INHERITANCE) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_INHERITANCE.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_ENCAPSULATION,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ENCAPSULATION.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ENCAPSULATION.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ENCAPSULATION) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_ENCAPSULATION.id] = it }
                        )
                    }
                }

                // Right Column: Environments & Static Typing
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Environments Group Container
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFD4E5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_PIPENV,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PIPENV.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PIPENV.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PIPENV) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PIPENV.id] = it }
                            )
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_VIRTUALENV,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_VIRTUALENV.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_VIRTUALENV.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_VIRTUALENV) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_VIRTUALENV.id] = it }
                            )
                            EduNovaRoadmapNodeBox(
                                node = PythonRoadmapStagingData.NODE_PYENV,
                                isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYENV.id,
                                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYENV.id),
                                onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYENV) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYENV.id] = it }
                            )
                            Text(
                                text = "Environments",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = EduNovaTextSecondary
                            )
                        }
                    }

                    // Static Typing Milestone
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_STATIC_TYPING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_STATIC_TYPING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_STATIC_TYPING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_STATIC_TYPING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_STATIC_TYPING.id] = it }
                    )

                    // Static Typing Subtopics
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_TYPING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TYPING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TYPING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TYPING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_TYPING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_MYPY,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_MYPY.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_MYPY.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_MYPY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_MYPY.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PYRIGHT,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYRIGHT.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYRIGHT.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYRIGHT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYRIGHT.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PYRE,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYRE.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYRE.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYRE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYRE.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PYDANTIC,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYDANTIC.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYDANTIC.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYDANTIC) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYDANTIC.id] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MILESTONE 4: Concurrency Center
            EduNovaRoadmapNodeBox(
                node = PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE,
                isSelected = selectedNodeId == PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id,
                isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id),
                onClick = { onSelectNode(PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.CONCURRENCY_CENTER_NODE.id] = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lower Section: Left Frameworks, Center Concurrency Subtopics, Right Formatting & File Handling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Web Frameworks
                Column(
                    modifier = Modifier.width(190.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_LEARN_FRAMEWORK.id] = it }
                    )

                    // Synchronous & Asynchronous Frameworks Container
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFD4E5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_PLOTLY_DASH,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PLOTLY_DASH.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PLOTLY_DASH.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PLOTLY_DASH) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PLOTLY_DASH.id] = it }
                                )
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_GEVENT,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_GEVENT.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_GEVENT.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_GEVENT) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_GEVENT.id] = it }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_PYRAMID,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYRAMID.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYRAMID.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYRAMID) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYRAMID.id] = it }
                                )
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_AIOHTTP,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_AIOHTTP.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_AIOHTTP.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_AIOHTTP) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_AIOHTTP.id] = it }
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_TORNADO,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TORNADO.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TORNADO.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TORNADO) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_TORNADO.id] = it }
                                )
                                EduNovaRoadmapNodeBox(
                                    node = PythonRoadmapStagingData.NODE_SANIC,
                                    isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SANIC.id,
                                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SANIC.id),
                                    onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SANIC) },
                                    compact = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SANIC.id] = it }
                                )
                            }
                        }
                    }

                    // Bottom Row: Modern APIs (FastAPI, Django, Flask)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_FASTAPI,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FASTAPI.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FASTAPI.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FASTAPI) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_FASTAPI.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_DJANGO,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DJANGO.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DJANGO.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DJANGO) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_DJANGO.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_FLASK,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FLASK.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FLASK.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FLASK) },
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_FLASK.id] = it }
                        )
                    }
                }

                // Center Column: Concurrency Subtopics
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_MULTIPROCESSING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_MULTIPROCESSING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_MULTIPROCESSING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_MULTIPROCESSING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_MULTIPROCESSING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_ASYNCHRONY,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_ASYNCHRONY.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_ASYNCHRONY.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_ASYNCHRONY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_ASYNCHRONY.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_GIL,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_GIL.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_GIL.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_GIL) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_GIL.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_THREADING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_THREADING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_THREADING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_THREADING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_THREADING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_GLOB,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_GLOB.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_GLOB.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_GLOB) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_GLOB.id] = it }
                    )
                }

                // Right Column: Code Formatting & File Handling
                Column(
                    modifier = Modifier.width(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_CODE_FORMATTING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_CODE_FORMATTING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_CODE_FORMATTING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_CODE_FORMATTING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_CODE_FORMATTING.id] = it }
                    )

                    // Linters & Formatters
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_YAPF,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_YAPF.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_YAPF.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_YAPF) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_YAPF.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_BLACK,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_BLACK.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_BLACK.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_BLACK) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_BLACK.id] = it }
                        )
                        EduNovaRoadmapNodeBox(
                            node = PythonRoadmapStagingData.NODE_RUFF,
                            isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_RUFF.id,
                            isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_RUFF.id),
                            onClick = { onSelectNode(PythonRoadmapStagingData.NODE_RUFF) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_RUFF.id] = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // File Handling & Documentation
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_SPHINX,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_SPHINX.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_SPHINX.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_SPHINX) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_SPHINX.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_FILE_HANDLING,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_FILE_HANDLING.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_FILE_HANDLING.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_FILE_HANDLING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_FILE_HANDLING.id] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MILESTONE 5: Testing with Right Branch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom Related Roadmaps Card (Left Side)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD4E5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .width(220.dp)
                        .onGloballyPositioned { nodeCoords["related_roadmaps_card"] = it }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Have a look at the following related roadmaps",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = EduNovaTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Backend", "Django", "Python for Data Analysis").forEach { badgeTitle ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF24777B)
                                ) {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Testing Milestone Node
                EduNovaRoadmapNodeBox(
                    node = PythonRoadmapStagingData.TESTING_CENTER_NODE,
                    isSelected = selectedNodeId == PythonRoadmapStagingData.TESTING_CENTER_NODE.id,
                    isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.TESTING_CENTER_NODE.id),
                    onClick = { onSelectNode(PythonRoadmapStagingData.TESTING_CENTER_NODE) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.TESTING_CENTER_NODE.id] = it }
                )

                // Right Branch: Testing Frameworks
                Column(
                    modifier = Modifier.width(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_TOX,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_TOX.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_TOX.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_TOX) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_TOX.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_UNITTEST,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_UNITTEST.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_UNITTEST.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_UNITTEST) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_UNITTEST.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_DOCTEST,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_DOCTEST.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_DOCTEST.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_DOCTEST) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_DOCTEST.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = PythonRoadmapStagingData.NODE_PYTEST,
                        isSelected = selectedNodeId == PythonRoadmapStagingData.NODE_PYTEST.id,
                        isCompleted = completedTopicIds.contains(PythonRoadmapStagingData.NODE_PYTEST.id),
                        onClick = { onSelectNode(PythonRoadmapStagingData.NODE_PYTEST) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[PythonRoadmapStagingData.NODE_PYTEST.id] = it }
                    )
                }
            }
        }
    }
}
