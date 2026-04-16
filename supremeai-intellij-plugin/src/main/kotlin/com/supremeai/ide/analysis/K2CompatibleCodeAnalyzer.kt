package com.supremeai.ide.analysis

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction

class K2CompatibleCodeAnalyzer {
    private val LOG = Logger.getInstance(K2CompatibleCodeAnalyzer::class.java)

    fun analyzeClass(ktClass: KtClass): ClassAnalysisInfo? {
        return try {
            analyze(ktClass) {
                val symbol = ktClass.symbol as? KaClassSymbol ?: return@analyze null

                val properties = symbol.declaredMemberScope.callables
                    .filterIsInstance<KaPropertySymbol>()
                    .map { prop ->
                        PropertyInfo(
                            name = prop.name?.asString() ?: "",
                            type = "Property"
                        )
                    }
                    .toList()

                val functions = symbol.declaredMemberScope.callables
                    .filterIsInstance<KaFunctionSymbol>()
                    .map { func ->
                        FunctionInfo(
                            name = func.name?.asString() ?: "",
                            returnType = "Function"
                        )
                    }
                    .toList()

                val superTypes = symbol.superTypes.map { "SuperType" }

                ClassAnalysisInfo(
                    name = symbol.name?.asString() ?: "anonymous",
                    qualifiedName = symbol.classId?.asFqNameString() ?: "",
                    isAbstract = symbol.modality == KaSymbolModality.ABSTRACT,
                    superTypes = superTypes,
                    properties = properties,
                    functions = functions
                )
            }
        } catch (e: Exception) {
            LOG.warn("Failed to analyze class ${ktClass.name}", e)
            null
        }
    }

    fun analyzeFunction(ktFunction: KtFunction): FunctionAnalysisInfo? {
        return try {
            analyze(ktFunction) {
                val symbol = ktFunction.symbol as? KaFunctionSymbol ?: return@analyze null

                val parameters = symbol.valueParameters.map { param ->
                    ParameterInfo(
                        name = param.name?.asString() ?: "",
                        type = "Parameter"
                    )
                }

                FunctionAnalysisInfo(
                    name = symbol.name?.asString() ?: "anonymous",
                    returnType = "ReturnType",
                    parameters = parameters,
                    isSuspend = ktFunction.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.SUSPEND_KEYWORD)
                )
            }
        } catch (e: Exception) {
            LOG.warn("Failed to analyze function ${ktFunction.name}", e)
            null
        }
    }
}

data class ClassAnalysisInfo(
    val name: String,
    val qualifiedName: String,
    val isAbstract: Boolean,
    val superTypes: List<String>,
    val properties: List<PropertyInfo>,
    val functions: List<FunctionInfo>
)

data class FunctionAnalysisInfo(
    val name: String,
    val returnType: String,
    val parameters: List<ParameterInfo>,
    val isSuspend: Boolean
)

data class PropertyInfo(val name: String, val type: String)
data class ParameterInfo(val name: String, val type: String)
data class FunctionInfo(val name: String, val returnType: String)
