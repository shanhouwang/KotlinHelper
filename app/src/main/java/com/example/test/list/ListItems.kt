package com.example.test.list

// ListItem 是所有列表项的父类型，方便在一个列表里混排多种样式。
sealed interface ListItem {
    val id: String
}

// 分组标题。
data class SectionHeaderItem(
    override val id: String,
    val title: String
) : ListItem

// 顶部横幅/推荐位。
data class BannerItem(
    override val id: String,
    val title: String,
    val subtitle: String
) : ListItem

// 文章卡片。
data class ArticleItem(
    override val id: String,
    val title: String,
    val summary: String
) : ListItem

// 用户信息条目。
data class UserItem(
    override val id: String,
    val name: String,
    val role: String
) : ListItem

// 统计数据。
data class StatItem(
    override val id: String,
    val label: String,
    val value: String
) : ListItem

// 广告/推广条目。
data class AdItem(
    override val id: String,
    val text: String
) : ListItem

// 列表底部提示。
data class FooterItem(
    override val id: String,
    val hint: String
) : ListItem
