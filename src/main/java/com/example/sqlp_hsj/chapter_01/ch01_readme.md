

````markdown
# SQL 분석도구와 실행계획 읽기

SQL 튜닝을 할 때는 DBMS가 SQL을 어떤 방식으로 실행하는지 확인해야 한다.  
이때 사용하는 대표적인 도구가 실행계획이다.

실행계획은 SQL이 실제로 어떤 테이블을 읽고, 어떤 인덱스를 사용하며, 어떤 방식으로 조인하는지를 보여준다.

---

## 1. Oracle SQL 분석 도구

### EXPLAIN PLAN

`EXPLAIN PLAN`은 SQL을 실제로 실행하지 않고, 옵티마이저가 예상한 실행계획을 확인하는 방법이다.

```sql
EXPLAIN PLAN FOR
SELECT *
FROM emp
WHERE empno = 7369;

SELECT *
FROM TABLE(DBMS_XPLAN.DISPLAY);
````

주의할 점은 `EXPLAIN PLAN`은 실제 실행 결과가 아니라 예상 실행계획이라는 것이다.
실제로 실행했을 때의 계획과 다를 수 있다.

---

### DBMS_XPLAN.DISPLAY_CURSOR

`DBMS_XPLAN.DISPLAY_CURSOR`는 실제로 실행된 SQL 커서의 실행계획을 확인할 때 사용한다.

```sql
SELECT *
FROM emp
WHERE empno = 7369;

SELECT *
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR);
```

실제 실행된 SQL을 기준으로 실행계획을 보여주기 때문에 `EXPLAIN PLAN`보다 실무 분석에 더 유용한 경우가 많다.

---

### gather_plan_statistics 힌트

`gather_plan_statistics` 힌트는 SQL을 실행할 때 실제 실행 통계도 같이 수집하라는 명령어이다.

```sql
SELECT /*+ gather_plan_statistics */ *
FROM emp
WHERE deptno = 10;
```

이 힌트를 사용하면 SQL 실행 후 실제 처리 row 수 같은 정보를 확인할 수 있다.

단, `gather_plan_statistics`만 사용한다고 해서 자동으로 결과가 출력되는 것은 아니다.
SQL 실행 후 `DBMS_XPLAN.DISPLAY_CURSOR`와 함께 확인해야 한다.

```sql
SELECT *
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

`gather_plan_statistics + ALLSTATS LAST` 조합을 사용하면 예상 실행계획뿐 아니라 실제 실행 통계까지 확인할 수 있다.

---

## 2. 예상 Rows와 실제 Rows 확인

Oracle 기준으로 예상 row 수와 실제 row 수를 비교하려면 다음처럼 사용한다.

```sql
SELECT /*+ gather_plan_statistics */ *
FROM emp
WHERE deptno = 10;

SELECT *
FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

그러면 실행계획에서 보통 다음과 같은 컬럼을 볼 수 있다.

| 컬럼     | 의미                               |
| ------ | -------------------------------- |
| E-Rows | Estimated Rows. 옵티마이저가 예상한 row 수 |
| A-Rows | Actual Rows. 실제 실행 중 처리된 row 수   |

예상 row 수와 실제 row 수가 크게 다르면 튜닝 포인트가 될 수 있다.

예를 들어 옵티마이저는 10건이 나올 것으로 예상했는데 실제로는 10만 건이 나온다면, 잘못된 실행계획이 선택되었을 가능성이 있다.

이런 차이가 발생하는 대표적인 이유는 다음과 같다.

* 통계정보가 오래됨
* 데이터 분포가 한쪽으로 치우침
* 히스토그램이 없거나 부정확함
* 조건절 컬럼의 선택도를 옵티마이저가 잘못 예측함

---

## 3. gather_plan_statistics와 인덱스 선택

`gather_plan_statistics` 힌트는 실행 통계를 수집하는 힌트이다.
이 힌트 자체가 특정 인덱스를 강제로 사용하게 만드는 것은 아니다.

옵티마이저는 여전히 비용을 계산해서 가장 저렴하다고 판단한 실행계획을 선택한다.

예를 들어 특정 인덱스가 가장 비용이 낮다고 판단되면 해당 인덱스를 사용하는 실행계획이 출력된다.

```sql
SELECT /*+ gather_plan_statistics */ *
FROM emp
WHERE empno = 7369;
```

예상 실행계획 예시:

```text
SELECT STATEMENT
  TABLE ACCESS BY INDEX ROWID EMP
    INDEX UNIQUE SCAN PK_EMP
```

이 경우 `PK_EMP` 인덱스를 사용하는 것이 가장 효율적이라고 옵티마이저가 판단한 것이다.

---

## 4. Docker Oracle에서도 확인 가능한가?

가능하다.

Docker로 Oracle 이미지를 실행한 뒤 컨테이너에 접속해서 `sqlplus` 또는 `sqlcl`을 사용하면 실행계획을 확인할 수 있다.

Oracle에서는 옵티마이저 통계를 `DBMS_STATS` 패키지로 수집하고 관리할 수 있다.

```sql
BEGIN
  DBMS_STATS.GATHER_TABLE_STATS(
    ownname => USER,
    tabname => 'EMP'
  );
END;
/
```

통계정보 자체는 딕셔너리 뷰를 통해 조회할 수 있다.

### 테이블 통계

```sql
SELECT table_name,
       num_rows,
       blocks,
       avg_row_len,
       last_analyzed
FROM user_tables
WHERE table_name = 'T';
```

### 인덱스 통계

```sql
SELECT index_name,
       blevel,
       leaf_blocks,
       distinct_keys,
       clustering_factor,
       num_rows
FROM user_indexes
WHERE table_name = 'T';
```

### 컬럼 통계

```sql
SELECT column_name,
       num_distinct,
       density,
       num_nulls,
       histogram
FROM user_tab_col_statistics
WHERE table_name = 'T';
```

---

## 5. MySQL Docker 컨테이너에서도 가능한가?

가능하다.

MySQL Docker 컨테이너에서도 실행계획을 확인할 수 있다.

### EXPLAIN

```sql
EXPLAIN
SELECT *
FROM t
WHERE deptno = 10
  AND no = 1;
```

`EXPLAIN`은 MySQL에서 SQL의 예상 실행계획을 보여준다.

---

### EXPLAIN ANALYZE

MySQL 8.0.18 이상에서는 `EXPLAIN ANALYZE`를 사용할 수 있다.

```sql
EXPLAIN ANALYZE
SELECT *
FROM t
WHERE deptno = 10
  AND no = 1;
```

`EXPLAIN ANALYZE`는 쿼리를 실제로 실행하면서 각 단계의 처리 row 수와 시간 정보를 측정해 보여준다.

즉, Oracle의 `gather_plan_statistics + DBMS_XPLAN.DISPLAY_CURSOR(..., 'ALLSTATS LAST')`와 비슷하게 실제 실행 정보를 확인할 수 있다.

다만 Oracle의 `DBMS_XPLAN.DISPLAY_CURSOR`처럼 세밀한 실행계획 트리, Rows, Bytes, Cost, Predicate Information 등을 보는 방식은 Oracle 쪽이 더 풍부하다.

---

### MySQL 테이블/인덱스 통계 확인

MySQL에서 테이블과 인덱스 관련 정보는 다음처럼 확인할 수 있다.

```sql
SHOW INDEX FROM t;
```

```sql
SHOW TABLE STATUS LIKE 't';
```

```sql
SELECT *
FROM information_schema.statistics
WHERE table_name = 't';
```

통계를 갱신하려면 다음 명령어를 사용할 수 있다.

```sql
ANALYZE TABLE t;
```

---

## 6. 실행계획 컬럼 의미

Oracle 실행계획 예시:

```text
--------------------------------------------------------------------------------------
| Id  | Operation                   | Name   | Rows | Bytes | Cost (%CPU)| Time     |
--------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT            |        |    1 |    37 |     1   (0)| 00:00:01 |
|   1 |  TABLE ACCESS BY INDEX ROWID| EMP    |    1 |    37 |     1   (0)| 00:00:01 |
|*  2 |   INDEX UNIQUE SCAN         | PK_EMP |    1 |       |     0   (0)| 00:00:01 |
--------------------------------------------------------------------------------------
```

---

### Id

`Id`는 실행계획의 각 단계를 구분하는 번호이다.

중요한 점은 `Id` 순서대로 실행된다는 뜻이 아니라는 것이다.

실행 순서는 보통 들여쓰기 구조를 보고 판단한다.
아래쪽 자식 노드가 먼저 실행되고, 그 결과가 부모 노드로 올라간다.

위 실행계획은 다음 순서로 이해할 수 있다.

```text
2 INDEX UNIQUE SCAN
1 TABLE ACCESS BY INDEX ROWID
0 SELECT STATEMENT
```

즉, 먼저 인덱스를 읽고, 그 인덱스에서 얻은 ROWID로 테이블에 접근한 뒤, 최종 SELECT 결과를 반환한다.

---

### Operation

`Operation`은 해당 단계에서 수행하는 작업 종류이다.

쉽게 말하면 DBMS가 이 단계에서 무슨 일을 하는지를 보여준다.

예:

```text
INDEX UNIQUE SCAN
TABLE ACCESS BY INDEX ROWID
NESTED LOOPS
INDEX RANGE SCAN
```

---

### Name

`Name`은 작업 대상 오브젝트 이름이다.

`Operation`이 `TABLE ACCESS`라면 `Name`에는 테이블명이 나온다.
`Operation`이 `INDEX RANGE SCAN` 또는 `INDEX UNIQUE SCAN`이라면 `Name`에는 인덱스명이 나온다.

예를 들어 다음 실행계획을 보자.

```text
TABLE ACCESS BY INDEX ROWID EMP
  INDEX UNIQUE SCAN PK_EMP
```

이 의미는 다음과 같다.

* `PK_EMP`라는 인덱스를 `INDEX UNIQUE SCAN` 방식으로 읽었다.
* 인덱스에서 찾은 ROWID를 이용해 `EMP` 테이블에 접근했다.

Oracle에서는 Primary Key를 만들면 해당 제약 조건을 보장하기 위해 내부적으로 유니크 인덱스가 생성된다.
따라서 PK 조건으로 조회할 때 `INDEX UNIQUE SCAN`이 자주 나타난다.

---

### Rows

`Rows`는 옵티마이저가 예상한 결과 건수이다.

즉, 실제 결과 건수가 아니라 예상값이다.

예상 rows와 실제 rows는 다를 수 있다.
통계정보가 오래됐거나, 데이터 분포가 치우쳐 있거나, 히스토그램이 부정확하면 예상 rows와 실제 rows가 크게 달라질 수 있다.

튜닝할 때는 예상 rows와 실제 rows의 차이를 확인하는 것이 중요하다.

---

### Bytes

`Bytes`는 옵티마이저가 예상한 데이터량이다.

대략 다음과 같이 계산된다고 이해하면 된다.

```text
Bytes = Rows * 평균 Row 길이
```

예를 들어 예상 row 수가 많거나 row 하나의 크기가 크면 `Bytes` 값도 커진다.

---

### Cost

`Cost`는 옵티마이저가 계산한 예상 비용이다.

이 비용은 주로 다음 요소를 기반으로 계산된다.

* I/O 비용
* CPU 비용
* 예상 처리 row 수
* 인덱스 선택도
* 조인 방식
* 테이블 통계
* 인덱스 통계

예시:

```text
Cost (%CPU)
27 (4)
```

이 경우 의미는 다음과 같다.

* 전체 비용은 `27`
* 전체 비용 중 CPU 비용 비중은 `4%`

주의할 점은 Cost가 실행 시간을 직접 의미하지는 않는다는 것이다.
Cost가 낮다고 항상 빠른 것은 아니고, Cost가 높다고 항상 느린 것도 아니다.
다만 옵티마이저가 여러 실행계획 중 어떤 계획을 선택할지 비교할 때 사용하는 기준이다.

---

## 7. 주요 실행계획 Operation 의미

### SELECT STATEMENT

`SELECT STATEMENT`는 SQL 실행의 최상위 작업이다.

실제 데이터를 찾는 작업이라기보다는 이 SELECT 문 전체를 실행한다는 의미이다.

예시:

```sql
SELECT *
FROM emp
WHERE empno = 7369;
```

실행계획 예시:

```text
SELECT STATEMENT
  TABLE ACCESS BY INDEX ROWID EMP
    INDEX UNIQUE SCAN PK_EMP
```

여기서 `SELECT STATEMENT`는 전체 SELECT 쿼리를 의미한다.

---

### TABLE ACCESS BY INDEX ROWID

`TABLE ACCESS BY INDEX ROWID`는 인덱스를 통해 찾은 ROWID로 실제 테이블 데이터를 읽는 작업이다.

Oracle 인덱스에는 인덱스 컬럼 값과 함께 해당 행의 위치인 ROWID가 들어 있다.

예시:

```sql
SELECT ename, job
FROM emp
WHERE empno = 7369;
```

실행계획 예시:

```text
SELECT STATEMENT
  TABLE ACCESS BY INDEX ROWID EMP
    INDEX UNIQUE SCAN PK_EMP
```

처리 흐름은 다음과 같다.

1. `PK_EMP` 인덱스에서 `empno = 7369` 조건에 맞는 값을 찾는다.
2. 인덱스에서 해당 행의 ROWID를 얻는다.
3. ROWID를 이용해 `EMP` 테이블의 실제 행을 읽는다.
4. `ename`, `job` 컬럼 값을 반환한다.

즉, `TABLE ACCESS BY INDEX ROWID`는 인덱스만 보고 끝난 것이 아니라 실제 테이블까지 접근했다는 뜻이다.

조회하는 컬럼이 모두 인덱스에 들어 있다면 테이블 접근이 생략될 수도 있다.

---

### INDEX UNIQUE SCAN

`INDEX UNIQUE SCAN`은 유니크 인덱스 또는 Primary Key 인덱스를 이용해 최대 1건만 찾는 작업이다.

예시:

```sql
SELECT *
FROM emp
WHERE empno = 7369;
```

`empno`가 Primary Key라면 중복이 불가능하다.
따라서 Oracle은 이 조건으로 최대 한 건만 나온다는 것을 안다.

실행계획 예시:

```text
SELECT STATEMENT
  TABLE ACCESS BY INDEX ROWID EMP
    INDEX UNIQUE SCAN PK_EMP
```

처리 흐름은 다음과 같다.

1. `PK_EMP` 인덱스에서 `empno = 7369`를 찾는다.
2. 해당 인덱스 항목에서 ROWID를 얻는다.
3. ROWID로 `EMP` 테이블의 실제 행을 읽는다.

`INDEX UNIQUE SCAN`은 보통 다음 조건에서 나온다.

```sql
WHERE empno = 7369
```

단, 조건 컬럼이 Primary Key 또는 Unique Index에 포함되어 있어야 한다.

---

### INDEX RANGE SCAN

`INDEX RANGE SCAN`은 인덱스에서 여러 개의 가능성 있는 값을 범위로 찾는 작업이다.

예시:

```sql
SELECT *
FROM emp
WHERE deptno = 10;
```

`deptno`에 일반 인덱스가 있다면 실행계획은 다음처럼 나올 수 있다.

```text
SELECT STATEMENT
  TABLE ACCESS BY INDEX ROWID EMP
    INDEX RANGE SCAN IDX_EMP_DEPTNO
```

이 의미는 다음과 같다.

* `IDX_EMP_DEPTNO` 인덱스에서 `deptno = 10`인 구간을 찾는다.
* 조건에 맞는 row가 여러 건일 수 있다.
* 찾은 ROWID로 `EMP` 테이블에 접근한다.

다음 조건들도 `INDEX RANGE SCAN`이 나올 수 있다.

```sql
WHERE deptno = 10
WHERE sal > 3000
WHERE hiredate BETWEEN DATE '2024-01-01' AND DATE '2024-12-31'
WHERE ename LIKE 'K%'
```

`INDEX RANGE SCAN`은 보통 결과가 여러 건일 수 있는 조건에서 사용된다.

---

### NESTED LOOPS

`NESTED LOOPS`는 두 테이블을 조인할 때 사용하는 방식 중 하나이다.

개념은 다음과 같다.

```text
바깥 테이블에서 한 건씩 읽고,
그 값으로 안쪽 테이블을 반복해서 찾는다.
```

예시:

```sql
SELECT *
FROM emp e
JOIN dept d
  ON e.deptno = d.deptno
WHERE e.empno = 7369;
```

실행계획 예시:

```text
SELECT STATEMENT
  NESTED LOOPS
    TABLE ACCESS BY INDEX ROWID EMP
      INDEX UNIQUE SCAN PK_EMP
    TABLE ACCESS BY INDEX ROWID DEPT
      INDEX UNIQUE SCAN PK_DEPT
```

처리 흐름은 다음과 같다.

1. `EMP` 테이블에서 `empno = 7369`인 직원을 찾는다.
2. 해당 직원의 `deptno` 값을 가져온다.
3. 그 `deptno` 값으로 `DEPT` 테이블을 찾는다.
4. 두 결과를 조인한다.

`NESTED LOOPS`는 보통 다음 상황에서 효율적이다.

* 바깥쪽 테이블의 결과가 적을 때
* 안쪽 테이블을 인덱스로 빠르게 찾을 수 있을 때
* 조인 조건 컬럼에 인덱스가 있을 때

---

## 8. 실행계획 읽는 예시

다음 실행계획을 보자.

```text
SELECT STATEMENT
  NESTED LOOPS
    TABLE ACCESS BY INDEX ROWID EMP
      INDEX RANGE SCAN IDX_EMP_DEPTNO
    TABLE ACCESS BY INDEX ROWID DEPT
      INDEX UNIQUE SCAN PK_DEPT
```

예상 SQL:

```sql
SELECT *
FROM emp e
JOIN dept d
  ON e.deptno = d.deptno
WHERE e.deptno = 10;
```

읽는 흐름은 다음과 같다.

1. `IDX_EMP_DEPTNO` 인덱스에서 `deptno = 10`인 직원을 범위 검색한다.
2. 찾은 ROWID로 `EMP` 테이블의 실제 행을 읽는다.
3. 각 직원의 `deptno` 값을 가지고 `DEPT` 테이블을 찾는다.
4. `DEPT.deptno`는 Primary Key이므로 `PK_DEPT` 인덱스를 `INDEX UNIQUE SCAN` 방식으로 읽는다.
5. 찾은 ROWID로 `DEPT` 테이블의 실제 행을 읽는다.
6. 두 결과를 `NESTED LOOPS` 방식으로 조인한다.
7. 최종 결과를 `SELECT STATEMENT`가 반환한다.

---

## 9. 핵심 정리

| 항목                            | 의미                                |
| ----------------------------- | --------------------------------- |
| `EXPLAIN PLAN`                | SQL을 실제 실행하지 않고 예상 실행계획 확인        |
| `DBMS_XPLAN.DISPLAY_CURSOR`   | 실제 실행된 SQL 커서의 실행계획 확인            |
| `gather_plan_statistics`      | SQL 실행 시 실제 실행 통계 수집              |
| `ALLSTATS LAST`               | 마지막 실행의 실제 통계 확인                  |
| `E-Rows`                      | 옵티마이저가 예상한 row 수                  |
| `A-Rows`                      | 실제 실행 중 처리된 row 수                 |
| `SELECT STATEMENT`            | SELECT 쿼리 전체 실행 단위                |
| `NESTED LOOPS`                | 한쪽 결과를 기준으로 다른 테이블을 반복 조회하는 조인 방식 |
| `TABLE ACCESS BY INDEX ROWID` | 인덱스에서 얻은 ROWID로 실제 테이블 행 조회       |
| `INDEX RANGE SCAN`            | 인덱스에서 범위 또는 여러 후보 검색              |
| `INDEX UNIQUE SCAN`           | 유니크 인덱스로 최대 1건 검색                 |
| `Rows`                        | 옵티마이저가 예상한 결과 건수                  |
| `Bytes`                       | 예상 데이터량                           |
| `Cost`                        | 옵티마이저가 계산한 예상 비용                  |





---

## 10. 가장 중요한 포인트

실행계획을 볼 때는 단순히 인덱스를 탔는지만 보면 안 된다.

다음 항목을 함께 봐야 한다.

1. 어떤 인덱스를 사용했는가?
2. 예상 Rows와 실제 Rows 차이가 큰가?
3. 테이블 접근이 너무 많이 발생하지 않는가?
4. 조인 방식이 적절한가?
5. Cost가 왜 그렇게 계산되었는가?
6. 통계정보가 최신인가?

특히 튜닝에서는 `E-Rows`와 `A-Rows`의 차이를 보는 것이 중요하다.

예상과 실제가 크게 다르면 옵티마이저가 데이터를 잘못 이해하고 있을 가능성이 높고, 이 경우 통계정보 갱신이나 히스토그램 확인이 필요할 수 있다.

```
<<<<<<< HEAD
```
=======
```
>>>>>>> 36e1226 (프로필 분리& readme.md 업데이트)
