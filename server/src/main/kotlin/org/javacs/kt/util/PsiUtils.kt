package org.javacs.kt.util

import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.nio.file.Path

inline fun<reified Find> PsiElement.findParent() =
        this.parentsWithSelf.filterIsInstance<Find>().firstOrNull()

fun PsiElement.preOrderTraversal(shouldTraverse: (PsiElement) -> Boolean = { true }): Sequence<PsiElement> {
    val root = this

    return sequence {
        if (shouldTraverse(root)) {
            yield(root)

            for (child in root.children) {
                if (shouldTraverse(child)) {
                    yieldAll(child.preOrderTraversal(shouldTraverse))
                }
            }
        }
    }
}

fun PsiFile.toPath(): Path =
        winCompatiblePathOf(this.originalFile.viewProvider.virtualFile.path)

fun PsiElement.getRange(): Range {
    val document = containingFile.viewProvider.document
        ?: return Range(Position(0, 0), Position(0, 0))

    val startOffset = textRange.startOffset
    val endOffset = textRange.endOffset

    val startLine = document.getLineNumber(startOffset)
    val startLineStart = document.getLineStartOffset(startLine)
    val startColumn = startOffset - startLineStart

    val endLine = document.getLineNumber(endOffset)
    val endLineStart = document.getLineStartOffset(endLine)
    val endColumn = endOffset - endLineStart

    return Range(
        Position(startLine, startColumn),
        Position(endLine, endColumn)
    )
}
