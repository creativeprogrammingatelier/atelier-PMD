package nl.utwente.processing.pmd.rules

import net.sourceforge.pmd.lang.java.ast.*
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule
import nl.utwente.processing.pmd.symbols.ProcessingApplet
import nl.utwente.processing.pmd.symbols.ProcessingAppletMethodCategory

/**
 * Class which implements the decentralized drawing smell as a PMD rule.
 */
class DecentralizedDrawingRule : AbstractJavaRule() {
    private val targetMethods = listOf<ProcessingAppletMethodCategory>(
            ProcessingAppletMethodCategory.SHAPE,
            ProcessingAppletMethodCategory.SHAPE_2D,
            ProcessingAppletMethodCategory.SHAPE_CURVES,
            ProcessingAppletMethodCategory.SHAPE_3D,
            ProcessingAppletMethodCategory.SHAPE_ATTRIBUTES,
            ProcessingAppletMethodCategory.SHAPE_VERTEX,
            ProcessingAppletMethodCategory.SHAPE_LD
    )

    private val globalVariables = HashMap<String, ASTClassOrInterfaceDeclaration?>()
    private val classDeclarations = HashMap<ASTClassOrInterfaceDeclaration, ArrayList<ASTMethodDeclaration>>()
    private val stringToMethodDeclarations = HashMap<String, ASTMethodDeclaration>()
    private val violatingMethods = ArrayList<ASTMethodDeclaration>()


    override fun visit(node: ASTCompilationUnit, data: Any): Any? {
        for (classDec in node.findDescendantsOfType(ASTClassOrInterfaceDeclaration::class.java)) {
            val methodList = ArrayList<ASTMethodDeclaration>()
            for (method in classDec.findDescendantsOfType(ASTMethodDeclaration::class.java)) {
                methodList.add(method)
                if (!classDec.isNested) {
                    stringToMethodDeclarations[method.name] = method
                }
            }
            classDeclarations[classDec] = methodList
        }
        return super.visit(node, data)
    }

    override fun visit(node: ASTClassOrInterfaceDeclaration, data: Any): Any? {
        if (!node.isNested) {
            for (field in node.findDescendantsOfType(ASTFieldDeclaration::class.java)) {
                val classDec= getClassDeclaration(field.getFirstDescendantOfType(ASTClassOrInterfaceType::class.java).image
                        , node)
                if (classDec != null) {
                    globalVariables[field.getFirstDescendantOfType(ASTVariableDeclaratorId::class.java).name] = classDec
                }
            }
            for (method in node.findDescendantsOfType(ASTMethodDeclaration::class.java)) {
                if (isEventHandler(method.name)) {
                    checkStatementExpressions(method.findDescendantsOfType(ASTStatementExpression::class.java), node, data)
                }
            }
        }
        return super.visit(node, data)
    }

    override fun visit(node: ASTMethodDeclaration, data: Any): Any? {
        if (violatingMethods.contains(node)) {
            this.addViolationWithMessage(data, node.getFirstDescendantOfType(ASTResultType::class.java), message, kotlin.arrayOf(node.getFirstDescendantOfType(ASTName::class.java).image
                    , node.name))
        }
        return super.visit(node, data)
    }

    private fun checkStatementExpressions(statements: List<ASTStatementExpression>, node: ASTClassOrInterfaceDeclaration, data: Any) {
        val localVariables = extractLocalVariables(statements[0].getFirstParentOfType(ASTMethodDeclaration::class.java))
        for (statement in statements) {
            if (isTargetShapeMethod(statement.getFirstDescendantOfType(ASTName::class.java).image)) {
                violatingMethods.add(statement.getFirstParentOfType(ASTMethodDeclaration::class.java)) // Check if method is target method
            }
            if (statement.getFirstDescendantOfType(ASTName::class.java).image.contains('.')) { // Check if method is called from an instantiated class.
                val expression = statement.getFirstDescendantOfType(ASTName::class.java).image.split('.')
                if (globalVariables.containsKey(expression[0])) {
                    checkClassMethod(expression[1], globalVariables[expression[0]], node, data)
                } else if (localVariables.containsKey(expression[0])) {
                    checkClassMethod(expression[1], localVariables[expression[0]], node, data)
                }
            }
            if (stringToMethodDeclarations.containsKey(statement.getFirstDescendantOfType(ASTName::class.java).image)) { // Check if method is from the Main Tab.
                checkClassMethod(statement.getFirstDescendantOfType(ASTName::class.java).image, statement.getFirstParentOfType(ASTClassOrInterfaceDeclaration::class.java)
                        , node, data)
            }
        }
    }

    private fun checkClassMethod(targetMethod: String, targetClass: ASTClassOrInterfaceDeclaration?, node: ASTClassOrInterfaceDeclaration, data: Any) {
        for (method in targetClass?.findDescendantsOfType(ASTMethodDeclaration::class.java)!!) {
            if (method.name == targetMethod) {
                checkStatementExpressions(method.findDescendantsOfType(ASTStatementExpression::class.java), node, data)
            }
        }
    }

    private fun extractLocalVariables(method: ASTMethodDeclaration): Map<String, ASTClassOrInterfaceDeclaration> {
        val res = HashMap<String, ASTClassOrInterfaceDeclaration>()
        for (localVariable in method.findDescendantsOfType(ASTLocalVariableDeclaration::class.java)) {
            val varName = localVariable.getFirstDescendantOfType(ASTVariableDeclaratorId::class.java).name
            val varClass = getClassDeclaration(localVariable.getFirstDescendantOfType(ASTClassOrInterfaceType::class.java).image
                    , method.getFirstParentOfType(ASTClassOrInterfaceDeclaration::class.java))
            if (varName != "kotlin.Unit" && varClass != null) {
                res[varName] = varClass
            }
        }
        return res
    }


    private fun getClassDeclaration(target: String, node: ASTClassOrInterfaceDeclaration): ASTClassOrInterfaceDeclaration? {
        for (classDec in node.findDescendantsOfType(ASTClassOrInterfaceDeclaration::class.java)) {
            if (classDec.simpleName == target) {
                return classDec
            }
        }
        return null
    }

    private fun isEventHandler(methodName: String): Boolean {
        ProcessingApplet.EVENT_METHOD_SIGNATURES.forEach {
            if (it.startsWith(methodName)) {
                return true
            }
        }
        return false;
    }

    private fun isTargetShapeMethod(methodName: String): Boolean {
        ProcessingApplet.DRAW_METHODS.forEach {
            if (it.name == methodName && targetMethods.contains(it.category)) {
                return true
            }
        }
        return false
    }
}