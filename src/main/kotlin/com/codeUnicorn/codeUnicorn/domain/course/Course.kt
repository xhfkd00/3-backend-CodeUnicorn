package com.codeUnicorn.codeUnicorn.domain.course

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "course")
data class Course(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "instructor_id")
    val instructorId: Int? = 0,
    @Column(length = 30)
    var category: String? = "",
    var type: Int? = 0,
    @Column(length = 30)
    var name: String? = "",
    @Column(length = 255)
    var description: String? = "",
    var price: Int? = 0,
    @Column(name = "discount_rate")
    var discountRate: Int? = 0,
    @Column(length = 255, name = "image_path")
    var imagePath: String? = "",
    @Column(name = "view_count")
    var viewCount: Int? = 0,
    @Column(name = "total_hours")
    var totalHours: Int? = 0,
    @Column(name = "lecture_count")
    var lectureCount: Int? = 0,
    @Column(name = "average_ratings")
    var averageRatings: Int? = 0,
    @Column(name = "user_count")
    var userCount: Int? = 0,
    @Column(name = "created_at")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    var createdAt: LocalDateTime? = null,
    @Column(name = "updated_at")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    var updatedAt: LocalDateTime? = null
)

@Entity
@Table(name = "course")
data class CourseInfo(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    var category: String,
    var type: Int,
    var name: String,
    @Column(name = "image_path")
    var imagePath: String?,
    @Column(name = "average_ratings")
    var averageRatings: Int?,
    @Column(name = "ratings_count")
    var ratingsCount: Int,
    @Column(name = "user_count")
    var userCount: Int?
)