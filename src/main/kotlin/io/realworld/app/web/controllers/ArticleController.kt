package io.realworld.app.web.controllers

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.request.receive
import io.ktor.response.respond
import io.realworld.app.domain.ArticleDTO
import io.realworld.app.domain.ArticlesDTO
import io.realworld.app.domain.User
import io.realworld.app.domain.service.ArticleService

class ArticleController(private val articleService: ArticleService) {

    fun findBy(ctx: ApplicationCall): ArticlesDTO {
        val tag = ctx.parameters["tag"]
        val author = ctx.parameters["author"]
        val favorited = ctx.parameters["favorited"]
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 0)
    }

    /**
     * GET /api/articles/search?q=<term>&limit=&offset=
     * Searches articles by title and body (case-insensitive). Requires authentication.
     */
    suspend fun search(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged." }
        val term = ctx.parameters["q"]
        require(!term.isNullOrBlank()) { "Search term 'q' is required." }
        val limit = (ctx.parameters["limit"] ?: "20").toInt()
        val offset = (ctx.parameters["offset"] ?: "0").toInt()
        articleService.search(term, limit, offset).also { articles ->
            ctx.respond(ArticlesDTO(articles, articles.size))
        }
    }

    fun feed(ctx: ApplicationCall): ArticlesDTO {
        val limit = ctx.parameters["limit"] ?: "20"
        val offset = ctx.parameters["offset"] ?: "0"
        return ArticlesDTO(listOf(), 0)
    }

    fun get(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    suspend fun create(ctx: ApplicationCall) {
        val email = ctx.authentication.principal<User>()?.email
        require(!email.isNullOrBlank()) { "User not logged." }
        ctx.receive<ArticleDTO>().article?.also { article ->
            articleService.create(email, article).also { created ->
                ctx.respond(ArticleDTO(created))
            }
        } ?: throw IllegalArgumentException("Article body is required.")
    }

    suspend fun update(ctx: ApplicationCall): ArticleDTO {
        val slug = ctx.parameters["slug"]
        ctx.receive<ArticleDTO>()
        return ArticleDTO(null)
    }

    fun delete(ctx: ApplicationCall) {
        ctx.parameters["slug"]
    }

    fun favorite(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }

    fun unfavorite(ctx: ApplicationCall): ArticleDTO {
        ctx.parameters["slug"]
        return ArticleDTO(null)
    }
}
