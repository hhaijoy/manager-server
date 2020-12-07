package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/15.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddDto {

    private String userName;
    private String password;
}
