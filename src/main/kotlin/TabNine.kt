import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import types.AutocompleteResult
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.*

const val VERSION = "2.0.2"
const val BINARY_PATH = "/usr/local/tabnine/$VERSION/TabNine"

object TabNine {
    var process: Process? = null
    var stdinWriter: BufferedWriter? = null
    var numRestarts = 0

    private fun getBinaryPath(): String {
        var path = BINARY_PATH

        val os = System.getProperty("os.name")
        if(os.startsWith("Windows")){
            path += ".exe"
        }

        return BINARY_PATH
    }

    private fun restartChild(){
        if(numRestarts >= 15) return
        numRestarts++
        process?.run { destroy() }
        process = ProcessBuilder(getBinaryPath()).start()
        stdinWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        println("TabNine process started.")
    }

    private fun isChildAlive(): Boolean {
        var alive = false

        process?.run {
            alive = isAlive
        }

        return alive
    }

    fun request(filePath: String, before: String, after: String, includesStart: Boolean, includesEnd: Boolean, maxResults: Int): AutocompleteResult? {
        if(!isChildAlive()){
            restartChild()
        }

        if(!isChildAlive()){
            throw Exception("TabNine process is dead.")
        }

        return process?.let {
            val request = json {
                obj(
                    "version" to VERSION,
                    "request" to obj(
                        "Autocomplete" to obj(
                            "filename" to filePath,
                            "before" to before,
                            "after" to after,
                            "region_includes_beginning" to includesStart,
                            "region_includes_end" to includesEnd,
                            "max_num_results" to maxResults
                        )
                    )
                )
            }

            val requestString = request.toJsonString()

            println(requestString)

            var result: AutocompleteResult? = null

            stdinWriter?.run {
                write(requestString)
                write("\n")
                flush()

                val scanner = Scanner(it.inputStream)
                if(scanner.hasNextLine()){
                    val line = scanner.nextLine()

                    println(line)

                    result = Klaxon().parse<AutocompleteResult>(line)
                }
            }

            result
        }
    }
}