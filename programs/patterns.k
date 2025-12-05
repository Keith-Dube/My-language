fun something(x: Int) {
    when (x) {
        is 0 => print("Zero");
        is 1 => print("One");
        else => print("Other");
    }
}
