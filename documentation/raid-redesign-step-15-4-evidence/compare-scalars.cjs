// Read-only arithmetic over the frozen step-15.3 independent mechanics summary.
// No Java processes, new battles, production writes, or access to target are used.
const fs = require('node:fs');
const input = process.argv[2]
  || '/tmp/seeker-step15-3-fillers.HXdOad/independent-review/summary.json';
const evidence = JSON.parse(fs.readFileSync(input, 'utf8'));
const mitigationWeights = [1.1, 0.9, 0.75, 1.25];
const result = {
  input,
  purpose: 'Analytic comparison only; no new balance evidence or accepted formula',
  confirmations: evidence.results.map(row => {
    const strong = row.crossTerms.strongSelf;
    const weak = row.crossTerms.weakSelf;
    const ratio = weak / strong;
    const selfProducts = row.base.npcs.map(npc => {
      const defense = npc.defenses.ARCANE;
      const meanMu = mitigationWeights.reduce((sum, weight) =>
        sum + 500 / (500 + defense * weight), 0) / 4;
      const criticalExpectation = npc.role === 'GUARDIAN' ? 1.02 : 1.056;
      return npc.health / meanMu * npc.attack * npc.speed / 1000 * criticalExpectation;
    });
    const [u, v] = selfProducts;
    return {
      style: row.style,
      level: row.level,
      strongOwnProduct: strong,
      weakOwnProduct: weak,
      weakToStrongHealth: row.weak.H / row.strong.H,
      weakToStrongDamage: row.weak.D / row.strong.D,
      N1to2TargetRatiosAtSameMAndParameters: {
        currentQ: row.append.playerQ / row.base.playerQ,
        sumOwnProducts: 1 + ratio,
        squaredSumSquareRoots: (1 + Math.sqrt(ratio)) ** 2,
        pairwiseMinimumB: 1 + 3 * ratio,
        rootSquaredMinimumR: Math.sqrt(1 + 3 * ratio * ratio),
        herfindahlEffectiveCount: (1 + ratio) ** 2 / (1 + ratio * ratio),
        meanOwnProducts: (1 + ratio) / 2,
      },
      N1to30StrongPlus29SameWeakPlayerRatios: {
        sumOwnProducts: 1 + 29 * ratio,
        pairwiseMinimumB: 1 + 899 * ratio,
        rootSquaredMinimumR: Math.sqrt(1 + 899 * ratio * ratio),
      },
      actualM2NpcOwnProducts: selfProducts,
      guardianToSlingerOwnProduct: u / v,
      conditionalUnroundedR4to3AtSameUnweightedNpcStats:
        (8 / 9) * Math.sqrt((u * u + 3 * v * v) / (u * u + 0.5 * v * v)),
    };
  }),
  counterexamples: {
    concentrationMultiply: {
      definition: 'S * n_eff = S^3 / sum(p_i^2)',
      before: { p: [3, 1, 1, 1, 1], value: 7 ** 3 / 13 },
      afterOneImprovement: { p: [4, 1, 1, 1, 1], value: 8 ** 3 / 20 },
    },
    concentrationDivide: {
      definition: 'S / n_eff = sum(p_i^2) / S',
      before: { p: [100, 1], value: 10001 / 101 },
      afterOneImprovement: { p: [100, 2], value: 10004 / 102 },
    },
    splitSpecialization: {
      equalTotals: { sumH: 200, sumD: 200, Q: 40000 },
      balanced: {
        H_D: [[100, 100], [100, 100]], P: 20000, B: 40000,
        rootSquaredMinimumR: 20000, squaredSumSquareRoots: 40000,
      },
      split: {
        H_D: [[199, 1], [1, 199]], P: 398, B: 796,
        rootSquaredMinimumR: 398, squaredSumSquareRoots: 796,
      },
      limitation: 'This is an information-loss counterexample, not a simulated victory claim.',
    },
  },
};
process.stdout.write(JSON.stringify(result, null, 2) + '\n');
