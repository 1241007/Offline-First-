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
import com.offline_First.data.staging.SqlRoadmapStagingData
import com.offline_First.data.staging.VisualRoadmapNode

/**
 * Connected Visual Diagram for SQL Roadmap.
 * Strictly reflects Screenshot 2 learning flow and connections while adopting
 * Screenshot 5's EduNova Java visual language (mint/teal palette, compact boxes, orthogonal lines).
 */
@Composable
fun SqlVisualRoadmapDiagram(
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

            // Top root -> Learn the Basics
            val rootB = getBounds(SqlRoadmapStagingData.NODE_ROOT.id)
            val basicsB = getBounds(SqlRoadmapStagingData.NODE_BASICS.id)
            if (rootB != null && basicsB != null) {
                drawStraightLine(Offset(rootB.center.x, rootB.bottom), Offset(basicsB.center.x, basicsB.top))
            }

            // Learn the Basics -> Right wing (RDBMS concepts)
            basicsB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    SqlRoadmapStagingData.NODE_WHAT_ARE_RDBMS.id,
                    SqlRoadmapStagingData.NODE_RDBMS_BENEFITS.id,
                    SqlRoadmapStagingData.NODE_SQL_VS_NOSQL.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(rightAnchor, Offset(targetB.left, targetB.center.y), isLeft = false)
                    }
                }
            }

            // Basics -> Basic SQL Syntax
            val syntaxB = getBounds(SqlRoadmapStagingData.NODE_BASIC_SYNTAX.id)
            if (basicsB != null && syntaxB != null) {
                drawStraightLine(Offset(basicsB.center.x, basicsB.bottom), Offset(syntaxB.center.x, syntaxB.top))
            }

            // Syntax -> Left wing (Keywords, Data Types, Operators)
            syntaxB?.let { b ->
                val leftAnchor = Offset(b.left, b.center.y)
                listOf(
                    SqlRoadmapStagingData.NODE_KEYWORDS.id,
                    SqlRoadmapStagingData.NODE_DATA_TYPES.id,
                    SqlRoadmapStagingData.NODE_OPERATORS.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(leftAnchor, Offset(targetB.right, targetB.center.y), isLeft = true)
                    }
                }
            }

            // Syntax -> DDL
            val ddlB = getBounds(SqlRoadmapStagingData.NODE_DDL.id)
            if (syntaxB != null && ddlB != null) {
                drawStraightLine(Offset(syntaxB.right, syntaxB.center.y), Offset(ddlB.left, ddlB.center.y))
            }

            // DDL -> Right branch (Truncate, Alter, Create, Drop)
            ddlB?.let { b ->
                getBounds(SqlRoadmapStagingData.NODE_CREATE_TABLE.id)?.let { targetB ->
                    drawStraightLine(Offset(b.center.x, b.bottom), Offset(targetB.center.x, targetB.top), isDashed = true)
                }
            }

            // Syntax / DDL -> DML
            val dmlB = getBounds(SqlRoadmapStagingData.NODE_DML.id)
            if (syntaxB != null && dmlB != null) {
                drawStraightLine(Offset(syntaxB.center.x, syntaxB.bottom), Offset(dmlB.center.x, dmlB.top))
            }

            // DML -> Aggregate Queries (Left)
            val aggB = getBounds(SqlRoadmapStagingData.NODE_AGGREGATES.id)
            if (dmlB != null && aggB != null) {
                drawOrthogonalPath(Offset(dmlB.left, dmlB.center.y), Offset(aggB.right, aggB.center.y))
            }

            // Aggregates -> subtopics
            aggB?.let { b ->
                val leftAnchor = Offset(b.left, b.center.y)
                listOf(
                    SqlRoadmapStagingData.NODE_SUM.id,
                    SqlRoadmapStagingData.NODE_COUNT.id,
                    SqlRoadmapStagingData.NODE_AVG.id,
                    SqlRoadmapStagingData.NODE_MIN_MAX.id,
                    SqlRoadmapStagingData.NODE_GROUP_BY.id,
                    SqlRoadmapStagingData.NODE_HAVING.id
                ).forEach { id ->
                    getBounds(id)?.let { targetB ->
                        drawFanningDottedCurve(leftAnchor, Offset(targetB.right, targetB.center.y), isLeft = true)
                    }
                }
            }

            // DML -> Data Constraints (Right)
            val constrB = getBounds(SqlRoadmapStagingData.NODE_CONSTRAINTS.id)
            if (dmlB != null && constrB != null) {
                drawStraightLine(Offset(dmlB.center.x, dmlB.bottom), Offset(constrB.center.x, constrB.top))
            }

            // Constraints -> Subqueries (Left) and JOINs (Right)
            val subqB = getBounds(SqlRoadmapStagingData.NODE_SUBQUERIES.id)
            val joinsB = getBounds(SqlRoadmapStagingData.NODE_JOINS.id)
            if (constrB != null && subqB != null) {
                drawOrthogonalPath(Offset(constrB.left, constrB.center.y), Offset(subqB.right, subqB.center.y))
            }
            if (constrB != null && joinsB != null) {
                drawStraightLine(Offset(constrB.right, constrB.center.y), Offset(joinsB.left, joinsB.center.y))
            }

            // Subqueries & JOINs -> Advanced Functions & Views
            val advFuncB = getBounds(SqlRoadmapStagingData.NODE_ADV_FUNCTIONS.id)
            val viewsB = getBounds(SqlRoadmapStagingData.NODE_VIEWS.id)
            if (subqB != null && advFuncB != null) {
                drawStraightLine(Offset(subqB.center.x, subqB.bottom), Offset(advFuncB.center.x, advFuncB.top))
            }
            if (advFuncB != null && viewsB != null) {
                drawStraightLine(Offset(advFuncB.center.x, advFuncB.bottom), Offset(viewsB.center.x, viewsB.top))
            }

            // Views -> Indexes & Transactions & Stored Procedures
            val idxB = getBounds(SqlRoadmapStagingData.NODE_INDEXES.id)
            val transB = getBounds(SqlRoadmapStagingData.NODE_TRANSACTIONS.id)
            val secB = getBounds(SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY.id)
            val spB = getBounds(SqlRoadmapStagingData.NODE_STORED_PROCS.id)
            if (viewsB != null && secB != null) {
                drawStraightLine(Offset(viewsB.center.x, viewsB.bottom), Offset(secB.center.x, secB.top))
            }
            if (secB != null && spB != null) {
                drawStraightLine(Offset(secB.left, secB.center.y), Offset(spB.right, spB.center.y))
            }
            if (secB != null && transB != null) {
                drawStraightLine(Offset(secB.right, secB.center.y), Offset(transB.left, transB.center.y))
            }
            if (transB != null && idxB != null) {
                drawStraightLine(Offset(transB.center.x, transB.top), Offset(idxB.center.x, idxB.bottom))
            }

            // Stored Procs -> Performance Optimization
            val perfB = getBounds(SqlRoadmapStagingData.NODE_PERFORMANCE_OPT.id)
            if (spB != null && perfB != null) {
                drawStraightLine(Offset(spB.center.x, spB.bottom), Offset(perfB.center.x, perfB.top))
            }

            // Performance Opt & Sec -> Advanced SQL
            val advSqlB = getBounds(SqlRoadmapStagingData.NODE_ADVANCED_SQL.id)
            if (perfB != null && advSqlB != null) {
                drawOrthogonalPath(Offset(perfB.right, perfB.bottom), Offset(advSqlB.left, advSqlB.center.y))
            }

            // Advanced SQL -> Window Functions, CTEs, etc.
            advSqlB?.let { b ->
                val rightAnchor = Offset(b.right, b.center.y)
                listOf(
                    SqlRoadmapStagingData.NODE_WINDOW_FUNCTIONS.id,
                    SqlRoadmapStagingData.NODE_RECURSIVE_QUERIES.id,
                    SqlRoadmapStagingData.NODE_PIVOT_UNPIVOT.id,
                    SqlRoadmapStagingData.NODE_CTES.id
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
            // Root Node: SQL
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EduNovaMilestoneBoxBg,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_ROOT.id] = it }
            ) {
                Text(
                    text = "SQL",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // =================================================================
            // MILESTONE 1: Learn the Basics (Right branch RDBMS)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(160.dp))

                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_BASICS,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_BASICS.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_BASICS.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_BASICS) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_BASICS.id] = it }
                )

                // Right Wing: Relational DB Concepts
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SqlRoadmapStagingData.NODE_WHAT_ARE_RDBMS,
                        SqlRoadmapStagingData.NODE_RDBMS_BENEFITS,
                        SqlRoadmapStagingData.NODE_SQL_VS_NOSQL
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
            // MILESTONE 2: Basic SQL Syntax & DDL
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Wing: Keywords, Data Types, Operators
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SqlRoadmapStagingData.NODE_KEYWORDS,
                        SqlRoadmapStagingData.NODE_DATA_TYPES,
                        SqlRoadmapStagingData.NODE_OPERATORS
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

                // Center: Basic SQL Syntax
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_BASIC_SYNTAX,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_BASIC_SYNTAX.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_BASIC_SYNTAX.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_BASIC_SYNTAX) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_BASIC_SYNTAX.id] = it }
                )

                // Right: DDL
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_DDL,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_DDL.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_DDL.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_DDL) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_DDL.id] = it }
                )
            }

            // Statements & DDL Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Statements (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(SqlRoadmapStagingData.NODE_SELECT, SqlRoadmapStagingData.NODE_INSERT).forEach { node ->
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
                        listOf(SqlRoadmapStagingData.NODE_UPDATE, SqlRoadmapStagingData.NODE_DELETE).forEach { node ->
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

                // DML (Center)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_DML,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_DML.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_DML.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_DML) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_DML.id] = it }
                )

                // DDL Subtopics (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SqlRoadmapStagingData.NODE_CREATE_TABLE,
                        SqlRoadmapStagingData.NODE_ALTER_TABLE,
                        SqlRoadmapStagingData.NODE_DROP_TABLE,
                        SqlRoadmapStagingData.NODE_TRUNCATE_TABLE
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
            // MILESTONE 3: Aggregate Queries & Data Constraints
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aggregate Queries (Left)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_AGGREGATES,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_AGGREGATES.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_AGGREGATES.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_AGGREGATES) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_AGGREGATES.id] = it }
                )

                // Center Spacer
                Spacer(modifier = Modifier.width(170.dp))

                // Data Constraints (Right)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_CONSTRAINTS,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_CONSTRAINTS.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_CONSTRAINTS.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_CONSTRAINTS) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_CONSTRAINTS.id] = it }
                )
            }

            // Aggregate Tools & Constraints Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Aggregates subtopics (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(SqlRoadmapStagingData.NODE_SUM, SqlRoadmapStagingData.NODE_COUNT).forEach { node ->
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
                        listOf(SqlRoadmapStagingData.NODE_AVG, SqlRoadmapStagingData.NODE_MIN_MAX).forEach { node ->
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
                        listOf(SqlRoadmapStagingData.NODE_GROUP_BY, SqlRoadmapStagingData.NODE_HAVING).forEach { node ->
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

                // Subqueries (Center) & JOINs (Right)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_SUBQUERIES,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_SUBQUERIES.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_SUBQUERIES.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_SUBQUERIES) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_SUBQUERIES.id] = it }
                )

                // Constraints list (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SqlRoadmapStagingData.NODE_PRIMARY_KEY,
                        SqlRoadmapStagingData.NODE_FOREIGN_KEY,
                        SqlRoadmapStagingData.NODE_UNIQUE,
                        SqlRoadmapStagingData.NODE_NOT_NULL,
                        SqlRoadmapStagingData.NODE_CHECK
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
            // MILESTONE 4: JOIN Queries & Advanced Functions & Views
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Advanced Functions (Left)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = SqlRoadmapStagingData.NODE_ADV_FUNCTIONS,
                        isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_ADV_FUNCTIONS.id,
                        isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_ADV_FUNCTIONS.id),
                        onClick = { onSelectNode(SqlRoadmapStagingData.NODE_ADV_FUNCTIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_ADV_FUNCTIONS.id] = it }
                    )
                    listOf(
                        SqlRoadmapStagingData.NODE_STRING_FUNCS,
                        SqlRoadmapStagingData.NODE_DATETIME_FUNCS,
                        SqlRoadmapStagingData.NODE_NUMERIC_FUNCS,
                        SqlRoadmapStagingData.NODE_COALESCE
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

                // Views (Center)
                Column(
                    modifier = Modifier.width(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EduNovaRoadmapNodeBox(
                        node = SqlRoadmapStagingData.NODE_VIEWS,
                        isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_VIEWS.id,
                        isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_VIEWS.id),
                        onClick = { onSelectNode(SqlRoadmapStagingData.NODE_VIEWS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_VIEWS.id] = it }
                    )
                }

                // JOIN Queries & Types (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EduNovaRoadmapNodeBox(
                        node = SqlRoadmapStagingData.NODE_JOINS,
                        isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_JOINS.id,
                        isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_JOINS.id),
                        onClick = { onSelectNode(SqlRoadmapStagingData.NODE_JOINS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_JOINS.id] = it }
                    )
                    listOf(
                        SqlRoadmapStagingData.NODE_INNER_JOIN,
                        SqlRoadmapStagingData.NODE_LEFT_JOIN,
                        SqlRoadmapStagingData.NODE_RIGHT_JOIN,
                        SqlRoadmapStagingData.NODE_FULL_OUTER_JOIN,
                        SqlRoadmapStagingData.NODE_SELF_JOIN
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
            // MILESTONE 5: Stored Procs + Security + Transactions + Indexes
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stored Procedures (Left)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_STORED_PROCS,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_STORED_PROCS.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_STORED_PROCS.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_STORED_PROCS) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_STORED_PROCS.id] = it }
                )

                // Data Integrity & Security (Center)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_DATA_INTEGRITY_SECURITY.id] = it }
                )

                // Transactions (Right)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_TRANSACTIONS,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_TRANSACTIONS.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_TRANSACTIONS.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_TRANSACTIONS) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_TRANSACTIONS.id] = it }
                )
            }

            // Indexes row right under Transactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_INDEXES,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_INDEXES.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_INDEXES.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_INDEXES) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_INDEXES.id] = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // =================================================================
            // MILESTONE 6: Performance Optimization & Advanced SQL
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Performance Optimization (Left)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_PERFORMANCE_OPT,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_PERFORMANCE_OPT.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_PERFORMANCE_OPT.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_PERFORMANCE_OPT) },
                    modifier = Modifier
                        .width(160.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_PERFORMANCE_OPT.id] = it }
                )

                // Advanced SQL (Center)
                EduNovaRoadmapNodeBox(
                    node = SqlRoadmapStagingData.NODE_ADVANCED_SQL,
                    isSelected = selectedNodeId == SqlRoadmapStagingData.NODE_ADVANCED_SQL.id,
                    isCompleted = completedTopicIds.contains(SqlRoadmapStagingData.NODE_ADVANCED_SQL.id),
                    onClick = { onSelectNode(SqlRoadmapStagingData.NODE_ADVANCED_SQL) },
                    modifier = Modifier
                        .width(170.dp)
                        .onGloballyPositioned { nodeCoords[SqlRoadmapStagingData.NODE_ADVANCED_SQL.id] = it }
                )

                // Window Functions & CTEs (Right)
                Column(
                    modifier = Modifier.width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SqlRoadmapStagingData.NODE_WINDOW_FUNCTIONS,
                        SqlRoadmapStagingData.NODE_RECURSIVE_QUERIES,
                        SqlRoadmapStagingData.NODE_PIVOT_UNPIVOT,
                        SqlRoadmapStagingData.NODE_CTES
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
