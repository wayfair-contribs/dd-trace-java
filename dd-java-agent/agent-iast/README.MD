# agent-iast

## Benchmark results

### String

#### concat()

| Benchmark                           | Mode | Cnt   | Score   | Error   | Units |
|-------------------------------------|------|-------|---------|---------|-------|
| StringConcatBenchmark.baseline      | ss   | 15000 | 107.220 | ± 3.072 | ns/op |
| StringConcatBenchmark.iastDisabled  | ss   | 15000 | 100.008 | ± 2.545 | ns/op |
| StringConcatBenchmark.notTainted    | ss   | 15000 | 194.672 | ± 5.714 | ns/op |
| StringConcatBenchmark.stringTainted | ss   | 15000 | 248.205 | ± 8.071 | ns/op |
| StringConcatBenchmark.paramTainted  | ss   | 15000 | 300.324 | ± 8.003 | ns/op |
| StringConcatBenchmark.bothTainted   | ss   | 15000 | 266.331 | ± 9.206 | ns/op |

### String Builder

#### constructor()

| Benchmark                               | Mode | Cnt   | Score   | Error   | Units |
|-----------------------------------------|------|-------|---------|---------|-------|
| StringBuilderInitBenchmark.baseline     | ss   | 15000 |  91.949 | ± 6.394 | ns/op |
| StringBuilderInitBenchmark.iastDisabled | ss   | 15000 |  86.585 | ± 2.889 | ns/op |
| StringBuilderInitBenchmark.notTainted   | ss   | 15000 | 159.242 | ± 3.923 | ns/op |
| StringBuilderInitBenchmark.tainted      | ss   | 15000 | 189.089 | ± 7.212 | ns/op |

#### append()

| Benchmark                                         | Mode | Cnt   | Score   | Error   | Units |
|---------------------------------------------------|------|-------|---------|---------|-------|
| StringBuilderAppendBenchmark.baseline             | ss   | 15000 |  96.849 | ± 5.309 | ns/op |
| StringBuilderAppendBenchmark.iastDisabled         | ss   | 15000 | 104.661 | ± 2.966 | ns/op |
| StringBuilderAppendBenchmark.notTainted           | ss   | 15000 | 185.388 | ± 5.512 | ns/op |
| StringBuilderAppendBenchmark.stringBuilderTainted | ss   | 15000 | 178.127 | ± 4.511 | ns/op |
| StringBuilderAppendBenchmark.paramTainted         | ss   | 15000 | 245.535 | ± 7.519 | ns/op |
| StringBuilderAppendBenchmark.bothTainted          | ss   | 15000 | 193.124 | ± 7.772 | ns/op |

#### toString()

| Benchmark                                   | Mode | Cnt   | Score   | Error   | Units |
|---------------------------------------------|------|-------|---------|---------|-------|
| StringBuilderToStringBenchmark.baseline     | ss   | 15000 |  69.559 | ± 4.164 | ns/op |
| StringBuilderToStringBenchmark.iastDisabled | ss   | 15000 |  61.980 | ± 2.737 | ns/op |
| StringBuilderToStringBenchmark.notTainted   | ss   | 15000 | 146.003 | ± 6.805 | ns/op |
| StringBuilderToStringBenchmark.tainted      | ss   | 15000 | 171.153 | ± 5.297 | ns/op |
