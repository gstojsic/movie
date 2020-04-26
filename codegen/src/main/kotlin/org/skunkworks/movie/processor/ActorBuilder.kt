package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

internal class ActorBuilder(private val roundEnv: RoundEnvironment,
                            private val processingEnv: ProcessingEnvironment) {
    internal fun generateActors(actors: MutableSet<out Element>) {
        if (actors.isEmpty())
            return
        actors.forEach {
            val className = "${it.simpleName}GeneratedActor"
            val pack = processingEnv.elementUtils.getPackageOf(it).toString()

            val actorType = TypeSpec.classBuilder(className).build()
            val file = FileSpec.builder(pack, className)
                    .addType(actorType)
                    .build()
            file.writeTo(processingEnv.filer)

            //generateClass(className, template(pack, className))
        }
    }
//
//
//    private fun generateClass(fileName: String, content: String) {
//        val file = File(outputDir, "$fileName.kt")
//        file.writeText(content)
//    }
//
//    private fun template(
//            packageName: String,
//            className: String
//    ) = """
//    package $packageName
//
//    import kotlinx.coroutines.CoroutineScope
//    import kotlinx.coroutines.channels.actor
//
//    class $className {
//        fun bla() {
//        }
//    }
//    """.trimIndent()
}