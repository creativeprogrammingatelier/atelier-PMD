package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.java.symboltable.ClassScope
import nl.utwente.processing.pmd.symbols.ProcessingApplet
import nl.utwente.processing.pmd.utils.findMethod
import nl.utwente.processing.pmd.utils.isMethodCall
import nl.utwente.processing.pmd.utils.matches
import nl.utwente.processing.pmd.utils.uniqueCallStack

/**
 * Class which implements the decentralized drawing smell as a PMD rule.
 */
class DecentralizedDrawingRule : AbstractJavaRule() {

    private var drawStack : Set<ASTMethodDeclaration> = emptySet();

    override fun visit(node: ASTCompilationUnit?, data: Any?): Any? {
        this.drawStack = emptySet()
        return super.visit(node, data)
    }

    override fun visit(node: ASTClassOrInterfaceDeclaration, data: Any): Any? {
        //Check if this is a top node, not a inner class.
        if (!node.isNested) {
            val scope = node.scope as? ClassScope
            val methodDecl = scope?.findMethod(ProcessingApplet.DRAW_METHOD_SIGNATURE)
            methodDecl?.let {
                this.drawStack = scope.uniqueCallStack(methodDecl)
            }
        }
        return super.visit(node, data)
    }

    override fun visit(node: ASTPrimaryExpression, data: Any): Any? {
        val method = node.getFirstParentOfType(ASTMethodDeclaration::class.java);
        if (node.isMethodCall && method != null && method !in this.drawStack) {
            val match = node.matches(*ProcessingApplet.DRAW_METHODS.toTypedArray())
            match?.let {
                this.addViolationWithMessage(data, node, message, kotlin.arrayOf(match, method.name))
            }
        }
        return super.visit(node, data)
    }
}