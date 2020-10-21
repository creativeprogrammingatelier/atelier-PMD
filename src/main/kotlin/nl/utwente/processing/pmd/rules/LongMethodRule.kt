package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTName
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.metrics.MetricsUtil
import nl.utwente.processing.pmd.symbols.ProcessingApplet
import nl.utwente.processing.pmd.symbols.ProcessingAppletMethodCategory

/**
 * Class which implements the long method smell as PMD rule. Based on the NcssMethodCountRule in PMD.
 */
class LongMethodRule : AbstractJavaRule() {

    private val targetMethods = listOf<ProcessingAppletMethodCategory>(
            ProcessingAppletMethodCategory.SHAPE,
            ProcessingAppletMethodCategory.SHAPE_2D,
            ProcessingAppletMethodCategory.SHAPE_CURVES,
            ProcessingAppletMethodCategory.SHAPE_3D,
            ProcessingAppletMethodCategory.SHAPE_ATTRIBUTES,
            ProcessingAppletMethodCategory.SHAPE_VERTEX,
            ProcessingAppletMethodCategory.SHAPE_LD
    )

    override fun visit(node: ASTMethodDeclaration, data: Any?): Any? {
        val ncss = MetricsUtil.computeMetric(JavaOperationMetricKey.NCSS, node)
        val expressions = node.body.findDescendantsOfType(ASTStatementExpression::class.java)
        var blockOffset = 0.0
        var i = 0
        while (i < expressions.size) {
            val methodName = expressions[i].getFirstDescendantOfType(ASTPrimaryPrefix::class.java)
                    .getFirstDescendantOfType(ASTName::class.java).image
            if (isShapeMethod(methodName) && i < expressions.size - 1) {
                println("Block Start: $methodName")
                for (j in i + 1 until expressions.size) {
                    val nextMethodName = expressions[j].getFirstDescendantOfType(ASTPrimaryPrefix::class.java)
                            .getFirstDescendantOfType(ASTName::class.java).image
                    if (!isShapeMethod(nextMethodName)) {
                        i = j
                        break
                    }
                    blockOffset++
                }
            }
            i++
        }
        if ((ncss - blockOffset) > 50.0) {
            this.addViolationWithMessage(data, node, message, arrayOf(node.name, ncss))
        }
        return super.visit(node, data)
    }

    private fun isShapeMethod(methodName: String): Boolean {
        ProcessingApplet.DRAW_METHODS.forEach {
            if (it.name == methodName && targetMethods.contains(it.category)) {
                return true
            }
        }
        return false
    }
}