package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.CoroutineScope
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
                val name = it.simpleName.toString()
                val generatedActorType = ClassName(processingEnv.elementUtils.getPackageOf(it).toString(), "${name}GeneratedActor")
                val actorBean = FunSpec.builder(name.decapitalize())
                        .addAnnotation(beanAnnotation)
                        .addModifiers(KModifier.OPEN)
                        .addParameter(executorParameter)
                        .returns(it.asType().asTypeName())
                        .addStatement("return %T(%T(executor.%T()))", generatedActorType, coroutineScope, asCoroutineDispatcher)
                        .build()
                configurationType.addFunction(actorBean)
            }

            val file = FileSpec.builder(pack, className)
                    .addType(configurationType.build())
                    .build()
            file.writeTo(processingEnv.filer)
        } catch (e: Exception) {
            processingEnv.printMessage("Error in generateConfig", e)
        }
    }

    companion object {
        private val coroutineScope = CoroutineScope::class.asTypeName()
        private val asCoroutineDispatcher = ClassName("kotlinx.coroutines", "asCoroutineDispatcher")
    }
}