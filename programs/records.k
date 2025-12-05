record Point(x: Int, y: Int);

record Circle(center: Point, radius: Float) {
    fun area(): Float {
        return 3.14 * radius * radius;
    }
}
