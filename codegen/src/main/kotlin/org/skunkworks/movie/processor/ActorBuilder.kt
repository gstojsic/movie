package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.metadata.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import com.squareup.kotlinpoet.ClassName as KotlinpoetClassName

internal class ActorBuilder(private val processingEnv: ProcessingEnvironment) {

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

                    val messageConstructor = FunSpec.constructorBuilder()
                    val returnTypeName = processingEnv.getTypeNameFromKmType(it.returnType)
                    val params = it.valueParameters.map { p -> ParameterData(p, getTypeNameFromParameter(p)) }
                    if (params.isNotEmpty()) {
                        params.forEach { p ->
                            messageConstructor.addParameter(ParameterSpec(p.kmData.name, p.typeName))
                        }
                        val messageProperties = params.fold(mutableListOf<PropertySpec>()) { l, p ->
                            l.add(PropertySpec.builder(p.kmData.name, p.typeName)
                                    .initializer(p.kmData.name)
                                    .build())
                            l
                        }

                        messageType.addProperties(messageProperties)
                    }
                    if (returnTypeName !== UNIT) {
                        val completableDeferred = CompletableDeferred::class.asClassName().parameterizedBy(returnTypeName)
                        messageConstructor.addParameter("response", completableDeferred)
                        messageType.addProperty(PropertySpec.builder("response", completableDeferred)
                                .initializer("response")
                                .build())
                    }
                    messageType.primaryConstructor(messageConstructor.build())
                    messagesType.addType(messageType.build())
                }

                val constructor = classMetadata.constructors.first { it.isPrimary() }
                val actorConstructor = FunSpec.constructorBuilder()
                        .addParameter("coroutineScope", coroutineScope)
                constructor.valueParameters.forEach { actorConstructor.addParameter(it.name, processingEnv.getClassName(it.type)) }
                val superclassConstructorParams = constructor.valueParameters.joinToString(", ") { it.name }

                val sendChannel = SendChannel::class.asClassName().parameterizedBy(messagesClassName)
                val actorStatement = MemberName("kotlinx.coroutines.channels", "actor")

                val actorProperty = PropertySpec.builder("actor", sendChannel)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("coroutineScope.createActor()")
                        .build()

                val createActorFun = FunSpec.builder("createActor")
                        .addModifiers(KModifier.PRIVATE)
                        .receiver(coroutineScope)
                        .returns(sendChannel)
                        .addCode(createActorBody(messages), actorStatement)

                val overrides = generateOverrides(messages)

                val actorType = TypeSpec.classBuilder(className)
                        .superclass(e.asType().asTypeName())
                        .addSuperclassConstructorParameter(superclassConstructorParams)
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
            processingEnv.printMessage("Error in generateActors", e)
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
            val returnTypeName = processingEnv.getTypeNameFromKmType(it.returnType)
            if (returnTypeName !== UNIT) {
                overrideFn.returns(returnTypeName)
                val completableDeferred = CompletableDeferred::class.asClassName().parameterizedBy(returnTypeName)
                overrideFn.addStatement("val response = %T()", completableDeferred)
            }
            val invokeParams = params.map { pd -> pd.kmData.name }

            val allParams = if (returnTypeName !== UNIT)
                invokeParams + "response"
            else
                invokeParams

            val allParamsStr = allParams.joinToString(", ")
            overrideFn.addStatement("actor.send(Messages.${it.name.capitalize()}($allParamsStr))")

            if (returnTypeName !== UNIT) {
                overrideFn.addStatement("return response.await()")
            }

            overrideFn.build()
        }
    }

    private fun getTypeNameFromParameter(p: KmValueParameter): TypeName {
        return processingEnv.getTypeNameFromKmType(p.type)
    }

    private fun createActorBody(messages: List<KmFunction>): String {
        val actorWhen = messages.fold(CodeBlock.builder().indent().indent().indent()) { cb, m ->
            val method = m.name
            val cls = method.capitalize()
            val params = m.valueParameters.map { p -> ParameterData(p, getTypeNameFromParameter(p)) }
            val returnTypeName = processingEnv.getClassName(m.returnType)

            if (returnTypeName !== UNIT) {
                cb.addStatement("is Messages.$cls -> msg.response.complete(super.$method(${getParamList(params)}))")
            } else {
                cb.addStatement("is Messages.$cls -> super.$method(${getParamList(params)})")
            }
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

    companion object {
        private val coroutineScope = CoroutineScope::class.asTypeName()
    }
}