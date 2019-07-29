import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ex.ApplicationUtil.runWithCheckCanceled
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import java.util.concurrent.Callable
import kotlin.math.max
import kotlin.math.min

const val CHAR_LIMIT = 10_000
const val MAX_RESULTS = 5
const val DEFAULT_DETAIL = "TabNine"

class CompletionHandler : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val callable = Callable {
            val doc = parameters.editor.document
            val documentLength = doc.textLength

            val offset = parameters.offset
            val beforeStartOffset = max(0, offset - CHAR_LIMIT)
            val afterEndOffset = min(documentLength, offset + CHAR_LIMIT)
            val before = doc.getText(TextRange.create(beforeStartOffset, offset))
            val after = doc.getText(TextRange.create(offset, afterEndOffset))
            val includesStart = beforeStartOffset == 0
            val includesEnd = afterEndOffset == documentLength

            val filePath = parameters.originalFile.virtualFile.toString();

            val response = TabNine.request(
                filePath.substring(7),
                before,
                after,
                includesStart,
                includesEnd,
                MAX_RESULTS)

            if(response != null && response.results.isNotEmpty()){
                val detailMessage = if (response.user_message.isNotEmpty()) response.user_message.joinToString("\n") else DEFAULT_DETAIL

                result.addAllElements(response.results.map {
                    println("Add entry: ${it.new_prefix}")

                    LookupElementBuilder.create(it.new_prefix).apply {
                        withIcon(IconLoader.getIcon("/icons/small_logo.png"))
                    }
                })
            }
        }

        runWithCheckCanceled(callable, EmptyProgressIndicator())
    }
}