package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.metadata.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import kotlin.coroutines.Continuation

internal class ActorBuilder(private val roundEnv: RoundEnvironment,
                            private val processingEnv: ProcessingEnvironment) {
    private val suspendableErasure = processingEnv.typeUtils.erasure(
            processingEnv.getTypeElement(Continuation::class.java.typeName).asType()
    )

    private val coroutineScope = processingEnv.getTypeElement(CoroutineScope::class.java.typeName).asType()

    internal fun generateActors(actors: MutableSet<out Element>) {
        if (actors.isEmpty())
            return

        actors.forEach { e ->
            val className = "${e.simpleName}GeneratedActor"
            val pack = processingEnv.elementUtils.getPackageOf(e).toString()

            val classMetadata = getClassMetadata(e)
            if (classMetadata === null) return
            val messages = classMetadata.functions.filter { it.isSuspendable() }
            val messagesClassName = ClassName.bestGuess("Messages")
            val messagesType = TypeSpec.classBuilder(messagesClassName)
                    .addModifiers(KModifier.SEALED, KModifier.PRIVATE)

            messages.forEach {
                val messageType = TypeSpec.classBuilder(it.name.capitalize())
                        .superclass(messagesClassName)

                val params = it.valueParameters.map { p -> ParameterData(p, getTypeNameFromParameter(p)) }
                if (params.isNotEmpty()) {
                    val messageConstructor = params.fold(FunSpec.constructorBuilder()) { c, p ->
                        c.addParameter(ParameterSpec(p.kmData.name, p.typeName))
                        c
                    }
                    val messageProperties = params.fold(mutableListOf<PropertySpec>()) { l, p ->
                        l.add(PropertySpec.builder(p.kmData.name, p.typeName)
                                .initializer(p.kmData.name)
                                .build())
                        l
                    }

                    messageType.primaryConstructor(messageConstructor.build())
                            .addProperties(messageProperties)
                }
                messagesType.addType(messageType.build())
            }

            val actorConstructor = FunSpec.constructorBuilder()
                    .addParameter("coroutineScope", coroutineScope.asTypeName())

            val sendChannel = SendChannel::class.asClassName().parameterizedBy(messagesClassName)
            val actorStatement = MemberName("kotlinx.coroutines.channels", "actor")

            val createActorFun = FunSpec.builder("createActor")
                    .addModifiers(KModifier.PRIVATE)
                    .receiver(coroutineScope.asTypeName())
                    .returns(sendChannel)
                    .addCode(createActorBody(messages), actorStatement)

            val actorProperty = PropertySpec.builder("actor", sendChannel)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("coroutineScope.createActor()")
                    .build()

            val actorType = TypeSpec.classBuilder(className)
                    .superclass(e.asType().asTypeName())
                    .primaryConstructor(actorConstructor.build())
                    .addProperty(actorProperty)
                    .addFunction(createActorFun.build())
                    .addType(messagesType.build())
                    .build()
            val file = FileSpec.builder(pack, className)
                    .addType(actorType)
                    .build()
            file.writeTo(processingEnv.filer)
        }
    }

    private fun getTypeNameFromParameter(p: KmValueParameter): TypeName {
        val type: KmType? = p.type
        if (type === null) {
            processingEnv.error("classifier is null")
            throw NullPointerException("classifier is null")
        }
        val typeName = when (val classifier = type.classifier) {
            is KmClassifier.Class -> classifier.name
            is KmClassifier.TypeParameter -> TODO()
            is KmClassifier.TypeAlias -> TODO()
        }
        return processingEnv.typenameFromClassifier(typeName)
    }
//    internal fun generateActors(actors: MutableSet<out Element>) {
//        if (actors.isEmpty())
//            return
//
//        actors.forEach { e ->
//            val className = "${e.simpleName}GeneratedActor"
//            val pack = processingEnv.elementUtils.getPackageOf(e).toString()
//
//            val classMetadata = getClassMetadata(e)
//            val messages = e.enclosedElements.asSequence()
//                    .filter { it.kind != ElementKind.CONSTRUCTOR }
//                    .filter { it.kind == ElementKind.METHOD }
//                    .filter { it.modifiers.contains(Modifier.PUBLIC) }
//                    .filter { !it.modifiers.contains(Modifier.FINAL) }
//                    .map { it as ExecutableElement }
//                    .filter { isSuspendable(it, classMetadata) }
//                    .toList()
//
//            val messagesClassName = ClassName.bestGuess("Messages")
//            val messagesType = TypeSpec.classBuilder(messagesClassName)
//                    .addModifiers(KModifier.SEALED, KModifier.PRIVATE)
//
//            messages.forEach {
//                val messageType = TypeSpec.classBuilder(it.simpleName.toString().capitalize())
//                        .superclass(messagesClassName)
//
//                val params = getMethodParams(it)
//                if (params.isNotEmpty()) {
//                    val messageConstructor = params.fold(FunSpec.constructorBuilder()) { c, p ->
//                        c.addParameter(p.simpleName.toString(), p.asType().asTypeName())
//                        c
//                    }
//                    val messageProperties = params.fold(mutableListOf<PropertySpec>()) { l, p ->
//                        l.add(PropertySpec.builder(p.simpleName.toString(), p.asType().asTypeName())
//                                .initializer(p.simpleName.toString())
//                                .build())
//                        l
//                    }
//
//                    messageType.primaryConstructor(messageConstructor.build())
//                            .addProperties(messageProperties)
//                }
//                messagesType.addType(messageType.build())
//            }
//
//            val actorConstructor = FunSpec.constructorBuilder()
//                    .addParameter("coroutineScope", coroutineScope.asTypeName())
//
//            val sendChannel = SendChannel::class.asClassName().parameterizedBy(messagesClassName)
//            val actorStatement = MemberName("kotlinx.coroutines.channels", "actor")
//
//            val createActorFun = FunSpec.builder("createActor")
//                    .addModifiers(KModifier.PRIVATE)
//                    .receiver(coroutineScope.asTypeName())
//                    .returns(sendChannel)
//                    .addCode(createActorBody(messages), actorStatement)
//
//            val actorType = TypeSpec.classBuilder(className)
//                    .superclass(e.asType().asTypeName())
//                    .primaryConstructor(actorConstructor.build())
//                    .addFunction(createActorFun.build())
//                    .addType(messagesType.build())
//                    .build()
//            val file = FileSpec.builder(pack, className)
//                    .addType(actorType)
//                    .build()
//            file.writeTo(processingEnv.filer)
//        }
//    }

    private fun getMethodParams(method: ExecutableElement): List<VariableElement> {
        val params = method.parameters
        return params.subList(0, params.size - 1)
    }

    private fun isSuspendable(method: ExecutableElement, classMetadata: KmClass?): Boolean {
        ////val returnTypeElement = processingEnv.getTypeElement(method.returnType)
        val parameters = method.parameters
        if (parameters.isEmpty())
            return false
        val b = parameters.last().asType()
        return processingEnv.typeUtils.isSameType(processingEnv.typeUtils.erasure(b), suspendableErasure)
    }

    private fun createActorBody(messages: List<KmFunction>): String {
        val actorWhen = messages.fold(CodeBlock.builder()) { cb, m ->
            val method = m.name
            val cls = method.capitalize()
            val params = m.valueParameters.map { p -> ParameterData(p, getTypeNameFromParameter(p)) }
            cb.indent()
            cb.addStatement("is Messages.$cls  -> super.$method(${getParamList(params)})")
            cb
        }

        return """return %M {
                |   for (msg in channel) {
                |       when (msg) {
                |           ${actorWhen.build()}
                |       }
                |   }
                |}
                |""".trimMargin()
    }

    private fun getParamList(params: List<ParameterData>): String {
        return params.joinToString(", ") { "msg.${it.kmData.name}" }
    }
}