package com.example.orderpoller.dto;

import java.math.BigDecimal;
import java.util.Date;

public record OrderDTO(String name, String customerID, String productType, BigDecimal price, Date orderDate){
}
