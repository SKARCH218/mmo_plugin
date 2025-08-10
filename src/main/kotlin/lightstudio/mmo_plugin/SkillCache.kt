package lightstudio.mmo_plugin

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SkillCache(private val plugin: LightMmo) {

    val cache: Cache<Pair<String, SkillType>, PlayerSkillData> = CacheBuilder.newBuilder()
        .maximumSize(1000) // 최대 1000명의 플레이어 데이터 캐싱
        .expireAfterAccess(10, TimeUnit.MINUTES) // 10분 동안 접근 없으면 만료
        .build()

    fun getPlayerSkillData(uuid: UUID, skillType: SkillType): PlayerSkillData {
        val key = Pair(uuid.toString(), skillType)
        // 1. Try to get from cache
        var skillData = cache.getIfPresent(key)

        if (skillData == null) {
            // 2. If not in cache, load synchronously from DB
            skillData = plugin.databaseManager.loadPlayerSkillDataSync(uuid.toString(), skillType)
            if (skillData != null) {
                // 3. If loaded from DB, put it into the cache
                cache.put(key, skillData)
            } else {
                // 4. If not in DB either, create default data and cache it
                skillData = PlayerSkillData(1, 0)
                cache.put(key, skillData)
            }
        }
        return skillData
    }

    fun get(uuid: String, skillType: SkillType): CompletableFuture<PlayerSkillData> {
        val key = Pair(uuid, skillType)
        val cachedData = cache.getIfPresent(key)

        if (cachedData != null) {
            return CompletableFuture.completedFuture(cachedData)
        } else {
            // Cache miss, load asynchronously from DB
            return plugin.databaseManager.execute { conn ->
                var level = 1
                var exp = 0

                val query = "SELECT skill_${skillType.name.lowercase()}_level, skill_${skillType.name.lowercase()}_exp FROM player_skills WHERE uuid = ?"
                conn.prepareStatement(query).use { statement ->
                    statement.setString(1, uuid)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            level = resultSet.getInt("skill_${skillType.name.lowercase()}_level")
                            exp = resultSet.getInt("skill_${skillType.name.lowercase()}_exp")
                        }
                    }
                }
                PlayerSkillData(level, exp)
            }.thenApply { loadedData ->
                cache.put(key, loadedData)
                loadedData
            }
        }
    }

    fun put(uuid: String, skillType: SkillType, level: Int, exp: Int) {
        cache.put(Pair(uuid, skillType), PlayerSkillData(level, exp))
    }

    fun invalidate(uuid: String, skillType: SkillType) {
        cache.invalidate(Pair(uuid, skillType))
    }

    fun invalidateAll() {
        cache.invalidateAll()
    }
}