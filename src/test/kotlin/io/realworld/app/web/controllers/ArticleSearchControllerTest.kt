package io.realworld.app.web.controllers

import com.mashape.unirest.http.Unirest
import io.realworld.app.domain.Article
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.web.rules.AppRule
import io.realworld.app.web.util.HttpUtil
import org.apache.http.HttpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArticleSearchControllerTest {
    @Rule
    @JvmField
    val appRule = AppRule()

    @Test
    fun `search returns article matching title term`() {
        val email = "search_title@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "search_title_user")
        appRule.http.loginAndSetTokenHeader(email, password)
        val article = Article(
            title = "UniqueSearchTitle Dragon Training",
            description = "A description",
            body = "Body content here.",
            tagList = listOf("search-title-tag")
        )
        appRule.http.post<ArticleDTO>("/api/articles", ArticleDTO(article))

        val response = appRule.http.get<ArticlesDTO>("/api/articles/search?q=UniqueSearchTitle")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articlesCount >= 1)
        assertTrue(response.body.articles.any { it.title == article.title })
    }

    @Test
    fun `search finds article by body term case-insensitively`() {
        val email = "search_body@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "search_body_user")
        appRule.http.loginAndSetTokenHeader(email, password)
        val article = Article(
            title = "Another Article Title",
            description = "Some description",
            body = "The XYZQWERTY keyword appears only in the body.",
            tagList = listOf("search-body-tag")
        )
        appRule.http.post<ArticleDTO>("/api/articles", ArticleDTO(article))

        val response = appRule.http.get<ArticlesDTO>("/api/articles/search?q=xyzqwerty")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertTrue(response.body.articlesCount >= 1)
        assertTrue(response.body.articles.any { it.title == article.title })
    }

    @Test
    fun `search without authentication returns 401`() {
        val http = HttpUtil(appRule.port)
        val response = Unirest.get("${http.origin}/api/articles/search?q=anything")
            .headers(http.headers)
            .asString()

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `search with blank query returns 422`() {
        val email = "search_blank@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "search_blank_user")
        appRule.http.loginAndSetTokenHeader(email, password)

        val response = Unirest.get("${appRule.http.origin}/api/articles/search?q=")
            .headers(appRule.http.headers)
            .asString()

        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.status)
    }

    @Test
    fun `search with no matching term returns 200 with empty results`() {
        val email = "search_nomatch@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "search_nomatch_user")
        appRule.http.loginAndSetTokenHeader(email, password)

        val response = appRule.http.get<ArticlesDTO>("/api/articles/search?q=ZZZNOMATCHTERM999")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(0, response.body.articlesCount)
        assertTrue(response.body.articles.isEmpty())
    }

    @Test
    fun `search honors the limit pagination parameter`() {
        val email = "search_page@valid_email.com"
        val password = "Test"
        appRule.http.registerUser(email, password, "search_page_user")
        appRule.http.loginAndSetTokenHeader(email, password)
        listOf("Paginationmarker one", "Paginationmarker two").forEach { title ->
            appRule.http.post<ArticleDTO>(
                "/api/articles",
                ArticleDTO(Article(title = title, description = "d", body = "b", tagList = listOf("p")))
            )
        }

        val response = appRule.http.get<ArticlesDTO>("/api/articles/search?q=paginationmarker&limit=1")

        assertEquals(HttpStatus.SC_OK, response.status)
        assertEquals(1, response.body.articles.size)
        assertEquals(response.body.articles.size, response.body.articlesCount)
    }
}
