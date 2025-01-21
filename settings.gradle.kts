plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "kotlin_springboot_portfolio_board"

// (모듈 모음)
// !!!모듈 추가/수정시 아래에 반영!!!

// 인증/인가 서버 (11000)
include("module-auth")

// 게시판 서비스 (13000)
include("module-board")

// Spring Cloud 게이트웨이 (8080)
include("module-cloud-gateway")

// Spring Cloud 유레카 서버 (10001)
include("module-cloud-eureka")
