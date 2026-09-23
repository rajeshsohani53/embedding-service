package com.raj.embeddingservice.service.EmbeddingService;

import java.util.Arrays;

public class Tasj {
public static void main(String[] args) {
	int[] arr= {1,2,3,4,5,5,6,7,7,8,998,747};
	Arrays.stream(arr).filter(i->i%2!=0).map(i->i*2).forEach(System.out::println);
}
}
