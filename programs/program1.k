class Calculator {
    fun add(a: Int, b: Int): Int {
        return a + b;
    }

    fun compute() {
        let result = add(5, 10);
        print(result);
    }
}

fun main() {
    let calc = Calculator();
    calc.compute();
}
