package org.skunkworks.movie.processor

import com.google.auto.service.AutoService
import org.skunkworks.movie.annotation.Actor
import org.skunkworks.movie.annotation.Movie
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_11)
internal class MovieProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Actor::class.java.name)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val actorElements = roundEnv.getElementsAnnotatedWith(Actor::class.java)
        if (actorElements.isEmpty()) return false

        ActorBuilder(processingEnv).generateActors(actorElements)

        val configElements = roundEnv.getElementsAnnotatedWith(Movie::class.java)
        if (configElements.isEmpty()) return false

        ConfigurationBuilder(processingEnv).generateConfig(configElements, actorElements)
        return true
    }
}