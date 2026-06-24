fun main() {
    val parent: java.io.File? = null
    try {
        val f = java.io.File(parent, "gemma-2b-it-cpu-int4.bin")
        println(f.absolutePath)
    } catch(e: Exception) {
        println("Crash: " + e.message)
    }
}
