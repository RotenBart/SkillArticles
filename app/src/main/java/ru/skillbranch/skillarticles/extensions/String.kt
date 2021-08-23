package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val indexes = mutableListOf<Int>()
    var index = 0
    while (index != -1) {
        this?.let { string ->
            if(substr.isNotEmpty()) {
                index = string.indexOf(substr, index, ignoreCase)
            } else return indexes
        }?: run {
            return indexes
        }
        if(index != -1) {
            indexes.add(index)
            index++
        }
    }
    return indexes
}