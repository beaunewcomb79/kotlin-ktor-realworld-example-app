package io.realworld.app.domain.repository

import io.realworld.app.domain.Article
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Date

internal object Articles : LongIdTable() {
    val slug: Column<String> = varchar("slug", 255).uniqueIndex()
    val title: Column<String> = varchar("title", 255)
    val description: Column<String?> = varchar("description", 1000).nullable()
    val body: Column<String> = text("body")
    val authorId: Column<Long> = long("author_id")
    val createdAt: Column<Long> = long("created_at")
    val updatedAt: Column<Long> = long("updated_at")

    fun toDomain(row: ResultRow): Article {
        return Article(
            slug = row[slug],
            title = row[title],
            description = row[description],
            body = row[body],
            createdAt = Date(row[createdAt]),
            updatedAt = Date(row[updatedAt])
        )
    }
}

class ArticleRepository {
    init {
        transaction {
            SchemaUtils.create(Articles)
        }
    }

    fun findBySlug(slug: String): Article? = transaction {
        Articles.select { Articles.slug eq slug }
            .map { Articles.toDomain(it) }
            .firstOrNull()
    }

    fun create(article: Article, authorId: Long): Long = transaction {
        Articles.insertAndGetId { row ->
            row[slug] = article.slug!!
            row[title] = article.title!!
            row[description] = article.description
            row[body] = article.body
            row[Articles.authorId] = authorId
            row[createdAt] = article.createdAt?.time ?: 0
            row[updatedAt] = article.updatedAt?.time ?: 0
        }.value
    }

    /**
     * Case-insensitive search over title and body, ordered by most recent first.
     * Each returned [Article] carries its [authorId] so the service can resolve the author.
     */
    fun search(term: String, limit: Int, offset: Int): List<Pair<Article, Long>> = transaction {
        val pattern = "%${term.lowercase()}%"
        Articles.select {
            (Articles.title.lowerCase() like pattern) or (Articles.body.lowerCase() like pattern)
        }
            .orderBy(Articles.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit, offset)
            .map { Articles.toDomain(it) to it[Articles.authorId] }
    }
}
