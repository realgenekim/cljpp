# Quick test (5 min, 3 API calls)
bb bin/test-variant.clj neutral 3 1
bb bin/test-variant.clj persuasive 3 1  
bb bin/test-variant.clj negative 3 1

# Full experiment (2-3 hours, 60 API calls)
bb bin/test-variant.clj neutral all 1
bb bin/test-variant.clj persuasive all 1
bb bin/test-variant.clj negative all 1
