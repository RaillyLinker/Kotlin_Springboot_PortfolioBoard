package com.raillylinker

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer
import java.util.*

@ComponentScan(
    // !!!Bean 스캔할 모듈들의 패키지 리스트(group) 추가하기!!!
    basePackages =
        [
            "com.raillylinker"
        ]
)
@SpringBootApplication
@EnableEurekaServer
class ApplicationMain {
    @Bean
    fun init() = CommandLineRunner {
        // 서버 타임존 명시적 설정 (UTC, Asia/Seoul, ...)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        // println("Current TimeZone: ${TimeZone.getDefault().id}")
    }
}

fun main(args: Array<String>) {
    runApplication<ApplicationMain>(*args)
}