package io.realworld.app.domain.service

import io.realworld.app.domain.Article
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.repository.ArticleRepository
import io.realworld.app.domain.repository.UserRepository
import java.util.Date

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
) {
    fun create(email: String, article: Article): Article {
        val author = userRepository.findByEmail(email)
            ?: throw NotFoundException("Author not found to create article.")
        val now = Date()
        val toCreate = article.copy(
            slug = uniqueSlug(article.title),
            createdAt = now,
            updatedAt = now,
            author = author.copy(password = null, token = null)
        )
        articleRepository.create(toCreate, author.id!!)
        return toCreate
    }

    fun search(term: String, limit: Int, offset: Int): List<Article> {
        require(term.isNotBlank()) { "Search term 'q' must not be blank." }
        return articleRepository.search(term, limit, offset).map { (article, authorId) ->
            val author = userRepository.findById(authorId)
            article.copy(author = author?.copy(password = null, token = null))
        }
    }

    private fun uniqueSlug(title: String?): String {
        require(!title.isNullOrBlank()) { "Article title must not be blank." }
        val base = title.toLowerCase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "article" }
        if (articleRepository.findBySlug(base) == null) return base
        var counter = 1
        while (articleRepository.findBySlug("$base-$counter") != null) counter++
        return "$base-$counter"
    }
}
