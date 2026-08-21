package galois.test;

import galois.llm.query.utils.cache.db.DBCache;
import galois.llm.query.utils.cache.db.DBCacheEntry;
import galois.llm.query.utils.cache.db.DaoCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TestCacheKeyMigration {
    private static final int BATCH_SIZE = 1000;

    @Test
    public void testHasKeysToMigrate() throws Exception {
        try (DaoCache dao = new DaoCache()) {
            dao.connect();

            // if the number of entries is already known
            assertEquals(23297, dao.countEntriesToRehash());
            // otherwise
            assertTrue(dao.countEntriesToRehash() > 0);
        }
    }

    @Test
    public void testMigrateCacheKeysToSha256() throws Exception {
        try (DaoCache dao = new DaoCache()) {
            dao.connect();

            long toMigrate = dao.countEntriesToRehash();
            log.info("Entries hashed with a legacy algorithm: {}", toMigrate);

            long migrated = 0;
            while (true) {
                List<DBCacheEntry> entries = dao.getEntriesToRehash(BATCH_SIZE);
                if (entries.isEmpty()) break;

                migrated += dao.callBatchTasks(() -> rehashEntries(dao, entries));
                log.info("Migrated {} of {} entries", migrated, toMigrate);
            }

            assertEquals(toMigrate, migrated);
            assertEquals(0, dao.countEntriesToRehash());
        }
    }

    // Recomputes the key of each entry from its own columns, hence no query result is needed to migrate the cache
    private int rehashEntries(DaoCache dao, List<DBCacheEntry> entries) throws SQLException {
        for (DBCacheEntry entry : entries) {
            // The key column is a CHAR(64), hence shorter keys are read back blank padded
            String legacyKey = entry.getCacheKey().trim();
            entry.setCacheKey(legacyKey);

            String cacheKey = DBCache.getCacheKey(entry);
            if (cacheKey.length() != DBCacheEntry.CACHE_KEY_LENGTH) {
                throw new IllegalStateException("Not a SHA-256 cache key: " + cacheKey);
            }

            // The entry may have been cached again after the switch to SHA-256: the legacy duplicate is dropped
            if (dao.containsEntry(cacheKey)) {
                log.warn("Entry {} is already cached with the key {}: dropping the legacy entry", legacyKey, cacheKey);
                checkUpdatedRows(dao.deleteEntry(legacyKey), legacyKey);
                continue;
            }

            checkUpdatedRows(dao.updateEntryKey(entry, cacheKey), legacyKey);
        }
        return entries.size();
    }

    private void checkUpdatedRows(int updatedRows, String cacheKey) {
        if (updatedRows != 1) {
            throw new IllegalStateException(String.format("Updated %d rows for the entry %s, expected 1", updatedRows, cacheKey));
        }
    }
}
