package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import kotlinx.metadata.*
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

import com.squareup.kotlinpoet.ClassName as KotlinpoetClassName

fun ProcessingEnvironment.getTypeElement(t: TypeMirror): TypeElement {
    return this.typeUtils.asElement(t) as TypeElement
}

fun ProcessingEnvironment.getTypeElement(type: String): TypeElement {
    try {
        return this.elementUtils.getTypeElement(type)
    } catch (e: Exception) {
        this.printMessage("failed to recognize type:$type\n")
        throw e
    }
}

fun getClassMetadata(element: Element): KmClass? {
    if (element.kind != ElementKind.CLASS) return null
    val metadataAnnotation = element.getAnnotation(Metadata::class.java) ?: return null
    val header = KotlinClassHeader(
            kind = metadataAnnotation.kind,
            metadataVersion = metadataAnnotation.metadataVersion,
            bytecodeVersion = metadataAnnotation.bytecodeVersion,
            data1 = metadataAnnotation.data1,
            data2 = metadataAnnotation.data2,
            extraString = metadataAnnotation.extraString,
            packageName = metadataAnnotation.packageName,
            extraInt = metadataAnnotation.extraInt
    )

    val metadata = KotlinClassMetadata.read(header) as KotlinClassMetadata.Class
    return metadata.toKmClass()
}

fun KmFunction.isSuspendable() = Flag.Function.IS_SUSPEND(this.flags)

fun KmFunction.isOpen() = Flag.IS_OPEN(this.flags)

fun KmFunction.isPublic() = Flag.IS_PUBLIC(this.flags)

fun KmConstructor.isPrimary() = Flag.Constructor.IS_PRIMARY(this.flags)

fun ProcessingEnvironment.printMessage(
        message: String,
        e: Exception? = null,
        kind:Diagnostic.Kind = Diagnostic.Kind.ERROR
) {
    val exceptionAsString = if (e !== null) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        sw.toString()
    } else ""
    this.messager.printMessage(kind, "$message $exceptionAsString")
}

fun ProcessingEnvironment.getClassName(type: KmType?): KotlinpoetClassName {
    if (type === null) {
        this.printMessage("classifier is null")
        throw NullPointerException("classifier is null")
    }
    val typeName = extractName(type)
    return this.classNameFromClassifier(typeName)
}

fun extractName(type: KmType): ClassName = when (val classifier = type.classifier) {
    is KmClassifier.Class -> classifier.name
    is KmClassifier.TypeParameter -> TODO()
    is KmClassifier.TypeAlias -> TODO()
}


fun ProcessingEnvironment.classNameFromClassifier(classifier: String): KotlinpoetClassName {
    return when (classifier) {
        "kotlin/Any" -> ANY
        "kotlin/Array" -> ARRAY
        "kotlin/Unit" -> UNIT
        "kotlin/Boolean" -> BOOLEAN
        "kotlin/Byte" -> BYTE
        "kotlin/Short" -> SHORT
        "kotlin/Int" -> INT
        "kotlin/Long" -> LONG
        "kotlin/Char" -> CHAR
        "kotlin/Float" -> FLOAT
        "kotlin/Double" -> DOUBLE
        "kotlin/String" -> STRING
        "kotlin/CharSequence" -> CHAR_SEQUENCE
        "kotlin/Comparable" -> COMPARABLE
        "kotlin/Throwable" -> THROWABLE
        "kotlin/Annotation" -> ANNOTATION
        "kotlin/Nothing" -> NOTHING
        "kotlin/Number" -> NUMBER
        "kotlin/collections/Iterable" -> ITERABLE
        "kotlin/collections/Collection" -> COLLECTION
        "kotlin/collections/List" -> LIST
        "kotlin/collections/Set" -> SET
        "kotlin/collections/Map" -> MAP
        //"kotlin/" -> MAP_ENTRY = MAP.nestedClass("Entry")
        "kotlin/collections/MutableIterable" -> MUTABLE_ITERABLE
        "kotlin/collections/MutableCollection" -> MUTABLE_COLLECTION
        "kotlin/collections/MutableList" -> MUTABLE_LIST
        "kotlin/collections/MutableSet" -> MUTABLE_SET
        "kotlin/collections/MutableMap" -> MUTABLE_MAP
        //"kotlin/" -> MUTABLE_MAP_ENTRY = MUTABLE_MAP.nestedClass("Entry")
        "kotlin/BooleanArray" -> BOOLEAN_ARRAY
        "kotlin/ByteArray" -> BYTE_ARRAY
        "kotlin/CharArray" -> CHAR_ARRAY
        "kotlin/ShortArray" -> SHORT_ARRAY
        "kotlin/IntArray" -> INT_ARRAY
        "kotlin/LongArray" -> LONG_ARRAY
        "kotlin/FloatArray" -> FLOAT_ARRAY
        "kotlin/DoubleArray" -> DOUBLE_ARRAY
        "kotlin/Enum" -> ENUM
        "kotlin/UByte" -> U_BYTE
        "kotlin/UShort" -> U_SHORT
        "kotlin/UInt" -> U_INT
        "kotlin/ULong" -> U_LONG
        "kotlin/UByteArray" -> U_BYTE_ARRAY
        "kotlin/UShortArray" -> U_SHORT_ARRAY
        "kotlin/UIntArray" -> U_INT_ARRAY
        "kotlin/ULongArray" -> U_LONG_ARRAY
        else -> this.getTypeElement(classifier.replace('/', '.')).asClassName()
    }
}