package com.yuyan.imemodule.entity

/**
 * 字符串队列
 * 存入：当队列满时，移除最早进入的（即队头），然后加入新元素到队尾。
 * 取出：按照进入队列的倒序，即从最晚进入的开始取出（也就是从队尾开始取）。
 */
class StringQueue(private val maxSize: Int = 10) {
    private val items = ArrayDeque<String>()

    @Synchronized
    fun push(item: String) {
        if (items.size >= maxSize) {
            items.removeFirst()
        }
        items.add(item)
    }

    // 倒序取出并移除元素（消费性取出）
    @Synchronized
    fun popInReverseOrder(): String? {
        return items.removeLastOrNull()
    }

    @Synchronized
    fun size(): Int = items.size

    @Synchronized
    fun isEmpty(): Boolean = items.isEmpty()

    @Synchronized
    fun clear() {
        items.clear()
    }
}