package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.CoroutineScope
import org.skunkworks.movie.annotation.Actor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ConfigurationBuilder(private val processingEnv: ProcessingEnvironment) {

    internal fun generateConfig(
            configs: MutableSet<out Element>,
            actors: MutableSet<out Element>
    ) {
        try {
            if (configs.isEmpty())
                return

            if (configs.size > 1) {
                processingEnv.printMessage("More than one class is annotated with @Movie, only ${configs.first().simpleName} will be processed.")
            }

            val configElement = configs.first()
            val className = "Cast"
            val pack = processingEnv.elementUtils.getPackageOf(configElement).toString()

            val beanClassName = ClassName("org.springframework.context.annotation", "Bean")
            val beanAnnotation = AnnotationSpec.builder(beanClassName).build()
            val executors = Executors::class.asClassName()
            val executorBean = FunSpec.builder("executor")
                    .addAnnotation(beanAnnotation)
                    .addModifiers(KModifier.OPEN)
                    .returns(ExecutorService::class)
                    .addStatement("return %T.newSingleThreadExecutor()", executors)
                    .build()

            val configurationType = TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.OPEN)
                    .addFunction(executorBean)

            val executorParameter = ParameterSpec.builder("executor", ExecutorService::class.asTypeName())
                    .build()

            actors.forEach {
                val actorAnnotation = it.getAnnotation(Actor::class.java)
                val name = it.simpleName.toString()
                val generatedActorPackage = processingEnv.elementUtils.getPackageOf(it)
                val generatedActorType = ClassName(generatedActorPackage.toString(), "${name}GeneratedActor")
                val classMetadata = getClassMetadata(it)

                if (actorAnnotation.factory) {
                    //create factory
                    val factoryName = "${name}Factory"
                    val factoryType = TypeSpec.classBuilder(factoryName)

                    val factoryFile = FileSpec.builder(generatedActorPackage.toString(), factoryName)
                            .addType(factoryType.build())
                            .build()
                    factoryFile.writeTo(processingEnv.filer)
                } else {
                    val (beanParams, generatedTypeParams) = if (classMetadata !== null) {
                        val constructor = classMetadata.constructors.first { c -> c.isPrimary() }
                        val valueParameters = constructor.valueParameters
                        val beanParams = valueParameters
                                .map { vp -> ParameterSpec.builder(vp.name, processingEnv.getClassName(vp.type)).build() }
                        val generatedTypeParams = if (valueParameters.isNotEmpty())
                            valueParameters.joinToString(", ", ", ") { vp -> vp.name }
                        else ""
                        beanParams to generatedTypeParams
                    } else {
                        emptyList<ParameterSpec>() to ""
                    }

                    val actorBean = FunSpec.builder(name.decapitalize())
                            .addAnnotation(beanAnnotation)
                            .addModifiers(KModifier.OPEN)
                            .addParameter(executorParameter)
                            .addParameters(beanParams)
                            .returns(it.asType().asTypeName())
                            .addStatement("return %T(%T(executor.%T())$generatedTypeParams)", generatedActorType, coroutineScope, asCoroutineDispatcher)
                            .build()

                    configurationType.addFunction(actorBean)
                }
            }

            val configurationFile = FileSpec.builder(pack, className)
                    .addType(configurationType.build())
                    .build()
            configurationFile.writeTo(processingEnv.filer)
        } catch (e: Exception) {
            processingEnv.printMessage("Error in generateConfig", e)
        }
    }

    companion object {
        private val coroutineScope = CoroutineScope::class.asTypeName()
        private val asCoroutineDispatcher = ClassName("kotlinx.coroutines", "asCoroutineDispatcher")
    }
}