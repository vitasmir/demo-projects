package com.example.json;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Lambdas {

    public static void main(String[] args) {
        // Example of using a lambda expression to create a Runnable
        Runnable runnable = () -> System.out.println("Hello from a lambda!");
        runnable.run();

        // Example of using a lambda expression with a functional interface
        Function<Integer, Integer> squareFunction = x -> x * x;
        int result = squareFunction.apply(5);
        System.out.println("Square of 5 is: " + result);

        Apple redApple = new Apple("Red", 150.0);
        Apple greenApple = new Apple("Green", 150.0);
        Apple yellowApple = new Apple("Yellow", 150.0);

        System.out.println("Red apple color: " + redApple.getColor());
        System.out.println("Green apple color: " + greenApple.getColor());
        System.out.println("Yellow apple color: " + yellowApple.getColor());

        ArrayList<Apple> apples = new ArrayList<>();
        apples.add(redApple);
        apples.add(redApple);
        apples.add(redApple);
        apples.add(greenApple);
        apples.add(yellowApple);

        List<Apple> greenApples = apples.stream()
                .filter(apple -> "Green".equalsIgnoreCase(apple.getColor()))
                .collect(Collectors.toList());

        System.out.println("Number of green apples: " + greenApples.size());

        BiFunction<Integer, Integer, Integer> addFunction = (a, b) -> a + b;
        java.util.function.Consumer<Integer> printConsumer = value -> System.out.println("Value: " + value);

        int sum = addFunction.apply(10, 20);
        printConsumer.accept(sum);

        System.out.println("Sum of 10 and 20 is: " + (addFunction.apply(10, 20)));

        printAppleRedColors(apples, apple -> System.out.println("Apple color: " + apple.getColor()));
        

        Function<Apple, Integer> redAppleCounter = apple -> { return "Red".equals(apple.getColor()) ? 1 : 0; };
        System.out.println("Number of red apples: " + printRedAppleColorsCount(apples, redAppleCounter));
        
        
        System.out.println("Number of red apples2: " + printRedAppleColorsCount0(apples, apple1 -> "Red".equals(apple1.getColor()) ? 1 : 0));
        System.out.println("Number of red apples3 " + printRedAppleColorsCount(apples, redAppleCounter));
        System.out.println("Number of red apples4: " + printRedAppleColorsCount2(apples, redAppleCounter));
    }


    public static void printAppleRedColors(List<Apple> apples, Consumer<Apple> consumer) {
        apples.forEach(consumer);
    }

    public static Integer printRedAppleColorsCount0(List<Apple> apples, Function<Apple, Integer> func) {
        int redCount = 0;
        for (Apple apple : apples) {
            redCount += func.apply(apple);
        }
        return redCount;
    }

    public static Integer printRedAppleColorsCount(List<Apple> apples, Function<Apple, Integer> func) {
        int redCount = 0;
        for (Apple apple : apples) {
            redCount += func.apply(apple);
        }
        return redCount;
    }

    public static Integer printRedAppleColorsCount2(List<Apple> apples, Function<Apple, Integer> func) {
        int redCount = 0;
        for (Apple apple : apples) {
            redCount += func.apply(apple);
        }
        System.out.println("Number of red apples: " + redCount);
        return redCount;
    }
 
}

class Apple {

    private String color;
    private double weight;

    public Apple(String color, double weight) {
        this.color = color;
        this.weight = weight;
    }

    public String getColor() {
        return color;
    }

    public double getWeight() {
        return weight;
    }
}
