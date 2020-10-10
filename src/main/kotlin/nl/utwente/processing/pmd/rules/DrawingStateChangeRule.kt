package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.java.rule.codestyle.AtLeastOneConstructorRule
import net.sourceforge.pmd.lang.java.symboltable.ClassScope
import net.sourceforge.pmd.lang.java.symboltable.JavaNameOccurrence
import net.sourceforge.pmd.lang.java.symboltable.MethodScope
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration
import net.sourceforge.pmd.lang.symboltable.NameOccurrence
import nl.utwente.processing.pmd.symbols.ProcessingApplet
import nl.utwente.processing.pmd.utils.callStack
import nl.utwente.processing.pmd.utils.findMethod
import nl.utwente.processing.pmd.utils.getContainingClass
import java.beans.MethodDescriptor

/**
 * Class which refines the state change smell.
 */
class DrawingStateChangeRule: AbstractJavaRule() {

    override fun visit(node: ASTClassOrInterfaceDeclaration, data: Any): Any? {
        val scope = node.scope as? ClassScope
        val nodeVariableDeclarations = extractImages(scope?.variableDeclarations!!)
        for (declaration in node.declarations) {
            if (declaration.kind == ASTAnyTypeBodyDeclaration.DeclarationKind.METHOD) {
                val method = declaration.declarationNode as? ASTMethodDeclaration
                for (expression in method?.body?.findDescendantsOfType(ASTStatementExpression::class.java)!!) {
                    if (expression.hasDescendantOfType(ASTAssignmentOperator::class.java)) {
                        if (!nodeVariableDeclarations.contains(expression.getFirstDescendantOfType(ASTName::class.java).image)) {
                            this.addViolationWithMessage(data, node, message,
                                    arrayOf(expression.getFirstDescendantOfType(ASTName::class.java).image, method.name))
                        }
                    }
                }
            } else if (declaration.kind == ASTAnyTypeBodyDeclaration.DeclarationKind.CONSTRUCTOR) {
                val constructor = declaration.declarationNode as? ASTConstructorDeclaration
                for (expression in constructor?.findDescendantsOfType(ASTStatementExpression::class.java)!!) {
                    if (expression.hasDescendantOfType(ASTAssignmentOperator::class.java)) {
                        if (!nodeVariableDeclarations.contains(expression.getFirstDescendantOfType(ASTName::class.java).image)) {
                            this.addViolationWithMessage(data, node, message,
                                    arrayOf(expression.getFirstDescendantOfType(ASTName::class.java).image, "Constructor"))
                        }
                    }
                }
            }
        }
        return super.visit(node, data)
    }

    private fun extractImages(variableDeclarations: Map<VariableNameDeclaration, List<NameOccurrence>>): ArrayList<String> {
        val res = ArrayList<String>()
        for (variableName in variableDeclarations.keys) {
            res.add(variableName.image)
        }
        println(res)
        return res
    }
}