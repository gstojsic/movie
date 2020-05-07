package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.CoroutineScope
import org.skunkworks.movie.annotation.Actor
import org.skunkworks.movie.annotation.Provide
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

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
                if (classMetadata === null)
                    return@forEach processingEnv.printMessage("classMetadata is null for $it")

                if (actorAnnotation.factory) {
                    //create factory
                    val factoryName = "${name}Factory"
                    val factoryClassName = ClassName(generatedActorPackage.toString(), factoryName)
                    val factoryType = TypeSpec.classBuilder(factoryClassName)

                    val constructor = it.enclosedElements
                            .filter { t -> t.kind == ElementKind.CONSTRUCTOR }
                            .map { e -> e as ExecutableElement }
                            .maxBy { e -> e.parameters.size }
                    if (constructor === null)
                        return@forEach processingEnv.printMessage("constructor not found for $it")

                    val constructorParams = constructor.parameters
                    val (provided, injected) = constructorParams
                            .partition { p -> p.getAnnotation(Provide::class.java) !== null }

                    val executorProperty = PropertySpec.builder("executor", ExecutorService::class.asTypeName())
                            .mutable(true)
                            .addModifiers(KModifier.LATEINIT, KModifier.PRIVATE)
                            .addAnnotation(autowiredAnnotation)
                            .build()

                    factoryType.addProperty(executorProperty)
                    if (injected.isNotEmpty()) {
                        injected.forEach { i ->
                            val property = PropertySpec.builder(i.simpleName.toString(), i.asType().asTypeName())
                                    .mutable(true)
                                    .addModifiers(KModifier.LATEINIT, KModifier.PRIVATE)
                                    .addAnnotation(autowiredAnnotation)
                                    .build()
                            factoryType.addProperty(property)
                        }
                    }
                    val kotlinConstructor = classMetadata.constructors.first { c -> c.isPrimary() }
                    val providedNames = provided.map { p -> p.simpleName.toString() }.toSet()
                    val kotlinProvided = kotlinConstructor.valueParameters.filter { vp -> providedNames.contains(vp.name) }
                    val createParameters = kotlinProvided.map { p ->
                        ParameterSpec.builder(p.name, processingEnv.getTypeNameFromKmType(p.type)).build()
                    }

                    val actorConstructorParams = if (constructorParams.isNotEmpty())
                        constructorParams.joinToString(", ", ", ") { p -> p.simpleName.toString() }
                    else ""

                    val factoryMethod = FunSpec.builder("create")
                            .addParameters(createParameters)
                            .returns(it.asType().asTypeName())
                            .addStatement("return %T(%T(executor.%T())$actorConstructorParams)", generatedActorType, coroutineScope, asCoroutineDispatcher)

                    factoryType.addFunction(factoryMethod.build())

                    val factoryFile = FileSpec.builder(generatedActorPackage.toString(), factoryName)
                            .addType(factoryType.build())
                            .build()
                    factoryFile.writeTo(processingEnv.filer)

                    val actorFactoryBean = FunSpec.builder(factoryName.decapitalize())
                            .addAnnotation(beanAnnotation)
                            .addModifiers(KModifier.OPEN)
                            .returns(factoryClassName)
                            .addStatement("return %T()", factoryClassName)
                            .build()

                    configurationType.addFunction(actorFactoryBean)
                } else {
                    val (beanParams, generatedTypeParams) = run {
                        val constructor = classMetadata.constructors.first { c -> c.isPrimary() }
                        val valueParameters = constructor.valueParameters
                        val beanParams = valueParameters
                                .map { vp -> ParameterSpec.builder(vp.name, processingEnv.getClassName(vp.type)).build() }
                        val generatedTypeParams = if (valueParameters.isNotEmpty())
                            valueParameters.joinToString(", ", ", ") { vp -> vp.name }
                        else ""
                        beanParams to generatedTypeParams
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
        private val beanClassName = ClassName("org.springframework.context.annotation", "Bean")
        private val beanAnnotation = AnnotationSpec.builder(beanClassName).build()
        private val autowiredClassName = ClassName("org.springframework.beans.factory.annotation", "Autowired")
        private val autowiredAnnotation = AnnotationSpec.builder(autowiredClassName).build()

        private val asCoroutineDispatcher = ClassName("kotlinx.coroutines", "asCoroutineDispatcher")
    }
}