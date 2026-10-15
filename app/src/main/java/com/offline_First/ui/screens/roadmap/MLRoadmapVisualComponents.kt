package com.offline_First.ui.screens.roadmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.offline_First.data.staging.MLRoadmapStagingData
import com.offline_First.data.staging.VisualRoadmapNode

/**
 * Connected Visual Diagram for Machine Learning Roadmap.
 * Strictly reflects Screenshots 3 & 4 learning sequence while adopting
 * Screenshot 5's EduNova Java visual language (mint/teal palette, compact boxes, orthogonal lines).
 */
@Composable
fun MLVisualRoadmapDiagram(
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

            // Root -> Intro
            val rootB = getBounds(MLRoadmapStagingData.NODE_ROOT.id)
            val introB = getBounds(MLRoadmapStagingData.NODE_INTRO.id)
            if (rootB != null && introB != null) {
                drawStraightLine(Offset(rootB.center.x, rootB.bottom), Offset(introB.center.x, introB.top))
            }

            // Intro -> Right wing (What is ML Eng, ML vs AI, Skills)
            introB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    MLRoadmapStagingData.NODE_WHAT_IS_ML_ENG.id,
                    MLRoadmapStagingData.NODE_ML_VS_AI.id,
                    MLRoadmapStagingData.NODE_SKILLS_RESP.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }

            // Intro -> Math Foundations
            val mathB = getBounds(MLRoadmapStagingData.NODE_MATH_FOUNDATIONS.id)
            if (introB != null && mathB != null) {
                drawStraightLine(Offset(introB.center.x, introB.bottom), Offset(mathB.center.x, mathB.top))
            }

            // Math -> Linear Algebra & Calculus & Stats & Prob
            val laB = getBounds(MLRoadmapStagingData.NODE_LINEAR_ALGEBRA.id)
            val calcB = getBounds(MLRoadmapStagingData.NODE_CALCULUS.id)
            if (mathB != null && laB != null) {
                drawStraightLine(Offset(mathB.center.x, mathB.bottom), Offset(laB.center.x, laB.top))
            }
            if (laB != null && calcB != null) {
                drawOrthogonalPath(Offset(laB.left, laB.center.y), Offset(calcB.right, calcB.center.y))
            }

            // LA -> Right wing (matrix ops, vectors, SVD, etc.)
            laB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    MLRoadmapStagingData.NODE_MATRIX_OPS.id,
                    MLRoadmapStagingData.NODE_SCALARS_VECTORS.id,
                    MLRoadmapStagingData.NODE_SVD.id,
                    MLRoadmapStagingData.NODE_DETERMINANTS.id,
                    MLRoadmapStagingData.NODE_EIGENVALUES.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }

            // Calculus -> Discrete Math -> Statistics
            val discB = getBounds(MLRoadmapStagingData.NODE_DISCRETE_MATH.id)
            if (calcB != null && discB != null) {
                drawStraightLine(Offset(calcB.center.x, calcB.bottom), Offset(discB.center.x, discB.top))
            }

            val statsB = getBounds(MLRoadmapStagingData.NODE_STATISTICS.id)
            if (discB != null && statsB != null) {
                drawStraightLine(Offset(discB.center.x, discB.bottom), Offset(statsB.center.x, statsB.top))
            }

            // Stats -> Probability
            val probB = getBounds(MLRoadmapStagingData.NODE_PROBABILITY.id)
            if (statsB != null && probB != null) {
                drawStraightLine(Offset(statsB.right, statsB.center.y), Offset(probB.left, probB.center.y))
            }

            // Stats / Prob -> Programming Fundamentals
            val progB = getBounds(MLRoadmapStagingData.NODE_PROGRAMMING_FUND.id)
            if (probB != null && progB != null) {
                drawOrthogonalPath(Offset(probB.center.x, probB.bottom), Offset(progB.center.x, progB.top))
            }

            // Programming -> Python & OOP & Essential Libs
            val pyB = getBounds(MLRoadmapStagingData.NODE_PYTHON.id)
            val oopB = getBounds(MLRoadmapStagingData.NODE_OOP.id)
            val libsB = getBounds(MLRoadmapStagingData.NODE_ESSENTIAL_LIBS.id)
            if (progB != null && pyB != null) {
                drawStraightLine(Offset(progB.center.x, progB.bottom), Offset(pyB.center.x, pyB.top))
            }
            if (pyB != null && oopB != null) {
                drawOrthogonalPath(Offset(pyB.left, pyB.center.y), Offset(oopB.right, oopB.center.y))
            }
            if (oopB != null && libsB != null) {
                drawStraightLine(Offset(oopB.center.x, oopB.bottom), Offset(libsB.center.x, libsB.top))
            }

            // Programming / Libs -> Data Collection & Data Cleaning
            val dataColB = getBounds(MLRoadmapStagingData.NODE_DATA_COLLECTION.id)
            if (libsB != null && dataColB != null) {
                drawStraightLine(Offset(libsB.center.x, libsB.bottom), Offset(dataColB.center.x, dataColB.top))
            }

            val preprocB = getBounds(MLRoadmapStagingData.NODE_PREPROCESSING.id)
            if (dataColB != null && preprocB != null) {
                drawOrthogonalPath(Offset(dataColB.right, dataColB.center.y), Offset(preprocB.left, preprocB.center.y))
            }

            // Data Prep -> Types of ML & Scikit-learn
            val typesMlB = getBounds(MLRoadmapStagingData.NODE_TYPES_OF_ML.id)
            val sklB = getBounds(MLRoadmapStagingData.NODE_SCIKIT_LEARN.id)
            if (dataColB != null && typesMlB != null) {
                drawStraightLine(Offset(dataColB.center.x, dataColB.bottom), Offset(typesMlB.center.x, typesMlB.top))
            }
            if (typesMlB != null && sklB != null) {
                drawStraightLine(Offset(typesMlB.center.x, typesMlB.bottom), Offset(sklB.center.x, sklB.top))
            }

            // Scikit-learn -> Supervised & Unsupervised
            val supB = getBounds(MLRoadmapStagingData.NODE_SUPERVISED.id)
            val unsupB = getBounds(MLRoadmapStagingData.NODE_UNSUPERVISED.id)
            if (sklB != null && supB != null) {
                drawOrthogonalPath(Offset(sklB.left, sklB.bottom), Offset(supB.center.x, supB.top))
            }
            if (sklB != null && unsupB != null) {
                drawOrthogonalPath(Offset(sklB.right, sklB.bottom), Offset(unsupB.center.x, unsupB.top))
            }

            // Supervised -> Classification & Regression
            val classB = getBounds(MLRoadmapStagingData.NODE_CLASSIFICATION.id)
            val regB = getBounds(MLRoadmapStagingData.NODE_REGRESSION.id)
            if (supB != null && classB != null) {
                drawStraightLine(Offset(supB.center.x, supB.bottom), Offset(classB.center.x, classB.top))
            }
            if (classB != null && regB != null) {
                drawStraightLine(Offset(classB.right, classB.center.y), Offset(regB.left, regB.center.y))
            }

            // Supervised & Unsupervised -> Model Evaluation
            val evalB = getBounds(MLRoadmapStagingData.NODE_MODEL_EVALUATION.id)
            if (regB != null && evalB != null) {
                drawOrthogonalPath(Offset(regB.center.x, regB.bottom), Offset(evalB.center.x, evalB.top))
            }

            // Evaluation -> Deep Learning
            val dlB = getBounds(MLRoadmapStagingData.NODE_DEEP_LEARNING.id)
            if (evalB != null && dlB != null) {
                drawStraightLine(Offset(evalB.center.x, evalB.bottom), Offset(dlB.center.x, dlB.top))
            }

            // Deep Learning -> CNN, RNN, Transformers, GANs
            val cnnB = getBounds(MLRoadmapStagingData.NODE_CNN.id)
            val rnnB = getBounds(MLRoadmapStagingData.NODE_RNN.id)
            val attB = getBounds(MLRoadmapStagingData.NODE_ATTENTION.id)
            if (dlB != null && cnnB != null) {
                drawOrthogonalPath(Offset(dlB.left, dlB.bottom), Offset(cnnB.center.x, cnnB.top))
            }
            if (dlB != null && rnnB != null) {
                drawStraightLine(Offset(dlB.center.x, dlB.bottom), Offset(rnnB.center.x, rnnB.top))
            }
            if (dlB != null && attB != null) {
                drawOrthogonalPath(Offset(dlB.right, dlB.bottom), Offset(attB.center.x, attB.top))
            }

            // Deep Learning -> Advanced Concepts (Explainable AI & NLP)
            val advB = getBounds(MLRoadmapStagingData.NODE_ADVANCED_ML.id)
            val nlpB = getBounds(MLRoadmapStagingData.NODE_NLP.id)
            if (attB != null && advB != null) {
                drawStraightLine(Offset(attB.center.x, attB.bottom), Offset(advB.center.x, advB.top))
            }
            if (advB != null && nlpB != null) {
                drawStraightLine(Offset(advB.center.x, advB.bottom), Offset(nlpB.center.x, nlpB.top))
            }

            // NLP -> Subtopics
            nlpB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    MLRoadmapStagingData.NODE_TOKENIZATION.id,
                    MLRoadmapStagingData.NODE_LEMMATIZATION.id,
                    MLRoadmapStagingData.NODE_EMBEDDINGS.id,
                    MLRoadmapStagingData.NODE_ATTENTION_MODELS.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }
        }

        // --- 2. FOREGROUND COMPOSE NODE BOXES ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Root Node
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduNovaMilestoneBoxBg,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_ROOT.id] = it }
            ) {
                Text(
                    text = "Machine Learning",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // =================================================================
            // MILESTONE 1: Introduction
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(160.dp))

                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_INTRO,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_INTRO.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_INTRO.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_INTRO) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_INTRO.id] = it }
                )

                // Right Wing: What is ML Eng?, etc.
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        MLRoadmapStagingData.NODE_WHAT_IS_ML_ENG,
                        MLRoadmapStagingData.NODE_ML_VS_AI,
                        MLRoadmapStagingData.NODE_SKILLS_RESP
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            // =================================================================
            // MILESTONE 2: Mathematical Foundations
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_MATH_FOUNDATIONS,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_MATH_FOUNDATIONS.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_MATH_FOUNDATIONS.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_MATH_FOUNDATIONS) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_MATH_FOUNDATIONS.id] = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Calculus & Discrete Math (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_CALCULUS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_CALCULUS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_CALCULUS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_CALCULUS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_CALCULUS.id] = it }
                    )

                    listOf(
                        MLRoadmapStagingData.NODE_DERIVATIVES,
                        MLRoadmapStagingData.NODE_CHAIN_RULE,
                        MLRoadmapStagingData.NODE_GRADIENT_HESSIAN,
                        MLRoadmapStagingData.NODE_DISCRETE_MATH
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }

                // Linear Algebra (Center)
                Column(
                    modifier = Modifier.width(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_LINEAR_ALGEBRA,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_LINEAR_ALGEBRA.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_LINEAR_ALGEBRA.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_LINEAR_ALGEBRA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_LINEAR_ALGEBRA.id] = it }
                    )
                }

                // Linear Algebra Subtopics (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        MLRoadmapStagingData.NODE_MATRIX_OPS,
                        MLRoadmapStagingData.NODE_SCALARS_VECTORS,
                        MLRoadmapStagingData.NODE_SVD,
                        MLRoadmapStagingData.NODE_DETERMINANTS,
                        MLRoadmapStagingData.NODE_EIGENVALUES
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            // Statistics & Probability Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_STATISTICS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_STATISTICS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_STATISTICS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_STATISTICS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_STATISTICS.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_BASIC_STATS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_BASIC_STATS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_BASIC_STATS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_BASIC_STATS) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_BASIC_STATS.id] = it }
                    )
                }

                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_PROBABILITY,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_PROBABILITY.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_PROBABILITY.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_PROBABILITY) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_PROBABILITY.id] = it }
                )

                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        MLRoadmapStagingData.NODE_BASICS_PROB,
                        MLRoadmapStagingData.NODE_BAYES_THEOREM,
                        MLRoadmapStagingData.NODE_DISTRIBUTIONS
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 3: Programming Fundamentals
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_PROGRAMMING_FUND,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_PROGRAMMING_FUND.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_PROGRAMMING_FUND.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_PROGRAMMING_FUND) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_PROGRAMMING_FUND.id] = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // OOP & Essential Libraries (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_OOP,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_OOP.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_OOP.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_OOP) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_OOP.id] = it }
                    )

                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_ESSENTIAL_LIBS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_ESSENTIAL_LIBS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_ESSENTIAL_LIBS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_ESSENTIAL_LIBS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_ESSENTIAL_LIBS.id] = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(MLRoadmapStagingData.NODE_NUMPY, MLRoadmapStagingData.NODE_PANDAS).forEach { node ->
                            EduNovaRoadmapNodeBox(
                                node = node,
                                isSelected = selectedNodeId == node.id,
                                isCompleted = completedTopicIds.contains(node.id),
                                onClick = { onSelectNode(node) },
                                compact = true,
                                modifier = Modifier.weight(1f).onGloballyPositioned { nodeCoords[node.id] = it }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(MLRoadmapStagingData.NODE_MATPLOTLIB, MLRoadmapStagingData.NODE_SEABORN).forEach { node ->
                            EduNovaRoadmapNodeBox(
                                node = node,
                                isSelected = selectedNodeId == node.id,
                                isCompleted = completedTopicIds.contains(node.id),
                                onClick = { onSelectNode(node) },
                                compact = true,
                                modifier = Modifier.weight(1f).onGloballyPositioned { nodeCoords[node.id] = it }
                            )
                        }
                    }
                }

                // Python Center
                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_PYTHON,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_PYTHON.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_PYTHON.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_PYTHON) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_PYTHON.id] = it }
                )

                // Python Subtopics (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        MLRoadmapStagingData.NODE_PY_SYNTAX,
                        MLRoadmapStagingData.NODE_PY_VARS,
                        MLRoadmapStagingData.NODE_PY_DATA_STRUCTS,
                        MLRoadmapStagingData.NODE_PY_LOOPS,
                        MLRoadmapStagingData.NODE_PY_CONDITIONALS
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 4: Data Collection & Preparation
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Data Collection & Sources
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_DATA_COLLECTION,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_DATA_COLLECTION.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_DATA_COLLECTION.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_DATA_COLLECTION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_DATA_COLLECTION.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_DATA_SOURCES,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_DATA_SOURCES.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_DATA_SOURCES.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_DATA_SOURCES) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_DATA_SOURCES.id] = it }
                    )
                }

                // Data Formats (Center)
                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_DATA_FORMATS,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_DATA_FORMATS.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_DATA_FORMATS.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_DATA_FORMATS) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_DATA_FORMATS.id] = it }
                )

                // Preprocessing Techniques (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_PREPROCESSING,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_PREPROCESSING.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_PREPROCESSING.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_PREPROCESSING) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_PREPROCESSING.id] = it }
                    )
                    listOf(
                        MLRoadmapStagingData.NODE_DATA_CLEANING,
                        MLRoadmapStagingData.NODE_FEATURE_ENG,
                        MLRoadmapStagingData.NODE_SCALING_NORM
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 5: Machine Learning Core & Scikit-learn
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_TYPES_OF_ML,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_TYPES_OF_ML.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_TYPES_OF_ML.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_TYPES_OF_ML) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_TYPES_OF_ML.id] = it }
            )

            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_SCIKIT_LEARN,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_SCIKIT_LEARN.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_SCIKIT_LEARN.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_SCIKIT_LEARN) },
                modifier = Modifier
                    .width(170.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_SCIKIT_LEARN.id] = it }
            )

            // Supervised vs Unsupervised Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Classification
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_CLASSIFICATION,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_CLASSIFICATION.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_CLASSIFICATION.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_CLASSIFICATION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_CLASSIFICATION.id] = it }
                    )
                    listOf(
                        MLRoadmapStagingData.NODE_KNN,
                        MLRoadmapStagingData.NODE_LOGISTIC_REG,
                        MLRoadmapStagingData.NODE_SVM,
                        MLRoadmapStagingData.NODE_DECISION_TREES,
                        MLRoadmapStagingData.NODE_GRADIENT_BOOSTING
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }

                // Supervised (Center) & Regression
                Column(
                    modifier = Modifier.width(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_SUPERVISED,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_SUPERVISED.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_SUPERVISED.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_SUPERVISED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_SUPERVISED.id] = it }
                    )

                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_REGRESSION,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_REGRESSION.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_REGRESSION.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_REGRESSION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_REGRESSION.id] = it }
                    )

                    listOf(
                        MLRoadmapStagingData.NODE_LIN_REG,
                        MLRoadmapStagingData.NODE_POLY_REG,
                        MLRoadmapStagingData.NODE_RIDGE_LASSO,
                        MLRoadmapStagingData.NODE_ELASTICNET
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }

                // Unsupervised & Clustering (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_UNSUPERVISED,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_UNSUPERVISED.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_UNSUPERVISED.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_UNSUPERVISED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_UNSUPERVISED.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_CLUSTERING,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_CLUSTERING.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_CLUSTERING.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_CLUSTERING) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_CLUSTERING.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_REINFORCEMENT,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_REINFORCEMENT.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_REINFORCEMENT.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_REINFORCEMENT) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_REINFORCEMENT.id] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 6: Model Evaluation
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_MODEL_EVALUATION,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_MODEL_EVALUATION.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_MODEL_EVALUATION.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_MODEL_EVALUATION) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_MODEL_EVALUATION.id] = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_VALIDATION_TECH,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_VALIDATION_TECH.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_VALIDATION_TECH.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_VALIDATION_TECH) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_VALIDATION_TECH.id] = it }
                )

                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_METRICS,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_METRICS.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_METRICS.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_METRICS) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_METRICS.id] = it }
                )

                Spacer(modifier = Modifier.width(160.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 7: Deep Learning
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_DEEP_LEARNING,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_DEEP_LEARNING.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_DEEP_LEARNING.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_DEEP_LEARNING) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_DEEP_LEARNING.id] = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // CNN (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_CNN,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_CNN.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_CNN.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_CNN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_CNN.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_CNN_APPS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_CNN_APPS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_CNN_APPS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_CNN_APPS) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_CNN_APPS.id] = it }
                    )
                }

                // RNN (Center)
                Column(
                    modifier = Modifier.width(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_RNN,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_RNN.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_RNN.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_RNN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_RNN.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_AUTOENCODERS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_AUTOENCODERS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_AUTOENCODERS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_AUTOENCODERS) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_AUTOENCODERS.id] = it }
                    )
                }

                // Attention & Transformers (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_ATTENTION,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_ATTENTION.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_ATTENTION.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_ATTENTION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_ATTENTION.id] = it }
                    )
                    EduNovaRoadmapNodeBox(
                        node = MLRoadmapStagingData.NODE_GANS,
                        isSelected = selectedNodeId == MLRoadmapStagingData.NODE_GANS.id,
                        isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_GANS.id),
                        onClick = { onSelectNode(MLRoadmapStagingData.NODE_GANS) },
                        compact = true,
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_GANS.id] = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 8: Advanced Concepts in ML (Explainable AI & NLP)
            // =================================================================
            EduNovaRoadmapNodeBox(
                node = MLRoadmapStagingData.NODE_ADVANCED_ML,
                isSelected = selectedNodeId == MLRoadmapStagingData.NODE_ADVANCED_ML.id,
                isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_ADVANCED_ML.id),
                onClick = { onSelectNode(MLRoadmapStagingData.NODE_ADVANCED_ML) },
                modifier = Modifier
                    .width(220.dp)
                    .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_ADVANCED_ML.id] = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_EXPLAINABLE_AI,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_EXPLAINABLE_AI.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_EXPLAINABLE_AI.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_EXPLAINABLE_AI) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_EXPLAINABLE_AI.id] = it }
                )

                EduNovaRoadmapNodeBox(
                    node = MLRoadmapStagingData.NODE_NLP,
                    isSelected = selectedNodeId == MLRoadmapStagingData.NODE_NLP.id,
                    isCompleted = completedTopicIds.contains(MLRoadmapStagingData.NODE_NLP.id),
                    onClick = { onSelectNode(MLRoadmapStagingData.NODE_NLP) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[MLRoadmapStagingData.NODE_NLP.id] = it }
                )

                // NLP Subtopics (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        MLRoadmapStagingData.NODE_TOKENIZATION,
                        MLRoadmapStagingData.NODE_LEMMATIZATION,
                        MLRoadmapStagingData.NODE_EMBEDDINGS,
                        MLRoadmapStagingData.NODE_ATTENTION_MODELS
                    ).forEach { node ->
                        EduNovaRoadmapNodeBox(
                            node = node,
                            isSelected = selectedNodeId == node.id,
                            isCompleted = completedTopicIds.contains(node.id),
                            onClick = { onSelectNode(node) },
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { nodeCoords[node.id] = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
