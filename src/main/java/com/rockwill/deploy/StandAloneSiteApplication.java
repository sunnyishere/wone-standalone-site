package com.rockwill.deploy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


/**
 * 启动程序
 *
 * @author Rockwill
 */
@SpringBootApplication
public class StandAloneSiteApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(StandAloneSiteApplication.class, args);
    }
}
