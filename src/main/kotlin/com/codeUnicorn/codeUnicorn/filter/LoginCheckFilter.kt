package com.codeUnicorn.codeUnicorn.filter

import com.codeUnicorn.codeUnicorn.constant.ExceptionMessage
import com.codeUnicorn.codeUnicorn.domain.ErrorResponse
import com.codeUnicorn.codeUnicorn.domain.user.User
import com.codeUnicorn.codeUnicorn.exception.UserAccessForbiddenException
import com.codeUnicorn.codeUnicorn.exception.UserUnauthorizedException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class LoginCheckFilter : Filter {
    // 인증이 필요한 요청 Url 정규표현식 목록
    // POST /courses/{courseId} GET /courses/{courseId}/lectures/{lectureId}
    // GET /users/{userId}, PATCH /users/{userId}/nickname, PATCH /users/{userId}/profile, GET /users/{userId}/courses
    private val authList: Array<Regex> = arrayOf(
        Regex("/courses/(\\w|\\d|\\s)*"),
        Regex("/courses/(\\w|\\d|\\s)*/lectures/(\\w|\\d|\\s)*"),
        Regex("^/users/[(\\w|\\d|\\s)*|^(login|logout)]"),
        Regex("^/users/(\\w|\\d|\\s)*/(info|courses)$")
    )

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpRequest: HttpServletRequest = request as HttpServletRequest
        val requestURI = httpRequest.requestURI
        val requestMethod = httpRequest.method
        val httpResponse: HttpServletResponse = response as HttpServletResponse

        try {
            log.info("인증 체크 필터 시작 {}", requestURI)
            if (isLoginCheckPath(requestURI, requestMethod)) {
                log.info("인증 체크 로직 실행 {}", requestURI)
                // 로그인 세션이 존재하면 세션 반환, 세션이 존재하지 않으면 null 값 반환
                val session = httpRequest.getSession(false)

                // 현재 로그인하지 않은 사용자의 접근 시 401 Unauthorized 에러 발생
                if (session?.getAttribute("user") == null) {
                    log.info("미인증 사용자 요청 {}", requestURI)

                    throw UserUnauthorizedException(ExceptionMessage.UNAUTHORIZED_USER_CANNOT_ACCESS)
                }

                val userInfoInSession: User? =
                    jacksonObjectMapper().readValue(session.getAttribute("user").toString(), User::class.java)

                // userId 에 대한 유효성 검증 처리
                if (requestURI.contains("/users")) {
                    /*
                        /users/{userId} || /users/{userId}/nickname || /users/{userId}/profile
                        userId 가 숫자인 경우 true
                    */
                    val regex = "/users/(\\d+)/?\\w*".toRegex()

                    // request URI 에 로그인한 사용자의 userId 가 포함되어 있지 않으면 403 Forbidden 에러 발생
                    if (requestURI.matches(regex) && !requestURI.contains(userInfoInSession?.id.toString())) {
                        throw UserAccessForbiddenException(ExceptionMessage.CURRENT_USER_CANNOT_ACCESS)
                    }
                }
            }
            chain?.doFilter(request, response)
        } catch (e: UserUnauthorizedException) {
            httpResponse.status = HttpStatus.UNAUTHORIZED.value()
            httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
            httpResponse.characterEncoding = "UTF-8"
            // ErrorResponse
            val errorResponse = ErrorResponse().apply {
                this.status = HttpStatus.UNAUTHORIZED.value()
                this.message = e.message.toString()
                this.method = request.method
                this.path = request.requestURI.toString()
            }
            jacksonObjectMapper().writeValue(httpResponse.writer, errorResponse)
        } catch (e: UserAccessForbiddenException) {
            httpResponse.status = HttpStatus.FORBIDDEN.value()
            httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
            httpResponse.characterEncoding = "UTF-8"
            // ErrorResponse
            val errorResponse = ErrorResponse().apply {
                this.status = HttpStatus.FORBIDDEN.value()
                this.message = e.message.toString()
                this.method = request.method
                this.path = request.requestURI.toString()
            }
            jacksonObjectMapper().writeValue(httpResponse.writer, errorResponse)
        }
    }

    // 사용자 인증이 필요한 경로인지 체크
    private fun isLoginCheckPath(requestURI: String, requestMethod: String): Boolean {
        var isAuthNeed = false
        authList.forEach { regex ->
            if (regex.matches(requestURI)) {
                isAuthNeed = true
            }
        }
        // GET /courses/{courseId} 인 경우 인증 필요하지 않음.
        if (requestURI.matches(Regex("^/courses/(\\w|\\d|\\s)*/?")) && requestMethod == "GET") isAuthNeed = false
        return isAuthNeed
    }
}
