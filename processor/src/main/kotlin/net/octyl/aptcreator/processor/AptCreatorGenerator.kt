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
    private val providedParams = creatorParameters.constructorParameters.filter { it.isProvided }
    private val requiredParams = creatorParameters.constructorParameters.filter { !it.isProvided }

    companion object {
        private val TYPE_NAME_PROVIDER = ClassName.get(Provider::class.java)

        private fun providerWrapping(typeName: TypeName) =
                ParameterizedTypeName.get(TYPE_NAME_PROVIDER, typeName)

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
                    addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    addFields(getProviderFields())
                    addMethod(constructorSpec())
                    addMethod(createSpec())
                    addMethod(CHECK_NOT_NULL_HELPER)
                }
                .build()
    }

    private fun getProviderFields(): List<FieldSpec> {
        return providedParams.map {
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
                    providedParams.mapIndexed { index, param ->
                        val pName = param.providerName
                        CodeBlock.of("$[\$L;\n$]", "this.$pName = checkNotNull($pName, $index)")
                    }, ""))
            .build()

    private fun getProviderParameters(): List<ParameterSpec> {
        return providedParams.map {
            val providerType = providerWrapping(it.type)
            ParameterSpec.builder(providerType, it.providerName)
                    .apply {
                        it.qualifierAnnotation?.let(this::addAnnotation)
                    }
                    .build()
        }
    }

    private fun createSpec() = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC)
            .returns(creatorParameters.targetClassName)
            .addParameters(getRequiredParameters())
            .addCode(CodeBlock.builder()
                    .add("$[return new \$T(", creatorParameters.targetClassName)
                    .add(CodeBlock.join(creatorParameters.constructorParameters
                            .mapIndexed(this@AptCreatorGenerator::loadAndCheckParameterCode),
                            ",\n"))
                    .add(");\n$]")
                    .build())
            .build()

    private fun getRequiredParameters(): List<ParameterSpec> {
        return requiredParams.map {
            ParameterSpec.builder(it.type, it.name)
                    .apply {
                        it.nullableAnnotation?.let(this::addAnnotation)
                    }
                    .build()
        }
    }

    private fun loadAndCheckParameterCode(index: Int, param: ConstructorParameter): CodeBlock {
        val value = when {
            param.isProvided -> "${param.providerName}.get()"
            else -> param.name
        }
        return when {
            param.nullableAnnotation != null -> CodeBlock.of("\$L", value)
            else -> CodeBlock.of("\$L", "checkNotNull($value, $index)")
        }
    }

    private fun generatedAnnotationSpec() = AnnotationSpec.builder(ClassName.get(generatedAnnotationElement!!))
            .addMember("value", "\$S", javaClass.name)
            .addMember("comments", "\$S", "https://github.com/kenzierocks/apt-creator")
            .build()
}
