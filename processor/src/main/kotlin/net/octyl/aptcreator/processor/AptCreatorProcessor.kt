/*
 * This file is part of apt-creator, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.octyl.aptcreator.processor

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import net.octyl.aptcreator.GenerateCreator
import java.util.Collections
import java.util.HashSet
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypesException
import javax.tools.Diagnostic

class AptCreatorProcessor : AbstractProcessor() {

    private val processedClasses = HashSet<String>()
    private val messager get() = processingEnv.messager

    override fun getSupportedAnnotationTypes(): Set<String> {
        return Collections.singleton(GenerateCreator::class.java.name)
    }

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(GenerateCreator::class.java)
                .asSequence()
                .mapNotNull(this::getClassForElement)
                .filter(this::needsProcessing)
                .forEach(this::processClass)

        return true
    }

    private fun needsProcessing(element: TypeElement): Boolean {
        if (element.kind != ElementKind.CLASS) {
            throw IllegalArgumentException("Can only check CLASS for processing.")
        }

        return processedClasses.add(element.qualifiedName.toString())
    }

    private fun getClassForElement(element: Element): TypeElement? {
        return when (element.kind) {
            ElementKind.CLASS -> {
                val te = element as TypeElement
                when {
                    Modifier.ABSTRACT in te.modifiers -> {
                        reportUnsupportedElement(element)
                        null
                    }
                    else -> te
                }
            }
            else -> {
                reportUnsupportedElement(element)
                null
            }
        }
    }

    private fun reportUnsupportedElement(element: Element) {
        messager.printMessage(Diagnostic.Kind.ERROR,
                "Unsupported element for @GenerateCreator.", element)
    }

    private val generatedTypeElement: TypeElement? by lazy {
        processingEnv.elementUtils.getTypeElement(when {
            processingEnv.sourceVersion > SourceVersion.RELEASE_8 ->
                "javax.annotation.processing.Generated"
            else ->
                "javax.annotation.Generated"
        })
    }

    private fun processClass(element: TypeElement) {
        val creatorParameters = interpretCreatorTarget(element) ?: return

        val typeSpec = AptCreatorGenerator(creatorParameters, generatedTypeElement).generateClass()
        val pkgName = creatorParameters.targetClassName.packageName()

        JavaFile.builder(pkgName, typeSpec).build().writeTo(processingEnv.filer)
    }

    private val baseInvalidNames: List<CharSequence?> by lazy {
        listOf(
                // Don't cause compile errors.
                generatedTypeElement?.qualifiedName,
                // Don't repeat ourselves.
                GenerateCreator::class.java.canonicalName,
                GenerateCreator.CopyAnnotations::class.java.canonicalName,
                // Kotlin's metadata shouldn't be copied.
                Metadata::class.java.canonicalName
        )
    }

    private fun isBadAnnotationName(testName: Name, excluded: List<String>): Boolean {
        val invalidNames: List<CharSequence?> = baseInvalidNames + excluded
        return invalidNames.any {
            System.err.println("$testName vs $it")
            it != null && testName.contentEquals(it)
        }
    }

    private fun interpretCreatorTarget(element: TypeElement): CreatorParameters? {
        val constructorTargets = element.getConstructors()
        val constructorParameters = constructorTargets.map {
            it.parameters.map(VariableElement::toConstructorParameter)
        }

        val annotation = element.getAnnotation(GenerateCreator::class.java)
                ?: throw IllegalStateException("Annotation cannot be null.")
        val copyAnnotations = element.getAnnotation(GenerateCreator.CopyAnnotations::class.java)

        val creatorClassName = when {
            annotation.className.isBlank() -> createCreatorClassName(element)
            else -> annotation.className
        }

        val annotations = when {
            copyAnnotations != null -> {
                element.annotationMirrors.filterNot { annot ->
                    val type = MoreElements.asType(annot.annotationType.asElement()).qualifiedName
                    val excludedClasses = mirroredExtract { copyAnnotations.exclude }
                    isBadAnnotationName(type, excludedClasses)
                }
            }
            else -> listOf()
        }

        return CreatorParameters(originatingElement = element,
                targetClassName = ClassName.get(element),
                constructorParameters = constructorParameters,
                creatorClassName = creatorClassName,
                classAnnotations = annotations)
    }

    private inline fun mirroredExtract(mirroredAccess: () -> Unit): List<String> {
        return try {
            mirroredAccess()
            throw IllegalStateException("Should be using mirrored types!")
        } catch (e: MirroredTypesException) {
            e.typeMirrors.map { MoreTypes.asTypeElement(it).qualifiedName.toString() }
        }
    }

    private fun createCreatorClassName(element: TypeElement): String {
        val wrappers = walkUpToPackage(element)
        return wrappers.joinToString("_") + "Creator"
    }

    private fun walkUpToPackage(element: TypeElement): List<String> {
        var current: Element = element
        val backwardsList = mutableListOf<String>()
        while (current.enclosingElement != null && current.kind != ElementKind.PACKAGE) {
            backwardsList.add(current.simpleName.toString())
            current = current.enclosingElement!!
        }
        return backwardsList.reversed()
    }

    private fun TypeElement.getConstructors(): List<ExecutableElement> {
        return enclosedElements
                .filter { e -> e.kind == ElementKind.CONSTRUCTOR }
                .map(MoreElements::asExecutable)
    }
}
