package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import net.sourceforge.pmd.lang.java.symboltable.ClassScope
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration
import net.sourceforge.pmd.lang.symboltable.NameOccurrence

/**
 * Class that implements the OutOfScopeStateChange rule.
 * <p>
 *     This is rule defines that global variables, variables
 *     that are defined within the main class of the Processing
 *     application, and variables that are out of the scope
 *     of the current class, should not have their state changed
 *     from anywhere except the class that defined them. Boolean
 *     operations are allowed however.
 * </p>
 */
class OutOfScopeStateChangeRule: AbstractJavaRule() {

    /**
     * Entry point for visitor. The visitor takes a class declaration and checks, for each class, if the
     * ASTStatementExpression within the methods, it then checks if the expression changes the state of a
     * variable that is outside the scope of the class declaring the method.
     */
    override fun visit(node: ASTClassOrInterfaceDeclaration, data: Any): Any? {
        val scope = node.scope as? ClassScope // Extract the scope of the class.
        val nodeVariableDeclarations = extractImages(scope?.variableDeclarations!!) // Extract names of the variables declared by the class.
        for (declaration in node.declarations) { // Iterate through ASTMethodOrConstructor declarations within the scope of the class.
            if (declaration.kind == ASTAnyTypeBodyDeclaration.DeclarationKind.METHOD) { // Check whether declarations is method.
                val method = declaration.declarationNode as? ASTMethodDeclaration // Extracts the ASTMethodDeclaration.
                for (expression in method?.body?.findDescendantsOfType(ASTStatementExpression::class.java)!!) { // Iterate through all ASTStatementExpressions within the method body.
                    if (expression.hasDescendantOfType(ASTAssignmentOperator::class.java) &&
                            !nodeVariableDeclarations.contains(expression.getFirstDescendantOfType(ASTName::class.java).image)) { // If the statement expressions changes the state of a variable
                                                                                                                                  // and if that variable is outside the scope of the class.
                        this.addViolationWithMessage(data, node, message,
                                arrayOf(expression.getFirstDescendantOfType(ASTName::class.java).image, "method " + method.name))  // If yes, add violation with method format.
                    }
                }
            } else if (declaration.kind == ASTAnyTypeBodyDeclaration.DeclarationKind.CONSTRUCTOR) { // Checks if the declaration is a constructor.
                val constructor = declaration.declarationNode as? ASTConstructorDeclaration // Extracts the ASTConstructorDeclaration, from here the logic is the same as method check.
                for (expression in constructor?.findDescendantsOfType(ASTStatementExpression::class.java)!!) {
                    if (expression.hasDescendantOfType(ASTAssignmentOperator::class.java) &&
                            !nodeVariableDeclarations.contains(expression.getFirstDescendantOfType(ASTName::class.java).image)) {
                        this.addViolationWithMessage(data, node, message,
                                arrayOf(expression.getFirstDescendantOfType(ASTName::class.java).image, "Constructor"))
                    }
                }
            }
        }
        return super.visit(node, data)
    }

    /**
     * Function for extracting the variable names for all variables withing a ClassScope's scope.
     *
     * @param variableDeclarations Map of variables name declarations and list of occurrences
     * @return A list of all variable names withing the ClassScope
     */
    private fun extractImages(variableDeclarations: Map<VariableNameDeclaration, List<NameOccurrence>>): ArrayList<String> {
        val res = ArrayList<String>()
        for (variableName in variableDeclarations.keys) {
            res.add(variableName.image)
        }
        return res
    }
}