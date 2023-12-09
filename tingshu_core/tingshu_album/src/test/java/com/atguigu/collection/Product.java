package com.atguigu.collection;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Product {
    public int id;
    public int num;
    public int price;
    public String name;
    public String category;
}
