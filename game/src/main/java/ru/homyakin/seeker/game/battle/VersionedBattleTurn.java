package ru.homyakin.seeker.game.battle;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.BattleEvent.ThreatReason;
import ru.homyakin.seeker.game.battle.effect.PeriodicDamageEffect;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingCooldowns;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingSkillMath;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillDamageCalculator;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.AttackType;

/** Executes the common ordered turn used whenever a battle contains an explicitly scalable skill. */
final class VersionedBattleTurn {
    private static final int BLEEDING_CHANCE = 6_000;
    private static final int BLEEDING_TICKS = 4;
    private static final int BLEEDING_COOLDOWN = 4;
    private static final int SELF_HEAL_COOLDOWN = 2;
    private static final int TEMPO_BREAK_V1_COOLDOWN = 2;
    private static final int TEMPO_BREAK_V2_COOLDOWN = 1;
    private static final int PENETRATION_FOLLOW_UPS = 2;

    private VersionedBattleTurn() {
    }

    static boolean process(
        BattlePersonage self,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        self.beginVersionedMove();
        final long turnId = log.nextTurnId();
        final int ownTurn = self.battlePersonageStats().turnsCount();
        final int turnStartLine = self.currentPosition();
        log.addTrace(new BattleTraceEvent.TurnStarted(
            turnId,
            self.id(),
            ownTurn,
            turnStartLine,
            round
        ));
        if (!self.isAlive()) {
            traceTurnFinished(log, self, turnId, ownTurn, turnStartLine, false, round);
            return false;
        }
        self.expireLegacyRangeBonusesForVersionedTurn();
        processPeriodicDamage(self, context, log, round);
        if (!self.isAlive()) {
            traceTurnFinished(log, self, turnId, ownTurn, turnStartLine, false, round);
            return false;
        }

        final var coolingAtStart = self.scalingSkills().coolingAtTurnStart();
        final boolean tempoImmuneAtStart = self.scalingSkills().tempoBreakImmune();
        activateBerserk(self, log, round);
        log.addAll(self.applyLegacyTurnStartSkill(ActiveEnum.BERSERK, context, round));
        log.addAll(self.applyLegacyTurnStartSkill(ActiveEnum.HIT_AND_RUN, context, round));
        if (!self.isAlive()) {
            finishTurn(self, coolingAtStart, tempoImmuneAtStart);
            traceTurnFinished(log, self, turnId, ownTurn, turnStartLine, true, round);
            return false;
        }

        final var enemyTeam = context.enemyAliveTeam(self);
        final var selection = selectInitialTarget(self, enemyTeam, context);
        if (selection == null) {
            context.moveTowardEnemy(self);
            log.add(new BattleEvent.MovedTowardEnemy(self.id(), self.currentPosition(), round));
            finishTurn(self, coolingAtStart, tempoImmuneAtStart);
            traceTurnFinished(log, self, turnId, ownTurn, turnStartLine, true, round);
            return enemyTeam.values().stream().noneMatch(BattlePersonage::isAlive);
        }

        final long attemptId = log.nextAttemptId();
        final var interception = interceptWithGuard(
            selection.target(),
            selection.candidates(),
            context
        );
        final var finalTarget = interception.finalTarget();
        final int distance = self.distanceTo(finalTarget);
        log.add(new BattleEvent.TargetSelected(
            self.id(),
            selection.target().id(),
            finalTarget.id(),
            distance,
            round
        ));
        if (interception.interceptor() != null) {
            log.add(new BattleEvent.AttackIntercepted(
                self.id(),
                selection.target().id(),
                interception.interceptor().id(),
                round
            ));
            log.add(new BattleEvent.SkillWindowUsed(
                interception.interceptor().id(),
                ActiveEnum.GUARD,
                round
            ));
        }

        final boolean penetrationOpened = consumePenetrationIfApplicable(
            self,
            selection.penetrationAttempt(),
            log,
            round
        );
        final boolean penetrationFollowUp = selection.penetrationAttempt() == PenetrationAttempt.FOLLOW_UP;
        if (penetrationFollowUp) {
            self.scalingSkills().consumePenetrationFollowUp();
        }
        final boolean penetrationAccess = penetrationOpened || penetrationFollowUp;
        final boolean hitAndRunUsed = (!penetrationAccess
            || !usesV2(self, ActiveEnum.PENETRATION)) && consumeHitAndRunIfApplicable(
                self,
                selection.target(),
                finalTarget,
                selection.ordinaryRange(),
                log,
                round
            );
        final var access = penetrationAccess
            ? AttackAccess.PENETRATION
            : hitAndRunUsed ? AttackAccess.HIT_AND_RUN : AttackAccess.NORMAL;
        final var savedAttack = self.attackForVersionedAttempt(distance, access);
        final var primaryAttackType = self.primaryAttackType();
        final int accumulationChargesBefore = self.scalingSkills().accumulationCharges();
        final boolean dischargeMarked = self.scalingSkills().has(ActiveEnum.ACCUMULATION)
            && accumulationChargesBefore == 3
            && primaryAttackType.filter(type -> savedAttack.getOrDefault(type, 0) > 0).isPresent();
        final Map<AttackType, Integer> dischargeBasis = dischargeMarked
            ? Map.of(primaryAttackType.orElseThrow(), savedAttack.get(primaryAttackType.orElseThrow()))
            : Map.of();
        final int dischargeCoefficientNumerator = dischargeMarked
            ? multiplier(self, ActiveEnum.ACCUMULATION)
            : 0;
        final int dischargeCoefficientDenominator = dischargeMarked
            ? accumulationDenominator(self)
            : 1;

        final boolean critical = self.versionedCriticalSucceeds(context.random());
        final var rolledAttack = critical ? self.applyCriticalMultiplier(savedAttack) : savedAttack;
        final boolean dodged = finalTarget.versionedDodgeSucceeds(context.random(), self);
        final AttemptOutcome attemptOutcome;
        if (dodged) {
            finalTarget.recordVersionedDodge(self, rolledAttack, log, round);
            attemptOutcome = applyDodgeBranch(
                self,
                finalTarget,
                distance,
                savedAttack,
                context,
                log,
                round
            );
        } else {
            attemptOutcome = applyHitBranch(
                self,
                finalTarget,
                distance,
                savedAttack,
                rolledAttack,
                primaryAttackType.orElse(null),
                critical,
                penetrationOpened && usesV2(self, ActiveEnum.PENETRATION),
                dischargeMarked,
                context,
                log,
                round
            );
        }

        updatePenetrationTarget(
            self,
            selection,
            finalTarget,
            penetrationOpened,
            dodged
        );

        updateAccumulation(self, savedAttack, primaryAttackType.orElse(null), dischargeMarked, log, round);
        final int accumulationChargesAfter = self.scalingSkills().accumulationCharges();
        updateAttemptThreat(
            self,
            finalTarget,
            !dodged,
            attemptOutcome.killRewardsThreat(),
            log,
            round
        );
        final int lineBeforeRetreat = self.currentPosition();
        if (hitAndRunUsed && self.isAlive()) {
            moveBackward(self, self, ActiveEnum.HIT_AND_RUN, context, log, round);
        }
        final int lineAfterRetreat = self.currentPosition();
        log.addTrace(new BattleTraceEvent.NormalAttackAttempt(
            turnId,
            attemptId,
            self.id(),
            ownTurn,
            selection.target().id(),
            finalTarget.id(),
            access,
            selection.ordinaryRange(),
            distance,
            savedAttack,
            critical,
            dodged,
            attemptOutcome.normalDamage(),
            !finalTarget.isAlive(),
            dischargeMarked,
            dischargeBasis,
            dischargeCoefficientNumerator,
            dischargeCoefficientDenominator,
            attemptOutcome.dischargeDamage(),
            accumulationChargesBefore,
            accumulationChargesAfter,
            lineBeforeRetreat,
            lineAfterRetreat,
            round
        ));
        if (self.isAlive()) {
            applySelfHeal(self, context, log, round);
            log.addAll(self.applyLegacyTurnEndSkill(ActiveEnum.SELF_HEAL, context, round));
        }
        finishTurn(self, coolingAtStart, tempoImmuneAtStart);
        traceTurnFinished(log, self, turnId, ownTurn, turnStartLine, true, round);
        return false;
    }

    private static TargetSelection selectInitialTarget(
        BattlePersonage self,
        Map<java.util.UUID, BattlePersonage> enemyTeam,
        BattleContext context
    ) {
        final int ordinaryRange = self.range();
        final boolean penetrationV2 = usesV2(self, ActiveEnum.PENETRATION);
        final var lockedTargetId = self.scalingSkills().penetrationTargetId();
        if (penetrationV2 && lockedTargetId.isPresent()) {
            final var lockedTarget = enemyTeam.get(lockedTargetId.orElseThrow());
            if (lockedTarget != null && lockedTarget.isAlive()) {
                final var accessibleCandidates = enemyTeam.values().stream()
                    .filter(BattlePersonage::isAlive)
                    .filter(candidate -> candidate == lockedTarget
                        || self.distanceTo(candidate) <= ordinaryRange)
                    .toList();
                return new TargetSelection(
                    lockedTarget,
                    accessibleCandidates,
                    ordinaryRange,
                    PenetrationAttempt.FOLLOW_UP
                );
            }
            self.scalingSkills().clearPenetrationTarget();
        }
        final boolean hitAndRunReady = self.scalingSkills().ready(ActiveEnum.HIT_AND_RUN);
        final boolean penetrationReady = self.scalingSkills().ready(ActiveEnum.PENETRATION);
        final int extendedRange = Math.addExact(ordinaryRange, hitAndRunReady ? 1 : 0);
        final var ordinaryCandidates = new ArrayList<BattlePersonage>();
        final var penetrationCandidates = new ArrayList<BattlePersonage>();
        final var accessibleCandidates = new ArrayList<BattlePersonage>();
        for (final var candidate : enemyTeam.values()) {
            if (!candidate.isAlive()) {
                continue;
            }
            final int distance = self.distanceTo(candidate);
            final boolean inOrdinaryRange = distance <= ordinaryRange;
            final boolean inExtendedRange = distance <= extendedRange;
            if (inExtendedRange || penetrationReady) {
                accessibleCandidates.add(candidate);
            }
            if (penetrationV2 && !inOrdinaryRange && penetrationReady) {
                penetrationCandidates.add(candidate);
            } else if (inExtendedRange) {
                ordinaryCandidates.add(candidate);
            } else if (penetrationReady) {
                penetrationCandidates.add(candidate);
            }
        }
        final boolean penetrationExpanded = !penetrationCandidates.isEmpty();
        final List<BattlePersonage> targetingCandidates;
        if (penetrationV2 && penetrationExpanded) {
            targetingCandidates = penetrationCandidates;
        } else {
            targetingCandidates = accessibleCandidates;
        }
        if (targetingCandidates.isEmpty()) {
            return null;
        }
        final var target = context.random().pickWeighted(
            "target-selection:" + self.id(),
            targetingWeights(self, targetingCandidates)
        );
        return new TargetSelection(
            target,
            List.copyOf(accessibleCandidates),
            ordinaryRange,
            penetrationExpanded ? PenetrationAttempt.OPENING : PenetrationAttempt.NONE
        );
    }

    private static Map<BattlePersonage, Integer> targetingWeights(
        BattlePersonage attacker,
        List<BattlePersonage> candidates
    ) {
        if (attacker.targetingTactic() != TargetingTactic.INITIATIVE_INTERCEPTION
            || !attacker.scalingSkills().ready(ActiveEnum.TEMPO_BREAK)) {
            return attacker.targetingWeights(candidates);
        }
        var referenceThreat = 1;
        for (final var candidate : candidates) {
            referenceThreat = Math.max(referenceThreat, candidate.totalThreat());
        }
        final var result = new java.util.LinkedHashMap<BattlePersonage, Integer>();
        for (final var candidate : candidates) {
            final int baseThreat = Math.max(0, candidate.totalThreat());
            var weight = NonNegativeRational.of(baseThreat);
            if (tempoBreakTargetIsValid(candidate)) {
                final long speed = candidate.initiative();
                final long denominator = Math.multiplyExact(8L, speed);
                final long timeNumerator = CombatRules.INITIATIVE_THRESHOLD - candidate.initiativeGauge();
                final long scoreNumerator = Math.max(0, Math.min(denominator, denominator - timeNumerator));
                weight = weight.add(
                    NonNegativeRational.of(Math.multiplyExact(2L, referenceThreat))
                        .multiply(NonNegativeRational.of(scoreNumerator, denominator))
                );
            }
            result.put(candidate, Math.max(1, ScalingSkillMath.roundHalfUpToInt(weight)));
        }
        return result;
    }

    private static GuardInterception interceptWithGuard(
        BattlePersonage originalTarget,
        List<BattlePersonage> accessibleCandidates,
        BattleContext context
    ) {
        final var teamState = context.teamSkillState(originalTarget);
        if (!teamState.guardReadyForAttempt()) {
            return GuardInterception.none(originalTarget);
        }
        if (originalTarget.scalingSkills().has(ActiveEnum.GUARD)) {
            return GuardInterception.none(originalTarget);
        }
        BattlePersonage interceptor = null;
        for (final var candidate : context.allyAliveTeam(originalTarget).values()) {
            if (candidate == originalTarget
                || !candidate.isAlive()
                || !candidate.scalingSkills().has(ActiveEnum.GUARD)
                || !accessibleCandidates.contains(candidate)
                || !guardCanIntercept(candidate, originalTarget)) {
                continue;
            }
            if (interceptor == null
                || candidate.totalThreat() > interceptor.totalThreat()
                || candidate.totalThreat() == interceptor.totalThreat()
                    && candidate.id().compareTo(interceptor.id()) < 0) {
                interceptor = candidate;
            }
        }
        if (interceptor == null) {
            return GuardInterception.none(originalTarget);
        }
        final var schedule = ScalingCooldowns.guardOrPenetration(
            interceptor.scalingSkills().version(ActiveEnum.GUARD),
            interceptor.scalingSkills().points(ActiveEnum.GUARD)
        ).orElseThrow();
        teamState.consumeGuard(schedule.firstCooldown(), schedule.secondCooldown());
        return new GuardInterception(interceptor, interceptor);
    }

    private static boolean consumePenetrationIfApplicable(
        BattlePersonage self,
        PenetrationAttempt penetrationAttempt,
        BattleActionLog log,
        int round
    ) {
        if (penetrationAttempt != PenetrationAttempt.OPENING
            || !self.scalingSkills().ready(ActiveEnum.PENETRATION)) {
            return false;
        }
        final var schedule = ScalingCooldowns.guardOrPenetration(
            self.scalingSkills().version(ActiveEnum.PENETRATION),
            self.scalingSkills().points(ActiveEnum.PENETRATION)
        ).orElseThrow();
        self.scalingSkills().startAlternatingCooldown(
            ActiveEnum.PENETRATION,
            schedule.firstCooldown(),
            schedule.secondCooldown()
        );
        log.add(new BattleEvent.SkillWindowUsed(self.id(), ActiveEnum.PENETRATION, round));
        return true;
    }

    private static void updatePenetrationTarget(
        BattlePersonage self,
        TargetSelection selection,
        BattlePersonage finalTarget,
        boolean penetrationOpened,
        boolean dodged
    ) {
        if (penetrationOpened
            && usesV2(self, ActiveEnum.PENETRATION)
            && !dodged
            && finalTarget.isAlive()) {
            self.scalingSkills().lockPenetrationTarget(finalTarget.id(), PENETRATION_FOLLOW_UPS);
            return;
        }
        if (selection.penetrationAttempt() == PenetrationAttempt.FOLLOW_UP
            && !selection.target().isAlive()) {
            self.scalingSkills().clearPenetrationTarget();
        }
    }

    private static boolean consumeHitAndRunIfApplicable(
        BattlePersonage self,
        BattlePersonage originalTarget,
        BattlePersonage finalTarget,
        int ordinaryRange,
        BattleActionLog log,
        int round
    ) {
        if (!self.scalingSkills().ready(ActiveEnum.HIT_AND_RUN)
            || self.distanceTo(originalTarget) > ordinaryRange + 1
            || self.distanceTo(finalTarget) > ordinaryRange + 1) {
            return false;
        }
        final var schedule = ScalingCooldowns.hitAndRun(
            self.scalingSkills().version(ActiveEnum.HIT_AND_RUN),
            self.scalingSkills().points(ActiveEnum.HIT_AND_RUN)
        ).orElseThrow();
        self.scalingSkills().startAlternatingCooldown(
            ActiveEnum.HIT_AND_RUN,
            schedule.firstCooldown(),
            schedule.secondCooldown()
        );
        log.add(new BattleEvent.SkillWindowUsed(self.id(), ActiveEnum.HIT_AND_RUN, round));
        return true;
    }

    private static AttemptOutcome applyDodgeBranch(
        BattlePersonage attacker,
        BattlePersonage target,
        int distance,
        Map<AttackType, Integer> savedAttack,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        applyFeint(target, attacker, distance, context, log, round);
        applyLegacySkill(ActiveEnum.FEINT, target, attacker, context, log, round);
        if (!attacker.isAlive() || !target.isAlive()) {
            return AttemptOutcome.NONE;
        }
        final boolean aliveBeforePreciseStrike = target.isAlive();
        applyVectorSkill(
            attacker,
            target,
            ActiveEnum.PRECISE_STRIKE,
            savedAttack,
            3 * multiplier(attacker, ActiveEnum.PRECISE_STRIKE),
            100,
            5_000,
            context,
            log,
            round
        );
        applyLegacySkill(ActiveEnum.PRECISE_STRIKE, attacker, target, context, log, round);
        return new AttemptOutcome(0, 0, aliveBeforePreciseStrike && !target.isAlive());
    }

    private static AttemptOutcome applyHitBranch(
        BattlePersonage attacker,
        BattlePersonage target,
        int distance,
        Map<AttackType, Integer> savedAttack,
        Map<AttackType, Integer> rolledAttack,
        AttackType primaryAttackType,
        boolean critical,
        boolean penetrationUsed,
        boolean dischargeMarked,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        final int normalDamage = target.applyVersionedNormalDamage(
            attacker,
            rolledAttack,
            critical,
            context.random(),
            log,
            round
        );
        logThreatLoss(target, attacker, null, normalDamage, log, round);
        if (!target.isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(target.id(), attacker.id(), round));
            return new AttemptOutcome(normalDamage, 0, true);
        }

        applyCounterAttack(target, attacker, distance, context, log, round);
        applyLegacySkill(ActiveEnum.COUNTER_ATTACK, target, attacker, context, log, round);
        if (attacker.isAlive()) {
            applyThorns(target, attacker, distance, context, log, round);
            applyLegacySkill(ActiveEnum.THORNS, target, attacker, context, log, round);
        }
        if (critical && target.isAlive()) {
            applyRetreat(target, attacker, context, log, round);
            applyLegacySkill(ActiveEnum.RETREAT, target, attacker, context, log, round);
        }
        if (!attacker.isAlive() || !target.isAlive()) {
            return new AttemptOutcome(normalDamage, 0, false);
        }

        if (penetrationUsed) {
            applyVectorSkill(
                attacker,
                target,
                ActiveEnum.PENETRATION,
                savedAttack,
                multiplier(attacker, ActiveEnum.PENETRATION),
                12,
                10_000,
                context,
                log,
                round
            );
        }
        if (!target.isAlive()) {
            return new AttemptOutcome(normalDamage, 0, false);
        }

        final boolean aliveBeforeDoubleAttack = target.isAlive();
        applyVectorSkill(
            attacker,
            target,
            ActiveEnum.DOUBLE_ATTACK,
            savedAttack,
            multiplier(attacker, ActiveEnum.DOUBLE_ATTACK),
            usesV2(attacker, ActiveEnum.DOUBLE_ATTACK) ? 100 : 50,
            5_000,
            context,
            log,
            round
        );
        applyLegacySkill(ActiveEnum.DOUBLE_ATTACK, attacker, target, context, log, round);
        if (!target.isAlive()) {
            return new AttemptOutcome(normalDamage, 0, aliveBeforeDoubleAttack);
        }
        applyBleeding(attacker, target, savedAttack, primaryAttackType, context, log, round);
        applyLegacySkill(ActiveEnum.BLEEDING, attacker, target, context, log, round);
        if (!target.isAlive()) {
            return new AttemptOutcome(normalDamage, 0, false);
        }
        var dischargeDamage = 0;
        if (dischargeMarked && primaryAttackType != null) {
            dischargeDamage = applyVectorSkill(
                attacker,
                target,
                ActiveEnum.ACCUMULATION,
                Map.of(primaryAttackType, savedAttack.get(primaryAttackType)),
                multiplier(attacker, ActiveEnum.ACCUMULATION),
                accumulationDenominator(attacker),
                10_000,
                context,
                log,
                round
            );
        }
        if (!target.isAlive()) {
            return new AttemptOutcome(normalDamage, dischargeDamage, true);
        }
        applyTempoBreak(attacker, target, context, log, round);
        if (critical && target.isAlive()) {
            applyKnockback(attacker, target, context, log, round);
            applyLegacySkill(ActiveEnum.KNOCKBACK, attacker, target, context, log, round);
        }
        return new AttemptOutcome(normalDamage, dischargeDamage, false);
    }

    private static void applyCounterAttack(
        BattlePersonage owner,
        BattlePersonage attacker,
        int distance,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!owner.scalingSkills().has(ActiveEnum.COUNTER_ATTACK)
            || distance > owner.ordinaryMaxRange()
            || !attacker.isAlive()) {
            return;
        }
        applyVectorSkill(
            owner,
            attacker,
            ActiveEnum.COUNTER_ATTACK,
            owner.scalingAttackAt(distance, AttackAccess.NORMAL),
            3 * multiplier(owner, ActiveEnum.COUNTER_ATTACK),
            200,
            5_000,
            context,
            log,
            round
        );
    }

    private static void applyThorns(
        BattlePersonage owner,
        BattlePersonage attacker,
        int distance,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!owner.scalingSkills().has(ActiveEnum.THORNS) || distance != 1 || !attacker.isAlive()) {
            return;
        }
        final int closeAttack = sum(owner.scalingAttackAt(1, AttackAccess.NORMAL));
        final int basis = Math.min(
            Math.multiplyExact(3, owner.maxHealth()),
            Math.multiplyExact(40, closeAttack)
        );
        if (basis <= 0) {
            return;
        }
        applyVectorSkill(
            owner,
            attacker,
            ActiveEnum.THORNS,
            Map.of(AttackType.PIERCE, basis),
            multiplier(owner, ActiveEnum.THORNS),
            4_800,
            10_000,
            context,
            log,
            round
        );
    }

    private static void applyFeint(
        BattlePersonage owner,
        BattlePersonage attacker,
        int distance,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!owner.scalingSkills().has(ActiveEnum.FEINT)
            || distance > owner.ordinaryMaxRange()
            || !attacker.isAlive()) {
            return;
        }
        applyVectorSkill(
            owner,
            attacker,
            ActiveEnum.FEINT,
            owner.scalingAttackAt(distance, AttackAccess.NORMAL),
            multiplier(owner, ActiveEnum.FEINT),
            40,
            8_000,
            context,
            log,
            round
        );
    }

    private static int applyVectorSkill(
        BattlePersonage source,
        BattlePersonage target,
        ActiveEnum skill,
        Map<AttackType, Integer> basis,
        int coefficientNumerator,
        int coefficientDenominator,
        int chanceBasisPoints,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!source.scalingSkills().has(skill)
            || !target.isAlive()
            || basis.isEmpty()
            || sum(basis) <= 0) {
            return 0;
        }
        if (!context.random().chance(
            "skill-chance:" + skill.name() + ":" + source.id() + ":" + target.id(),
            chanceBasisPoints
        )) {
            return 0;
        }
        return applyScalingSkillDamage(
            source,
            target,
            skill,
            basis,
            coefficientNumerator,
            coefficientDenominator,
            false,
            context,
            log,
            round
        );
    }

    private static int applyScalingSkillDamage(
        BattlePersonage source,
        BattlePersonage target,
        ActiveEnum skill,
        Map<AttackType, Integer> basis,
        int coefficientNumerator,
        int coefficientDenominator,
        boolean periodic,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!target.isAlive() || coefficientNumerator <= 0 || basis.isEmpty()) {
            return 0;
        }
        final var rawDamage = new EnumMap<AttackType, NonNegativeRational>(AttackType.class);
        for (final var entry : basis.entrySet()) {
            if (entry.getValue() > 0) {
                rawDamage.put(
                    entry.getKey(),
                    NonNegativeRational.of(entry.getValue())
                        .multiply(NonNegativeRational.of(coefficientNumerator, coefficientDenominator))
                );
            }
        }
        if (rawDamage.isEmpty()) {
            return 0;
        }
        final var result = SkillDamageCalculator.calculate(
            rawDamage,
            target.defenses(),
            (minimum, maximum) -> context.random().nextInt(
                "skill-damage-" + skill.name()
                    + ":" + (periodic ? "periodic" : "direct")
                    + ":" + source.id()
                    + ":" + target.id(),
                minimum,
                maximum
            )
        );
        var exactRaw = NonNegativeRational.ZERO;
        for (final var component : rawDamage.values()) {
            exactRaw = exactRaw.add(component);
        }
        final int rawForStatistics = exactRaw.floor().min(BigInteger.valueOf(Integer.MAX_VALUE)).intValueExact();
        final boolean wasAlive = target.isAlive();
        final int actualDamage = target.applyVersionedSkillDamage(
            rawForStatistics,
            result.damage(),
            source
        );
        log.add(new BattleEvent.ScalingSkillDamage(
            target.id(),
            source.id(),
            skill,
            basis,
            coefficientNumerator,
            coefficientDenominator,
            periodic,
            actualDamage,
            target.health(),
            round
        ));
        logThreatLoss(target, source, skill, actualDamage, log, round);
        if (wasAlive && !target.isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(target.id(), source.id(), round));
        }
        return actualDamage;
    }

    private static void applyBleeding(
        BattlePersonage source,
        BattlePersonage target,
        Map<AttackType, Integer> savedAttack,
        AttackType primaryAttackType,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!source.scalingSkills().ready(ActiveEnum.BLEEDING)
            || primaryAttackType == null
            || savedAttack.getOrDefault(primaryAttackType, 0) <= 0
            || !context.random().chance(
                "skill-chance:BLEEDING:" + source.id() + ":" + target.id(),
                BLEEDING_CHANCE
            )) {
            return;
        }
        final int coefficient = multiplier(source, ActiveEnum.BLEEDING);
        target.addOrReplaceScalingBleeding(new ScalingBleedingEffect(
            source,
            Map.of(primaryAttackType, savedAttack.get(primaryAttackType)),
            coefficient,
            80,
            BLEEDING_TICKS
        ));
        source.scalingSkills().startCooldown(ActiveEnum.BLEEDING, BLEEDING_COOLDOWN);
        log.add(new BattleEvent.SkillWindowUsed(source.id(), ActiveEnum.BLEEDING, round));
    }

    private static void processPeriodicDamage(
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        final var effects = new ArrayList<PeriodicEffect>();
        for (final var effect : target.scalingBleedings()) {
            effects.add(PeriodicEffect.scaling(effect));
        }
        for (final var effect : target.legacyPeriodicDamages()) {
            effects.add(PeriodicEffect.legacy(effect));
        }
        effects.sort(
            Comparator.comparing(PeriodicEffect::skill)
                .thenComparing(PeriodicEffect::sourceId)
                .thenComparing(PeriodicEffect::scaling)
        );

        for (final var entry : effects) {
            if (!target.isAlive()) {
                break;
            }
            if (entry.scaling()) {
                final var effect = entry.scalingEffect();
                applyScalingSkillDamage(
                    effect.source(),
                    target,
                    ActiveEnum.BLEEDING,
                    effect.basis(),
                    effect.coefficientNumerator(),
                    effect.coefficientDenominator(),
                    true,
                    context,
                    log,
                    round
                );
                if (effect.consumeTick()) {
                    target.removeScalingBleeding(effect.source().id());
                }
            } else {
                final var effect = entry.legacyEffect();
                final boolean wasAlive = target.isAlive();
                final int healthBefore = target.health();
                final boolean exhausted = target.withoutAutomaticThreatLoss(
                    () -> effect.tickOnOwnTurnBeginWithoutDefeatEvent(target, log, round)
                );
                logThreatLoss(
                    target,
                    effect.sourceId(),
                    effect.skill(),
                    healthBefore - target.health(),
                    log,
                    round
                );
                if (wasAlive && !target.isAlive()) {
                    log.add(new BattleEvent.PersonageDefeated(target.id(), effect.sourceId(), round));
                }
                if (exhausted) {
                    target.removeLegacyPeriodicDamage(effect);
                }
            }
        }
    }

    private static void applySelfHeal(
        BattlePersonage self,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!self.scalingSkills().ready(ActiveEnum.SELF_HEAL) || self.health() >= self.maxHealth()) {
            return;
        }
        final int amount = Math.max(1, ScalingSkillMath.roundHalfUpToInt(
            NonNegativeRational.of(self.maxHealth())
                .multiply(NonNegativeRational.of(multiplier(self, ActiveEnum.SELF_HEAL), 400))
        ));
        if (!context.random().chance(
            "skill-chance:SELF_HEAL:" + self.id(),
            5_000
        )) {
            return;
        }
        final int healed = self.healAndGetActual(amount);
        if (healed <= 0) {
            return;
        }
        self.scalingSkills().startCooldown(ActiveEnum.SELF_HEAL, SELF_HEAL_COOLDOWN);
        log.add(new BattleEvent.SkillWindowUsed(self.id(), ActiveEnum.SELF_HEAL, round));
        log.add(new BattleEvent.PersonageHealed(
            self.id(),
            ActiveEnum.SELF_HEAL,
            healed,
            self.health(),
            round
        ));
    }

    private static void activateBerserk(BattlePersonage self, BattleActionLog log, int round) {
        if (!self.scalingSkills().has(ActiveEnum.BERSERK)
            || self.scalingSkills().berserkActivated()
            || self.primaryAttackType().isEmpty()
            || !self.exactBerserkThresholdReached()) {
            return;
        }
        self.scalingSkills().activateBerserk();
        log.add(new BattleEvent.SkillWindowUsed(self.id(), ActiveEnum.BERSERK, round));
    }

    private static void applyRetreat(
        BattlePersonage owner,
        BattlePersonage attacker,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!owner.scalingSkills().has(ActiveEnum.RETREAT)
            || !context.random().chance(
                "skill-chance:RETREAT:" + owner.id() + ":" + attacker.id(),
                Math.multiplyExact(multiplier(owner, ActiveEnum.RETREAT), 250)
            )) {
            return;
        }
        moveBackward(owner, attacker, ActiveEnum.RETREAT, context, log, round);
    }

    private static void applyKnockback(
        BattlePersonage source,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!source.scalingSkills().has(ActiveEnum.KNOCKBACK)
            || !context.random().chance(
                "skill-chance:KNOCKBACK:" + source.id() + ":" + target.id(),
                Math.multiplyExact(multiplier(source, ActiveEnum.KNOCKBACK), 250)
            )) {
            return;
        }
        moveBackward(target, source, ActiveEnum.KNOCKBACK, context, log, round);
    }

    private static void moveBackward(
        BattlePersonage personage,
        BattlePersonage source,
        ActiveEnum skill,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        final int before = personage.currentPosition();
        context.moveBackward(personage, 1);
        if (personage.currentPosition() != before) {
            log.add(new BattleEvent.PersonageForcedMove(
                personage.id(),
                source.id(),
                skill,
                personage.currentPosition(),
                round
            ));
        }
    }

    private static void applyTempoBreak(
        BattlePersonage source,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!source.scalingSkills().ready(ActiveEnum.TEMPO_BREAK) || !tempoBreakTargetIsValid(target)) {
            return;
        }
        final int requested = ScalingSkillMath.roundHalfUpToInt(
            NonNegativeRational.of(multiplier(source, ActiveEnum.TEMPO_BREAK))
                .multiply(NonNegativeRational.of(
                    source.impactStrength(),
                    usesV2(source, ActiveEnum.TEMPO_BREAK) ? 5 : 20
                ))
        );
        final int removed = target.reduceInitiativeGauge(requested);
        if (removed <= 0) {
            return;
        }
        source.scalingSkills().startCooldown(
            ActiveEnum.TEMPO_BREAK,
            usesV2(source, ActiveEnum.TEMPO_BREAK)
                ? TEMPO_BREAK_V2_COOLDOWN
                : TEMPO_BREAK_V1_COOLDOWN
        );
        target.scalingSkills().setTempoBreakImmune(true);
        log.add(new BattleEvent.SkillWindowUsed(source.id(), ActiveEnum.TEMPO_BREAK, round));
        log.add(new BattleEvent.InitiativeDelayed(
            target.id(),
            source.id(),
            ActiveEnum.TEMPO_BREAK,
            removed,
            target.initiativeGauge(),
            round
        ));
    }

    private static boolean tempoBreakTargetIsValid(BattlePersonage target) {
        return !target.readyToAct()
            && !target.scalingSkills().tempoBreakImmune()
            && target.initiativeGauge() > 0
            && target.initiative() > 0;
    }

    private static void updateAccumulation(
        BattlePersonage self,
        Map<AttackType, Integer> savedAttack,
        AttackType primaryAttackType,
        boolean dischargeMarked,
        BattleActionLog log,
        int round
    ) {
        if (!self.scalingSkills().has(ActiveEnum.ACCUMULATION)) {
            return;
        }
        if (dischargeMarked) {
            self.scalingSkills().clearAccumulationCharges();
            log.add(new BattleEvent.SkillChargeChanged(
                self.id(),
                ActiveEnum.ACCUMULATION,
                0,
                true,
                round
            ));
        } else if (primaryAttackType != null && savedAttack.getOrDefault(primaryAttackType, 0) > 0) {
            self.scalingSkills().addAccumulationCharge();
            log.add(new BattleEvent.SkillChargeChanged(
                self.id(),
                ActiveEnum.ACCUMULATION,
                self.scalingSkills().accumulationCharges(),
                false,
                round
            ));
        }
    }

    private static void updateAttemptThreat(
        BattlePersonage attacker,
        BattlePersonage finalTarget,
        boolean normalHit,
        boolean killRewardsThreat,
        BattleActionLog log,
        int round
    ) {
        final int delta;
        final ThreatReason reason;
        if (!finalTarget.isAlive() && killRewardsThreat) {
            delta = attacker.gainThreatAfterKill();
            reason = ThreatReason.KILL;
        } else if (finalTarget.isAlive() && normalHit) {
            delta = attacker.gainThreatAfterHit();
            reason = ThreatReason.NORMAL_HIT;
        } else {
            return;
        }
        log.add(new BattleEvent.ThreatChanged(
            attacker.id(),
            attacker.id(),
            null,
            delta,
            attacker.totalThreat(),
            reason,
            round
        ));
    }

    private static void logThreatLoss(
        BattlePersonage target,
        BattlePersonage source,
        ActiveEnum skill,
        int damage,
        BattleActionLog log,
        int round
    ) {
        logThreatLoss(target, source.id(), skill, damage, log, round);
    }

    private static void logThreatLoss(
        BattlePersonage target,
        UUID sourceId,
        ActiveEnum skill,
        int damage,
        BattleActionLog log,
        int round
    ) {
        if (damage <= 0) {
            return;
        }
        final int delta = target.loseThreatAfterDamage(damage);
        if (delta != 0) {
            log.add(new BattleEvent.ThreatChanged(
                target.id(),
                sourceId,
                skill,
                delta,
                target.totalThreat(),
                ThreatReason.DAMAGE_TAKEN,
                round
            ));
        }
    }

    private static void applyLegacySkill(
        ActiveEnum skill,
        BattlePersonage source,
        BattlePersonage target,
        BattleContext context,
        BattleActionLog log,
        int round
    ) {
        if (!source.hasLegacySkill(skill)
            || !target.isAlive() && skill != ActiveEnum.RETREAT) {
            return;
        }
        final int healthBefore = target.health();
        final boolean aliveBefore = target.isAlive();
        log.addAll(target.withoutAutomaticThreatLoss(
            () -> source.applyLegacyDamageSkill(skill, context, target, round)
        ));
        logThreatLoss(target, source, skill, healthBefore - target.health(), log, round);
        if (aliveBefore && !target.isAlive()) {
            log.add(new BattleEvent.PersonageDefeated(target.id(), source.id(), round));
        }
    }

    private static int multiplier(BattlePersonage personage, ActiveEnum skill) {
        return ScalingSkillMath.multiplierNumerator(personage.scalingSkills().points(skill));
    }

    private static int accumulationDenominator(BattlePersonage personage) {
        return usesV2(personage, ActiveEnum.ACCUMULATION) ? 28 : 25;
    }

    private static boolean guardCanIntercept(BattlePersonage guard, BattlePersonage target) {
        if (usesV2(guard, ActiveEnum.GUARD)) {
            return target.currentPosition()
                == guard.currentPosition() - guard.advanceDirection().indexDelta();
        }
        return Math.abs(guard.currentPosition() - target.currentPosition()) <= 1;
    }

    private static boolean usesV2(BattlePersonage personage, ActiveEnum skill) {
        return personage.scalingSkills().has(skill)
            && personage.scalingSkills().version(skill) == SkillFormulaVersion.SCALING_SKILLS_V2;
    }

    private static int sum(Map<AttackType, Integer> values) {
        var result = 0;
        for (final var value : values.values()) {
            result = Math.addExact(result, value);
        }
        return result;
    }

    private static void traceTurnFinished(
        BattleActionLog log,
        BattlePersonage self,
        long turnId,
        int ownTurn,
        int startLineIndex,
        boolean actionPerformed,
        int round
    ) {
        log.addTrace(new BattleTraceEvent.TurnFinished(
            turnId,
            self.id(),
            ownTurn,
            startLineIndex,
            self.currentPosition(),
            actionPerformed,
            self.isAlive(),
            round
        ));
    }

    private static void finishTurn(
        BattlePersonage self,
        Set<ActiveEnum> coolingAtStart,
        boolean tempoImmuneAtStart
    ) {
        self.scalingSkills().finishTurn(coolingAtStart);
        if (tempoImmuneAtStart) {
            self.scalingSkills().setTempoBreakImmune(false);
        }
    }

    private record AttemptOutcome(int normalDamage, int dischargeDamage, boolean killRewardsThreat) {
        private static final AttemptOutcome NONE = new AttemptOutcome(0, 0, false);
    }

    private record TargetSelection(
        BattlePersonage target,
        List<BattlePersonage> candidates,
        int ordinaryRange,
        PenetrationAttempt penetrationAttempt
    ) {
    }

    private enum PenetrationAttempt {
        NONE,
        OPENING,
        FOLLOW_UP,
    }

    private record GuardInterception(
        BattlePersonage finalTarget,
        BattlePersonage interceptor
    ) {
        private static GuardInterception none(BattlePersonage target) {
            return new GuardInterception(target, null);
        }
    }

    private record PeriodicEffect(
        ActiveEnum skill,
        UUID sourceId,
        boolean scaling,
        ScalingBleedingEffect scalingEffect,
        PeriodicDamageEffect legacyEffect
    ) {
        private static PeriodicEffect scaling(ScalingBleedingEffect effect) {
            return new PeriodicEffect(
                ActiveEnum.BLEEDING,
                effect.source().id(),
                true,
                effect,
                null
            );
        }

        private static PeriodicEffect legacy(PeriodicDamageEffect effect) {
            return new PeriodicEffect(
                effect.skill(),
                effect.sourceId(),
                false,
                null,
                effect
            );
        }
    }
}
