data class Habit(
    var id: Long? = null,
    var groupId: Long? = null,
    var name: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Habit) return false
        if (id != other.id) return false
        if (groupId != other.groupId) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (groupId?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        return result
    }
}

fun main() {
    val h1 = Habit(1L, null, "Test")
    val list = mutableListOf<Habit>()
    list.add(h1)
    
    println("List contains h1 initially: " + list.contains(h1))
    
    h1.groupId = 5L
    
    println("List contains h1 after mutation: " + list.contains(h1))
    
    val removed = list.remove(h1)
    
    println("Removed: $removed, list size: ${list.size}")
}
