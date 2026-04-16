package com.supremeai.ide.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class SupremeAIInspection : LocalInspectionTool() {
    
    override fun getDisplayName(): String = "SupremeAI Patterns"
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                if (klass.name?.contains("Agent") == true) {
                    holder.registerProblem(
                        klass.nameIdentifier ?: klass,
                        "Consider adding @AIAgent annotation",
                        com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
            
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (function.name?.startsWith("ai") == true) {
                    holder.registerProblem(
                        function.nameIdentifier ?: function,
                        "AI function should follow SupremeAI patterns",
                        com.intellij.codeInspection.ProblemHighlightType.INFORMATION
                    )
                }
            }
        }
    }
}
