package com.supremeai.ide.analysis

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.types.Variance

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

                ClassAnalysisInfo(
                    name = ktClass.name ?: "anonymous",
                    qualifiedName = symbol.classId?.asFqNameString() ?: "",
                    isAbstract = symbol.modality == KaSymbolModality.ABSTRACT,
                    superTypes = symbol.superTypes.map { it.render(position = Variance.INVARIANT) },
                    properties = symbol.combinedDeclaredMemberScope.callables
                        .filterIsInstance<KaPropertySymbol>()
                        .map { 
                            PropertyInfo(
                                name = it.name.asString(), 
                                type = it.returnType.render(position = Variance.INVARIANT),
                                visibility = it.visibility.name,
                                modality = it.modality.name
                            ) 
                        }
                        .toList(),
                    functions = symbol.combinedDeclaredMemberScope.callables
                        .mapNotNull { it as? KaFunctionSymbol }
                        .map { 
                            FunctionInfo(
                                name = (it as? org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol)?.name?.asString() ?: "<anonymous>", 
                                returnType = it.returnType.render(position = Variance.INVARIANT),
                                visibility = it.visibility.name,
                                modality = it.modality.name,
                                isExternal = false // Removed it.isExternal as it might not be available in this K2 version
                            ) 
                        }
                        .toList()
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

                FunctionAnalysisInfo(
                    name = ktFunction.name ?: "anonymous",
                    returnType = symbol.returnType.render(position = Variance.INVARIANT),
                    parameters = symbol.valueParameters.map { 
                        ParameterInfo(it.name.asString(), it.returnType.render(position = Variance.INVARIANT))
                    },
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
 * @property visibility The visibility of the property.
 * @property modality The modality of the property.
 */
data class PropertyInfo(
    val name: String, 
    val type: String,
    val visibility: String,
    val modality: String
)

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
 * @property visibility The visibility of the function.
 * @property modality The modality of the function.
 * @property isExternal Whether the function is external.
 */
data class FunctionInfo(
    val name: String, 
    val returnType: String,
    val visibility: String,
    val modality: String,
    val isExternal: Boolean
)
