package com.supremeai.ide.analysis

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction

/**
 * A code analyzer that is compatible with the K2 Kotlin compiler plugin.
 * This class provides methods for analyzing Kotlin classes and functions.
 */
class K2CompatibleCodeAnalyzer {
    private val LOG = Logger.getInstance(K2CompatibleCodeAnalyzer::class.java)

    /**
     * Analyzes the given [ktClass] and returns a [ClassAnalysisInfo] containing information
     * about the class, such as its name, qualified name, super types, properties, and functions.
     *
     * Returns `null` if the analysis fails.
     */
    fun analyzeClass(ktClass: KtClass): ClassAnalysisInfo? {
        return try {
            analyze(ktClass) {
                val symbol = ktClass.symbol as? KaClassSymbol ?: return@analyze null

                val properties = symbol.declaredMemberScope.callables
                    .filterIsInstance<KaPropertySymbol>()
                    .map { prop ->
                        PropertyInfo(
                            name = prop.name?.asString() ?: "",
                            type = prop.returnType.toString()
                        )
                    }
                    .toList()

                val functions = symbol.declaredMemberScope.callables
                    .filterIsInstance<KaFunctionSymbol>()
                    .map { func ->
                        FunctionInfo(
                            name = func.name?.asString() ?: "",
                            returnType = func.returnType.toString()
                        )
                    }
                    .toList()

                val superTypes = symbol.superTypes.map { it.type.toString() }

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

    /**
     * Analyzes the given [ktFunction] and returns a [FunctionAnalysisInfo] containing information
     * about the function, such as its name, return type, and parameters.
     *
     * Returns `null` if the analysis fails.
     */
    fun analyzeFunction(ktFunction: KtFunction): FunctionAnalysisInfo? {
        return try {
            analyze(ktFunction) {
                val symbol = ktFunction.symbol as? KaFunctionSymbol ?: return@analyze null

                val parameters = symbol.valueParameters.map { param ->
                    ParameterInfo(
                        name = param.name?.asString() ?: "",
                        type = param.returnType.toString()
                    )
                }

                FunctionAnalysisInfo(
                    name = symbol.name?.asString() ?: "anonymous",
                    returnType = symbol.returnType.toString(),
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

/**
 * Holds analysis information for a class.
 *
 * @property name The simple name of the class.
 * @property qualifiedName The fully qualified name of the class.
 * @property isAbstract Whether the class is abstract.
 * @property superTypes The list of super types for the class.
 * @property properties The list of properties in the class.
 * @property functions The list of functions in the class.
 */
data class ClassAnalysisInfo(
    val name: String,
    val qualifiedName: String,
    val isAbstract: Boolean,
    val superTypes: List<String>,
    val properties: List<PropertyInfo>,
    val functions: List<FunctionInfo>
)

/**
 * Holds analysis information for a function.
 *
 * @property name The name of the function.
 * @property returnType The return type of the function.
 * @property parameters The list of parameters for the function.
 * @property isSuspend Whether the function is a suspend function.
 */
data class FunctionAnalysisInfo(
    val name: String,
    val returnType: String,
    val parameters: List<ParameterInfo>,
    val isSuspend: Boolean
)

/**
 * Holds information about a property.
 *
 * @property name The name of the property.
 * @property type The type of the property.
 */
data class PropertyInfo(val name: String, val type: String)

/**
 * Holds information about a parameter.
 *
 * @property name The name of the parameter.
 * @property type The type of the parameter.
 */
data class ParameterInfo(val name: String, val type: String)

/**
 * Holds information about a function inside a class.
 *
 * @property name The name of the function.
 * @property returnType The return type of the function.
 */
data class FunctionInfo(val name: String, val returnType: String)
