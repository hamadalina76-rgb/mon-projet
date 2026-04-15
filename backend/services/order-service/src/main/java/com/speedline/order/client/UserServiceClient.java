package com.speedline.order.client;

import com.speedline.order.client.dto.AddressSnapshot;
import com.speedline.order.client.dto.CustomerSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/customers/by-user/{userId}")
    CustomerSnapshot getCustomerByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/addresses/{id}")
    AddressSnapshot getAddressById(@PathVariable("id") Long id);
}
