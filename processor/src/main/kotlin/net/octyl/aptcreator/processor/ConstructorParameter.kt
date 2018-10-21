package net.octyl.aptcreator.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.TypeName
import net.octyl.aptcreator.Provided
import javax.inject.Qualifier
import javax.lang.model.element.VariableElement

data class ConstructorParameter(val name: String,
                                val type: TypeName,
                                val nullableAnnotation: AnnotationSpec?,
                                val isProvided: Boolean,
                                val qualifierAnnotation: AnnotationSpec?) {

    // A parameter is only unique by this key -- name & nullability don't matter.
    data class UniqueKey(val type: TypeName, val qualifierAnnotation: AnnotationSpec?)
    val providerName = "${name}Provider"
    val key = UniqueKey(type, qualifierAnnotation)
}

fun VariableElement.toConstructorParameter(): ConstructorParameter {
    val name = simpleName.toString()
    val type = TypeName.get(asType())
    val nullable = findSingleNullable()
    val provided = getAnnotation(Provided::class.java) != null
    val qualifierAnnotation = findSingleQualifier()
    return ConstructorParameter(name, type, nullable, provided, qualifierAnnotation)
}

private fun VariableElement.findSingleQualifier(): AnnotationSpec? {
    val qualifiers = annotationMirrors
            .filter { it.annotationType.asElement().isAnnotationPresent<Qualifier>() }
    return when (qualifiers.size) {
        0 -> null
        1 -> {
            val qualifier = qualifiers.first()
            AnnotationSpec.get(qualifier)
        }
        else -> throw IllegalStateException("Multiple qualifying annotations present.")
    }
}

private fun VariableElement.findSingleNullable(): AnnotationSpec? {
    val na = nullableAnnotation() ?: return null
    return AnnotationSpec.get(na)
}
