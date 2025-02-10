package org.javacs.kt.classpath

import com.google.gson.*
import java.io.File
import java.lang.reflect.Type

data class Analysis(
    val classes: Map<String, ClassInfo> // Key is fully qualified class name
)

/**
 * ClassInfo holds the information about a clas
 */
data class ClassInfo(
    val name: String,
    val packageName: String,
    val methods: List<MethodInfo>,
    val superClass: String?,
    val interfaces: List<String>,
    val sourceJars: List<String>,
)

data class MethodInfo(
    val name: String,
)

class JarMetadata {
    companion object {
        @JvmStatic
        fun fromMetadataJsonFile(metadataJsonFile: File?): Analysis? {
            val gson = Gson()
            val jsonContent = metadataJsonFile?.readText()
            if (jsonContent.isNullOrBlank()) return null
            return gson.fromJson(jsonContent, Analysis::class.java)
        }

        fun readAnalysisFromJsonConfigured(jsonPath: String): Analysis {
            val gson = GsonBuilder()
                .registerTypeAdapter(Analysis::class.java, AnalysisDeserializer())
                .create()

            val jsonContent = File(jsonPath).readText()
            return gson.fromJson(jsonContent, Analysis::class.java)
        }

        // Optional deserializer if you need custom deserialization logic
        class AnalysisDeserializer : JsonDeserializer<Analysis> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Analysis {
                val jsonObject = json.asJsonObject
                val classesMap = mutableMapOf<String, ClassInfo>()

                jsonObject.getAsJsonObject("classes").entrySet().forEach { (key, value) ->
                    classesMap[key] = context.deserialize(value, ClassInfo::class.java)
                }

                return Analysis(classesMap)
            }
        }
    }
}
