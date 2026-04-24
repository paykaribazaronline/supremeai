package com.supremeai.ide.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.supremeai.ide.analysis.K2CompatibleCodeAnalyzer
import com.supremeai.ide.metrics.SupremeAIMetricsService
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class SupremeAIInspection : LocalInspectionTool() {
    
    private val analyzer = K2CompatibleCodeAnalyzer()
    
    override fun getDisplayName(): String = "SupremeAI Patterns"
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val metricsService = SupremeAIMetricsService.getInstance(holder.project)
        
        return object : KtVisitorVoid() {
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                val info = analyzer.analyzeClass(klass)
                if (info != null) {
                    metricsService.sendMetrics(mapOf("type" to "class_analysis", "name" to info.name))
                    if (info.name.contains("Agent")) {
                        holder.registerProblem(
                            klass.nameIdentifier ?: klass,
                            "Class '${info.name}' matches Agent pattern. Consider adding @AIAgent. (Qualified: ${info.qualifiedName})",
                            com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
                        )
                    }
                    
                                        // Specific K2-based check: check for abstract agents without supertypes
                    if (info.isAbstract && info.superTypes.isEmpty() && info.name.endsWith("Agent")) {
                        holder.registerProblem(
                            klass.nameIdentifier ?: klass,
                            "Abstract agent should typically extend a BaseAgent.",
                            com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        )
                    }
                    
                    // Context-aware optimization for RecyclerView adapters
                    if (info.superTypes.any { it.contains("RecyclerView.Adapter") } || info.name.endsWith("Adapter")) {
                        holder.registerProblem(
                            klass.nameIdentifier ?: klass,
                            "Context-Aware AI Suggestion: Consider using DiffUtil instead of notifyDataSetChanged() for optimal RecyclerView performance.",
                            com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
                        )
                    }
                }
            }
            
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                val info = analyzer.analyzeFunction(function)
                if (info != null) {
                    if (info.name.startsWith("ai")) {
                        holder.registerProblem(
                            function.nameIdentifier ?: function,
                            "AI function '${info.name}' returns ${info.returnType}. Ensure it follows SupremeAI patterns.",
                            com.intellij.codeInspection.ProblemHighlightType.INFORMATION
                        )
                    }
                }
            }
        }
    }
}
