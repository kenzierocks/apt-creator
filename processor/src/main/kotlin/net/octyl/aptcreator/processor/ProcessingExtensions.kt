package net.octyl.aptcreator.processor

import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element

inline fun <reified A : Annotation> Element.isAnnotationPresent(): Boolean {
    return MoreElements.isAnnotationPresent(this, A::class.java)
}

fun Element.isNullable(): Boolean {
    return nullableAnnotation() != null
}

fun Element.nullableAnnotation(): AnnotationMirror? {
    return annotationMirrors.firstOrNull {
        it.annotationType.asElement().simpleName.contentEquals("Nullable")
    }
}