package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import nl.utwente.processing.pmd.symbols.ProcessingApplet

class ReservedMethodNamesRule: AbstractJavaRule() {

    override fun visit(node: ASTClassOrInterfaceDeclaration, data: Any): Any? {
        if (node.isNested) {
            for (method in node.findDescendantsOfType(ASTMethodDeclaration::class.java)) {
                if (checkIfReserved(method.name)) {
                    this.addViolationWithMessage(data, node, message, kotlin.arrayOf(method.name))
                }
            }
        }
        return super.visit(node, data)
    }

    private fun checkIfReserved(targetMethod: String): Boolean {
        if (ProcessingApplet.SETUP_METHOD_SIGNATURE.startsWith(targetMethod)) {
            return true
        }
        if (ProcessingApplet.DRAW_METHOD_SIGNATURE.startsWith(targetMethod)) {
            return true
        }
        ProcessingApplet.EVENT_METHOD_SIGNATURES.forEach {
            if (it.startsWith(targetMethod)) {
                return true
            }
        }
        return false
    }

}