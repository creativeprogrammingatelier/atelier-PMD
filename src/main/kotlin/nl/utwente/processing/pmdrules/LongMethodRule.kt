package nl.utwente.processing.pmdrules

import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.metrics.MetricsUtil

/**
 * Class which implements the long method smell as PMD rule. Based on the NcssMethodCountRule in PMD.
 */
class LongMethodRule : AbstractJavaRule() {
    override fun visit(node: ASTMethodDeclaration, data: Any?): Any? {
        val ncss = MetricsUtil.computeMetric(JavaOperationMetricKey.NCSS, node)
        if (ncss > 50.0) {
            this.addViolationWithMessage(data, node, message, arrayOf(node.name, ncss))
        }
        return super.visit(node, data)
    }
}