package io.github.dalpner.mctd.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Every tower needs a block state, models and names in both languages. */
class AssetsJUnitTest {

    private static String read(String path) throws IOException {
        try (InputStream in = AssetsJUnitTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyTowerHasAssets() throws IOException {
        String de = read("/assets/mctd/lang/de_de.json");
        String en = read("/assets/mctd/lang/en_us.json");
        for (TowerType type : Towers.all()) {
            String block = type.id() + "_tower";
            read("/assets/mctd/blockstates/" + block + ".json");
            read("/assets/mctd/models/item/" + block + ".json");
            for (int tier = 1; tier <= 3; tier++) {
                read("/assets/mctd/models/block/" + block + "_t" + tier + ".json");
            }
            for (String key : new String[]{type.translationKey(), type.descriptionKey(), "block.mctd." + block}) {
                assertTrue(de.contains("\"" + key + "\""), "de_de misses " + key);
                assertTrue(en.contains("\"" + key + "\""), "en_us misses " + key);
            }
        }
        for (Element e : Element.values()) {
            assertTrue(de.contains("\"" + e.translationKey() + "\""), "de_de misses " + e.translationKey());
        }
        read("/data/mctd/dimension/arena.json");
        read("/data/mctd/dimension_type/arena.json");
        read("/fabric.mod.json");
    }
}
