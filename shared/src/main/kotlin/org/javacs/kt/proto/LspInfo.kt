package org.javacs.kt.proto

import org.bazelkls.proto.KotlinLsp.KotlinLspBazelTargetInfo
import com.google.protobuf.util.JsonFormat
import java.nio.file.Path

object LspInfo {

    fun fromJson(jsonFile: Path): KotlinLspBazelTargetInfo {
        val targetInfo = KotlinLspBazelTargetInfo.newBuilder()
        val protoJson = jsonFile.toFile().readText()
        JsonFormat.parser().ignoringUnknownFields().merge(protoJson, targetInfo)
        return targetInfo.build()
    }
}
