package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.metadata.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import com.squareup.kotlinpoet.ClassName as KotlinpoetClassName

internal class ActorBuilder(private val roundEnv: RoundEnvironment,
                            private val processingEnv: ProcessingEnvironment) {

    private val coroutineScope = processingEnv.getTypeElement(CoroutineScope::class.java.typeName).asType()

    internal fun generateActors(actors: MutableSet<out Element>) {
        try {
            if (actors.isEmpty())
                return

            actors.forEach { e ->
                val className = "${e.simpleName}GeneratedActor"
                val pack = processingEnv.elementUtils.getPackageOf(e).toString()

                val classMetadata = getClassMetadata(e)
                if (classMetadata === null) return
                val messages = classMetadata.functions
                        .filter { it.isSuspendable() }
                        .filter { it.isOpen() }
                        .filter { it.isPublic() }
                val messagesClassName = KotlinpoetClassName.bestGuess("Messages")
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

                val actorProperty = PropertySpec.builder("actor", sendChannel)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("coroutineScope.createActor()")
                        .build()

                val createActorFun = FunSpec.builder("createActor")
                        .addModifiers(KModifier.PRIVATE)
                        .receiver(coroutineScope.asTypeName())
                        .returns(sendChannel)
                        .addCode(createActorBody(messages), actorStatement)

                val overrides = generateOverrides(messages)

                val actorType = TypeSpec.classBuilder(className)
                        .superclass(e.asType().asTypeName())
                        .primaryConstructor(actorConstructor.build())
                        .addProperty(actorProperty)
                        .addFunction(createActorFun.build())
                        .addFunctions(overrides)
                        .addType(messagesType.build())
                        .build()
                val file = FileSpec.builder(pack, className)
                        .addType(actorType)
                        .build()
                file.writeTo(processingEnv.filer)
            }
        } catch (e: Exception) {
            processingEnv.error("Error in generateActors", e)
        }
    }

    private fun generateOverrides(messages: List<KmFunction>): List<FunSpec> {
        return messages.map {
            val overrideFn = FunSpec.builder(it.name)
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)

            val params = it.valueParameters.map { p -> ParameterData(p, getTypeNameFromParameter(p)) }
            params.forEach { p ->
                overrideFn.addParameter(p.kmData.name, p.typeName)
            }
            val r = extractName(it.returnType)
            val returnTypeName = getTypeName(it.returnType)
//            if(it.returnType.)
//               return

            overrideFn.build()
        }
    }

    private fun getTypeNameFromParameter(p: KmValueParameter): TypeName {
        val type: KmType? = p.type
        return getTypeName(type)
    }

    private fun getTypeName(type: KmType?): TypeName {
        if (type === null) {
            processingEnv.error("classifier is null")
            throw NullPointerException("classifier is null")
        }
        val typeName = extractName(type)
        return processingEnv.typenameFromClassifier(typeName)
    }

    private fun extractName(type: KmType): ClassName = when (val classifier = type.classifier) {
        is KmClassifier.Class -> classifier.name
        is KmClassifier.TypeParameter -> TODO()
        is KmClassifier.TypeAlias -> TODO()
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