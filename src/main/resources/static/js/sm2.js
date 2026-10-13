// Mirrors com.placementprep.core.Sm2 so the revision buttons can preview the next interval.
export function previewInterval(problem, quality) {
  if (quality < 3) return 1;
  const repetitions = problem.repetitions + 1;
  if (repetitions === 1) return 1;
  if (repetitions === 2) return 6;
  const miss = 5 - quality;
  const ease = Math.max(130, Math.round(problem.ease * 100) + 10 - miss * (8 + miss * 2));
  return Math.min(365, Math.floor((Math.max(1, problem.intervalDays) * ease + 50) / 100));
}
