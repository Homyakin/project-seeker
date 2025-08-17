package ru.homyakin.seeker.game.random.item.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.ProbabilityPicker;

import java.util.HashMap;
import java.util.Map;

@Component
public class RaidLevelItemConfig {
    private static final Logger logger = LoggerFactory.getLogger(RaidLevelItemConfig.class);
    /**
     * Базовое распределение редкости предметов для рейдов (уровень 1-10)
     */
    private Map<ItemRarity, Integer> baseRarityDistribution = new HashMap<>();

    /**
     * Множители для улучшения редкости предметов с ростом уровня рейда
     */
    private Map<ItemRarity, Double> rarityImprovementMultipliers = new HashMap<>();

    /**
     * Уровни рейда, с которых начинается улучшение для каждой редкости
     */
    private Map<ItemRarity, Integer> rarityStartLevels = new HashMap<>();
    
    public RaidLevelItemConfig() {
        // Базовое распределение редкости предметов для рейдов (уровень 10)
        // Сначала доступны только COMMON и UNCOMMON вещи
        baseRarityDistribution.put(ItemRarity.COMMON, 60);
        baseRarityDistribution.put(ItemRarity.UNCOMMON, 40);
        baseRarityDistribution.put(ItemRarity.RARE, 0);
        baseRarityDistribution.put(ItemRarity.EPIC, 0);
        baseRarityDistribution.put(ItemRarity.LEGENDARY, 0);

        // Множители для улучшения редкости предметов с ростом уровня рейда
        // Вероятности улучшаются каждый уровень
        rarityImprovementMultipliers.put(ItemRarity.COMMON, 0.85);
        rarityImprovementMultipliers.put(ItemRarity.UNCOMMON, 0.95);
        rarityImprovementMultipliers.put(ItemRarity.RARE, 1.5);
        rarityImprovementMultipliers.put(ItemRarity.EPIC, 1.8);
        rarityImprovementMultipliers.put(ItemRarity.LEGENDARY, 2.0);

        // Уровни рейда, с которых начинается улучшение для каждой редкости
        rarityStartLevels.put(ItemRarity.COMMON, 10);      // Обычные начинают улучшаться с уровня 10
        rarityStartLevels.put(ItemRarity.UNCOMMON, 10);    // Необычные начинают улучшаться с уровня 10
        rarityStartLevels.put(ItemRarity.RARE, 10);        // Редкие начинают появляться с уровня 11 (начинают расти с 10)
        rarityStartLevels.put(ItemRarity.EPIC, 20);        // Эпические начинают появляться с уровня 21 (начинают расти с 20)
        rarityStartLevels.put(ItemRarity.LEGENDARY, 30);   // Легендарные начинают появляться с уровня 31 (начинают расти с 30)

        // Примерное распределение для разных уровней рейда:
        // 10: {COMMON=60.0, RARE=0.0, LEGENDARY=0.0, UNCOMMON=40.0, EPIC=0.0}
        // 20: {COMMON=3.7, RARE=22.2222, LEGENDARY=0.0, UNCOMMON=74.0740, EPIC=0.0}
        // 30: {COMMON=4.54, RARE=50.0, LEGENDARY=0.0, UNCOMMON=4.545, EPIC=40.909090}
        // 40: {COMMON=2.1739, RARE=34.782, LEGENDARY=23.913, UNCOMMON=2.1739, EPIC=36.95}
        // 50: {COMMON=1.449, RARE=30.434, LEGENDARY=30.434, UNCOMMON=1.449, EPIC=36.231}
    }
    
    /**
     * Генерирует распределение вероятностей редкости предметов для заданного уровня рейда.
     * 
     * <p><strong>Алгоритм работы:</strong></p>
     * <ol>
     *   <li><strong>Вычисление фактора улучшения</strong>: Для каждой редкости вычисляем
     *       {@code improvementFactor = max(0, raidLevel - startLevel)}, где {@code startLevel} - 
     *       уровень, с которого начинается улучшение для данной редкости</li>
     *   
     *   <li><strong>Относительное увеличение шансов</strong>: Применяем формулу относительного увеличения:
     *       <ul>
     *         <li><strong>Для предметов с базовым шансом 0</strong> (RARE/EPIC/LEGENDARY):
     *             {@code adjustedChance = 1 + improvementFactor * (multiplier - 1)}
     *             <br>Например, RARE (multiplier=1.5) на уровне 11: 1 + 1 * (1.5-1) = 1.5%</li>
     *         <li><strong>Для предметов с базовым шансом > 0</strong> (COMMON/UNCOMMON):
     *             {@code adjustedChance = baseChance * (1 + improvementFactor * (multiplier - 1))}
     *             <br>Например, COMMON (60%, multiplier=0.85) на уровне 11: 60 * (1 + 1 * (0.85-1)) = 51%</li>
     *       </ul>
     *   </li>
     *   
     *   <li><strong>Минимальные шансы</strong>: Предметы с базовым шансом 0 получают минимум 1% 
     *       только если {@code improvementFactor > 0} (т.е. уровень рейда достиг их startLevel)</li>
     * </ol>
     * 
     * @return ProbabilityPicker с распределением редкости предметов для данного уровня
     * @throws IllegalStateException если общий вес распределения неположительный
     */
    public ProbabilityPicker<ItemRarity> getRarityPickerForLevel(int raidLevel) {
        final var adjustedDistribution = new HashMap<ItemRarity, Integer>();
        
        for (final var entry : baseRarityDistribution.entrySet()) {
            final var rarity = entry.getKey();
            final var baseChance = entry.getValue();
            final var multiplier = rarityImprovementMultipliers.get(rarity);
            final var startLevel = rarityStartLevels.get(rarity);
            
            // Вычисляем фактор улучшения для конкретной редкости
            final var improvementFactor = Math.max(0, raidLevel - startLevel);
            
            int adjustedChance;
            if (improvementFactor > 0) {
                // Для предметов с базовым шансом 0, начинаем с минимального значения
                if (baseChance == 0) {
                    // Начинаем с 1% и увеличиваем относительно
                    adjustedChance = (int) Math.round(1 + improvementFactor * (multiplier - 1));
                } else {
                    // Для предметов с базовым шансом > 0, применяем относительное увеличение
                    adjustedChance = (int) Math.round(baseChance * (1 + improvementFactor * (multiplier - 1)));
                }
            } else {
                adjustedChance = baseChance;
            }
                
            // Минимум 1% шанс только если improvementFactor > 0, и максимум 0% если результат отрицательный
            adjustedDistribution.put(
                rarity,
                improvementFactor > 0
                    ? Math.max(1, Math.max(0, adjustedChance))
                    : Math.max(0, adjustedChance)
            );
        }
        
        // Проверяем, что общий вес положительный
        int totalWeight = adjustedDistribution.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            logger.error("Total weight of rarity distribution is zero or negative for raid level {}!", raidLevel);
            throw new IllegalStateException(
                "Total weight of rarity distribution is zero or negative for raid level " + raidLevel
            );
        }
        
        return new ProbabilityPicker<>(adjustedDistribution);
    }
    
    /**
     * Получить базовое распределение редкости предметов
     * @return базовое распределение
     */
    public Map<ItemRarity, Integer> getBaseRarityDistribution() {
        return new HashMap<>(baseRarityDistribution);
    }
    
    /**
     * Получить множители улучшения редкости
     * @return множители улучшения
     */
    public Map<ItemRarity, Double> getRarityImprovementMultipliers() {
        return new HashMap<>(rarityImprovementMultipliers);
    }
    
    /**
     * Получить уровни начала улучшения для каждой редкости
     * @return уровни начала улучшения
     */
    public Map<ItemRarity, Integer> getRarityStartLevels() {
        return new HashMap<>(rarityStartLevels);
    }
}
