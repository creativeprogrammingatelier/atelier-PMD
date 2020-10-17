package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTName
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.java.symboltable.ClassScope
import net.sourceforge.pmd.lang.metrics.MetricsUtil

/**
 * Class which implements the long method smell as PMD rule. Based on the NcssMethodCountRule in PMD.
 */
class LongMethodRule : AbstractJavaRule() {

    override fun visit(node: ASTMethodDeclaration, data: Any?): Any? {
        val ncss = MetricsUtil.computeMetric(JavaOperationMetricKey.NCSS, node)
        val methods = node.body.findDescendantsOfType(ASTStatementExpression::class.java)
        val numBlocks = 0.0
        for (i in 0 until methods.size) {
            val methodPrefix = methods.get(i).getFirstDescendantOfType(ASTPrimaryPrefix::class.java)
                    .getFirstDescendantOfType(ASTName::class.java).image
            println(methodPrefix)
//            if () {
//
//            }
        }
        if (ncss > 50.0) {
            this.addViolationWithMessage(data, node, message, arrayOf(node.name, ncss))
        }
        return super.visit(node, data)
    }
}