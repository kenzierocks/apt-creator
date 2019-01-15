package net.octyl.aptcreator.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import javax.inject.Provider
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class AptCreatorGenerator(private val creatorParameters: CreatorParameters,
                          private val generatedAnnotationElement: TypeElement?) {
    // pull all provided params from each constructor
    private val providedParams = creatorParameters.constructorParameters
            .flatten()
            .filter { it.isProvided }
            .distinctBy { it.key }
            .associate { param ->
                param.key to param
            }

    companion object {
        private val TYPE_NAME_PROVIDER = ClassName.get(Provider::class.java)

        private fun providerWrapping(typeName: TypeName) =
                ParameterizedTypeName.get(TYPE_NAME_PROVIDER, typeName.box())

        private val CHECK_NOT_NULL_HELPER: MethodSpec = MethodSpec.methodBuilder("checkNotNull")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(TypeVariableName.get("T"))
                .addParameter(TypeVariableName.get("T"), "arg")
                .addParameter(TypeName.INT, "argIndex")
                .addCode(CodeBlock.builder()
                        .beginControlFlow("if (arg == null)")
                        .addStatement("throw new \$T(\$S + argIndex)",
                                ClassName.get("java.lang", "NullPointerException"),
                                "@GenerateCreator class was passed null to a non-null argument. " +
                                        "Index: "
                        )
                        .endControlFlow()
                        .addStatement("return arg")
                        .build())
                .build()
    }

    fun generateClass(): TypeSpec {
        return TypeSpec.classBuilder(creatorParameters.creatorClassName)
                .apply {
                    if (generatedAnnotationElement != null) {
                        addAnnotation(generatedAnnotationSpec())
                    }
                    addAnnotations(creatorParameters.classAnnotations
                            .map(AnnotationSpec::get))
                    addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    addFields(getProviderFields())
                    addMethod(constructorSpec())
                    creatorParameters.constructorParameters.forEach { params ->
                        addMethod(createSpec(params))
                    }
                    addMethod(CHECK_NOT_NULL_HELPER)
                    addOriginatingElement(creatorParameters.originatingElement)
                }
                .build()
    }

    private fun getProviderFields(): List<FieldSpec> {
        return providedParams.values.map {
            val providerType = providerWrapping(it.type)
            FieldSpec.builder(providerType, it.providerName)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build()
        }
    }

    private fun constructorSpec() = MethodSpec.constructorBuilder()
            .addAnnotation(ClassName.get("javax.inject", "Inject"))
            .addModifiers(Modifier.PUBLIC)
            .addParameters(getProviderParameters())
            .addCode(CodeBlock.join(
                    providedParams.values.mapIndexed { index, param ->
                        val pName = param.providerName
                        CodeBlock.of("$[\$L;\n$]", "this.$pName = checkNotNull($pName, $index)")
                    }, ""))
            .build()

    private fun getProviderParameters(): List<ParameterSpec> {
        return providedParams.values.map {
            val providerType = providerWrapping(it.type)
            ParameterSpec.builder(providerType, it.providerName)
                    .apply {
                        it.qualifierAnnotation?.let(this::addAnnotation)
                    }
                    .build()
        }
    }

    private fun createSpec(params: List<ConstructorParameter>) = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC)
            .returns(creatorParameters.targetClassName)
            .addParameters(getRequiredParameters(params))
            .addCode(CodeBlock.builder()
                    .add("$[return new \$T(", creatorParameters.targetClassName)
                    .add(CodeBlock.join(params
                            .mapIndexed(this@AptCreatorGenerator::loadAndCheckParameterCode),
                            ",\n"))
                    .add(");\n$]")
                    .build())
            .build()

    private fun getRequiredParameters(params: List<ConstructorParameter>): List<ParameterSpec> {
        return params.filter { !it.isProvided }.map {
            ParameterSpec.builder(it.type, it.name)
                    .apply {
                        it.nullableAnnotation?.let(this::addAnnotation)
                    }
                    .build()
        }
    }

    private fun loadAndCheckParameterCode(index: Int, param: ConstructorParameter): CodeBlock {
        val value = when {
            param.isProvided -> {
                val backingParameter = providedParams[param.key]
                        ?: throw IllegalStateException("Missing entry for $param.")
                "${backingParameter.providerName}.get()"
            }
            else -> param.name
        }
        val noNullCheck = param.nullableAnnotation != null
                || (param.type.isPrimitive && !param.isProvided)
        return when {
            noNullCheck -> CodeBlock.of("\$L", value)
            else -> CodeBlock.of("\$L", "checkNotNull($value, $index)")
        }
    }

    private fun generatedAnnotationSpec() = AnnotationSpec.builder(ClassName.get(generatedAnnotationElement!!))
            .addMember("value", "\$S", javaClass.name)
            .addMember("comments", "\$S", "https://github.com/kenzierocks/apt-creator")
            .build()
}
