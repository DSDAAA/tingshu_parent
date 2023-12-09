package com.atguigu.collection;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Demo {
    public static void main(String[] args) {
        Product product1 = new Product(1, 1, 15, "面包", "零食");
        Product product2 = new Product(2, 1, 5, "饼干", "零食");
        Product product3 = new Product(3, 1, 2, "卤蛋", "零食");
        Product product4 = new Product(4, 1, 5, "勇闯天涯", "啤酒");
        Product product5 = new Product(5, 1, 10, "乌苏", "啤酒");
        ArrayList<Product> productList = Lists.newArrayList(product1, product2, product3, product4, product5);
        //对集合进行迭代
        //productList.forEach(product-> System.out.println(product));
        //productList.stream().forEach(product-> System.out.println(product));
//        Map<String, List<Product>> productMap = productList.stream().collect(Collectors.groupingBy(Product::getCategory));
//        Set<Map.Entry<String, List<Product>>> productEntry = productMap.entrySet();
//        for (Map.Entry<String, List<Product>> entry : productMap.entrySet()) {
//            String key = entry.getKey();
//            List<Product> products = entry.getValue();
//            System.out.print(key);
//            System.out.println(products);
//        }
        ArrayList<String> characterList = Lists.newArrayList("a", "b", "c", "d");
        //ArrayList<String> upperCharacter = new ArrayList<>();
//        for (String character : characterList) {
//            upperCharacter.add(character.toUpperCase());
//            System.out.println(character.toUpperCase());
//        }
//        characterList.stream().forEach(new Consumer<String>() {
//            @Override
//            public void accept(String character) {
//                upperCharacter.add(character.toUpperCase());
//            }
//        });
        List<String> upperCharacter = characterList.stream().map(character->character.toUpperCase()).collect(Collectors.toList());
        System.out.println(upperCharacter);
    }
}
