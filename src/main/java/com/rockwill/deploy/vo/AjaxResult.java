package com.rockwill.deploy.vo;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Objects;

@Data
public class AjaxResult <T>
{
     private Integer code;
     private T data;
     private String msg;
}
