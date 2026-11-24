package io.github.smallora.sharereborn.data.model

/** 微博用户（只保留渲染需要的字段） */
data class WeiboUser(
    val id: String = "",
    val name: String = "",
    val avatar: String = "",
    val verified: Boolean = false,
    val verifiedType: Int = -1,
)

/** 单张配图 */
data class WeiboPic(
    val large: String = "",
    val thumb: String = "",
)

/** 卡片型附加内容（视频 / 文章 / 链接等） */
data class PageInfo(
    val objectType: String = "",
    val type: Int = -1,
    val cover: String = "",
    val title: String = "",
    val desc: String = "",
)

/** 一条微博（含转发原微博） */
data class WeiboStatus(
    val id: String = "",
    val mblogid: String = "",
    val text: String = "",
    val createdAt: String = "",
    val user: WeiboUser = WeiboUser(),
    val pics: List<WeiboPic> = emptyList(),
    val pageInfo: PageInfo? = null,
    val retweeted: WeiboStatus? = null,
    val commentsCount: Int = 0,
    val attitudesCount: Int = 0,
    val repostsCount: Int = 0,
)

/** 一条评论 */
data class WeiboComment(
    val id: String = "",
    val text: String = "",
    val createdAt: String = "",
    val user: WeiboUser = WeiboUser(),
    val likeCount: Int = 0,
    val replyText: String = "",
    val replyUser: String = "",
)

/** 分页结果 */
data class Page<T>(
    val items: List<T>,
    val maxId: String? = null,
)
