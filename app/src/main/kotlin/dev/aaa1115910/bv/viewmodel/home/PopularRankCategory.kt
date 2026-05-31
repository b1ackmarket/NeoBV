package dev.aaa1115910.bv.viewmodel.home

enum class PopularFeedSource {
    Popular,
    Rank
}

enum class PopularRankCategory(
    val displayName: String,
    val slug: String?,
    val rid: Int?,
    val feedSource: PopularFeedSource
) {
    All("全部", null, null, PopularFeedSource.Popular),
    Douga("动画", "douga", 1, PopularFeedSource.Rank),
    Game("游戏", "game", 4, PopularFeedSource.Rank),
    Kichiku("鬼畜", "kichiku", 119, PopularFeedSource.Rank),
    Music("音乐", "music", 3, PopularFeedSource.Rank),
    Dance("舞蹈", "dance", 129, PopularFeedSource.Rank),
    Cinephile("影视", "cinephile", 181, PopularFeedSource.Rank),
    Ent("娱乐", "ent", 5, PopularFeedSource.Rank),
    Knowledge("知识", "knowledge", 36, PopularFeedSource.Rank),
    Tech("科技数码", "tech", 188, PopularFeedSource.Rank),
    Food("美食", "food", 211, PopularFeedSource.Rank),
    Car("汽车", "car", 223, PopularFeedSource.Rank),
    Fashion("时尚美妆", "fashion", 155, PopularFeedSource.Rank),
    Sports("体育运动", "sports", 234, PopularFeedSource.Rank),
    Animal("动物", "animal", 217, PopularFeedSource.Rank)
}
