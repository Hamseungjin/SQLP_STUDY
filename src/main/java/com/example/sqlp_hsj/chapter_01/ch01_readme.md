- README에는 각 실습의 목적, 실행 방법, 예상 실행계획, 결과 해석 방법을 포함해줘.

1단계: ExplainPlanService 작성

ExplainPlanService를 만들어 다음 기능을 제공해줘.

기능:
- EXPLAIN 쿼리 실행
- EXPLAIN FORMAT=JSON 실행
- 가능하면 EXPLAIN ANALYZE 실행
- SQL과 파라미터를 받아 실행계획 출력
- SELECT 쿼리를 실제 실행해서 수행 시간과 반환 row 수 측정
- 콘솔 로그를 보기 좋게 출력

주의:
- PreparedStatement 파라미터가 있는 SQL도 실행계획을 확인할 수 있게 구현해줘.
- EXPLAIN ANALYZE가 실패하면 일반 EXPLAIN과 FORMAT=JSON 결과만 출력해줘.
- 결과 비교가 쉽도록 실습 이름과 SQL 이름을 함께 출력해줘.

2단계: Index Range Scan 실습

IndexScanExperimentRunner에 다음 실습을 작성해줘.

실습 1: Index Range Scan이 가능한 쿼리

쿼리:
SELECT *
FROM orders
WHERE order_date >= ?
AND order_date < ?

요구사항:
- order_date 인덱스 idx_orders_order_date를 사용하도록 한다.
- EXPLAIN 결과에서 type이 range인지 확인한다.
- key가 idx_orders_order_date인지 확인한다.
- rows 추정치와 실제 반환 row 수를 비교한다.
- README에 Index Range Scan의 의미를 설명한다.
- README에 B*Tree에서 수직적 탐색으로 시작점을 찾고, 수평적 탐색으로 범위를 읽는다는 점을 설명한다.

3단계: 인덱스 컬럼 가공으로 Range Scan이 어려워지는 쿼리

IndexScanExperimentRunner에 다음 실습을 추가해줘.

실습 2: 인덱스 컬럼 가공 쿼리

쿼리 예시:
SELECT *
FROM orders
WHERE DATE(order_date) = ?

또는:
SELECT *
FROM orders
WHERE YEAR(order_date) = ?

요구사항:
- order_date에 인덱스가 있어도 컬럼을 함수로 가공하면 일반적인 Range Scan이 어려워질 수 있음을 보여준다.
- EXPLAIN 결과에서 type, key, rows, Extra를 출력한다.
- 실습 1의 Range Scan 쿼리와 rows 추정치, 실행 시간, key 사용 여부를 비교한다.
- README에 “인덱스 컬럼을 가공하면 인덱스 키 값의 정렬 순서를 그대로 활용하기 어렵다”는 점을 설명한다.

4단계: 같은 의미지만 인덱스를 사용할 수 있도록 개선한 쿼리

IndexScanExperimentRunner에 다음 실습을 추가해줘.

나쁜 쿼리:
SELECT *
FROM orders
WHERE DATE(order_date) = '2025-01-01'

개선 쿼리:
SELECT *
FROM orders
WHERE order_date >= '2025-01-01 00:00:00'
AND order_date < '2025-01-02 00:00:00'

요구사항:
- 두 쿼리의 EXPLAIN 결과를 비교한다.
- 두 쿼리의 EXPLAIN ANALYZE 결과를 비교한다.
- 수행 시간과 반환 row 수를 비교한다.
- README에 같은 의미라도 컬럼을 가공하지 않고 범위 조건으로 바꾸면 인덱스를 사용할 수 있음을 설명한다.

5단계: 결합 인덱스(customer_id, status) 사용 실습

CompositeIndexExperimentRunner를 작성해줘.

인덱스:
idx_orders_customer_status(customer_id, status)

비교 쿼리:

A. customer_id와 status를 모두 조건에 넣은 경우
SELECT *
FROM orders
WHERE customer_id = ?
AND status = ?

B. status만 조건에 넣은 경우
SELECT *
FROM orders
WHERE status = ?

C. customer_id만 조건에 넣은 경우
SELECT *
FROM orders
WHERE customer_id = ?

요구사항:
- 각 쿼리에 대해 EXPLAIN, EXPLAIN FORMAT=JSON, 가능하면 EXPLAIN ANALYZE를 출력한다.
- type, key, rows, Extra를 비교한다.
- 결합 인덱스의 선두 컬럼 원리를 README에 설명한다.
- customer_id 없이 status만 조건으로 사용할 때 idx_orders_customer_status를 효율적으로 쓰기 어려울 수 있음을 설명한다.
- 단, 별도 idx_orders_status(status)가 있으면 옵티마이저가 그 인덱스를 선택할 수 있으므로 README에 이 점을 설명한다.

주의:
- MySQL은 Oracle의 Index Skip Scan과 동작 방식이 다르다.
- MySQL 8 기준으로 Skip Scan이 항상 명확히 관찰되는 것은 아니므로 README에는 “Oracle 실습 시나리오와 MySQL 실습 시나리오의 차이”를 반드시 설명해줘.
- Oracle에서는 선두 컬럼이 없어도 Index Skip Scan이 나타날 수 있지만, MySQL에서는 실행계획 표현과 동작이 다르며 항상 관찰 가능하지 않다는 점을 설명해줘.

6단계: Index Full Scan 실습

CompositeIndexExperimentRunner에 Index Full Scan 실습을 추가해줘.

목표:
결합 인덱스 idx_orders_customer_status(customer_id, status)가 있는 상태에서 선두 컬럼 customer_id 없이 status만 조건으로 조회할 때 실행계획을 관찰한다.

쿼리:
SELECT customer_id, status
FROM orders
WHERE status = ?

요구사항:
- EXPLAIN 결과에서 type, key, rows, Extra를 확인한다.
- idx_orders_status(status)가 있는 경우와 없는 경우의 차이를 설명한다.
- 필요하다면 README에 “정확한 Index Full Scan을 관찰하려면 idx_orders_status를 임시로 제거하거나 optimizer hint를 사용할 수 있다”는 주의사항을 적어줘.
- Index Range Scan, Index Full Scan, Table Full Scan의 차이를 README에 설명한다.
- MySQL에서는 실행계획 용어가 Oracle과 다르게 표현될 수 있음을 설명한다.

7단계: Index Unique Scan 또는 Primary Key Lookup 실습

IndexScanExperimentRunner 또는 별도 Runner에 Primary Key Lookup 실습을 추가해줘.

쿼리:
SELECT *
FROM orders
WHERE order_id = ?

요구사항:
- order_id PK로 단건 조회한다.
- EXPLAIN 결과에서 type이 const 또는 eq_ref/ref 계열로 나타날 수 있음을 확인한다.
- MySQL의 Primary Key Lookup이 Oracle의 Index Unique Scan과 개념적으로 대응된다는 점을 README에 설명한다.
- 단건 조회에서 rows 추정치가 1에 가까운지 확인한다.
- 수행 시간과 결과 row 수를 출력한다.

8단계: Covered Index / Covered Query 실습

CoveringIndexExperimentRunner를 작성해줘.

비교 쿼리:

A. 테이블 랜덤 액세스가 발생할 수 있는 쿼리
SELECT *
FROM orders
WHERE customer_id = ?
AND status = ?

B. 인덱스만으로 처리 가능한 커버링 쿼리
SELECT customer_id, status
FROM orders
WHERE customer_id = ?
AND status = ?

요구사항:
- 두 쿼리의 EXPLAIN 결과를 비교한다.
- Extra에서 Using index가 나타나는지 확인한다.
- SELECT *는 인덱스에 없는 컬럼을 읽기 위해 테이블 접근이 필요할 수 있음을 설명한다.
- 커버링 쿼리는 인덱스만 읽고 결과를 만들 수 있어 테이블 랜덤 액세스를 줄일 수 있음을 README에 설명한다.
- 실제 수행 시간과 반환 row 수를 비교한다.

9단계: 커버링 인덱스 추가 실습

CoveringIndexExperimentRunner에 다음 실습을 추가해줘.

기존 인덱스:
idx_orders_customer_status(customer_id, status)

추가 인덱스:
idx_orders_customer_status_date_amount(customer_id, status, order_date, amount)

비교 쿼리:
SELECT order_date, amount
FROM orders
WHERE customer_id = ?
AND status = ?

요구사항:
- 기존 인덱스만 있을 때와 추가 커버링 인덱스가 있을 때 실행계획을 비교한다.
- key 선택, rows, Extra의 Using index 여부를 비교한다.
- 인덱스에 필요한 컬럼을 추가하면 테이블 랜덤 액세스를 줄일 수 있음을 설명한다.
- 단, 인덱스가 많아지면 INSERT/UPDATE/DELETE 성능과 저장공간에 비용이 생긴다는 점을 README에 설명한다.
- 실습용 DDL은 index-practice-schema.sql에 포함한다.

10단계: 선행 컬럼 범위조건 실습

CompositeIndexExperimentRunner에 다음 실습을 추가해줘.

비교 인덱스:
A. idx_orders_customer_status_date(customer_id, status, order_date)
B. idx_orders_customer_date_status(customer_id, order_date, status)

비교 쿼리:
SELECT *
FROM orders
WHERE customer_id = ?
AND status = ?
AND order_date >= ?
AND order_date < ?

요구사항:
- 두 인덱스 중 어떤 인덱스가 더 효율적인지 EXPLAIN ANALYZE로 비교한다.
- 가능하면 FORCE INDEX를 사용해서 두 인덱스를 각각 강제로 사용한 결과를 비교한다.
- rows 추정치, actual rows, actual time을 비교한다.
- README에 다음 내용을 설명한다.
    - 결합 인덱스에서 앞쪽 컬럼이 = 조건이면 뒤쪽 컬럼 활용에 유리하다.
    - 중간에 범위조건이 오면 그 뒤쪽 컬럼은 인덱스 탐색 조건으로 충분히 활용하기 어려울 수 있다.
    - 따라서 조건 형태와 컬럼 순서가 인덱스 스캔 효율에 영향을 준다.

11단계: IN-List 전환 실습

CompositeIndexExperimentRunner에 IN-List 실습을 추가해줘.

비교 쿼리 예시:

A. status를 범위성 조건처럼 사용하는 쿼리
SELECT *
FROM orders
WHERE customer_id = ?
AND status >= ?
AND status <= ?

B. status IN 조건을 사용하는 쿼리
SELECT *
FROM orders
WHERE customer_id = ?
AND status IN (?, ?)

요구사항:
- 두 쿼리의 EXPLAIN, EXPLAIN FORMAT=JSON, EXPLAIN ANALYZE 결과를 비교한다.
- MySQL에서는 range/ref 접근으로 표현될 수 있음을 설명한다.
- Oracle에서는 INLIST ITERATOR가 나타날 수 있음을 README에 설명한다.
- MySQL과 Oracle의 실행계획 표현 차이를 설명한다.
- 필요한 경우 optimizer trace를 통해 MySQL 옵티마이저 판단을 확인하는 방법도 README에 참고로 적어줘.

12단계: 함수기반 인덱스 대체 실습

FunctionalIndexExperimentRunner를 작성해줘.

목표:
DATE(order_date) = ? 쿼리는 일반 order_date 인덱스를 제대로 활용하기 어렵다는 점을 보여주고,
MySQL 8의 generated column 또는 functional index를 이용해 개선하는 방법을 실습한다.

나쁜 쿼리:
SELECT *
FROM orders
WHERE DATE(order_date) = ?

개선 방법 1: Generated Column 사용
ALTER TABLE orders
ADD COLUMN order_ymd DATE GENERATED ALWAYS AS (DATE(order_date)) STORED;

CREATE INDEX idx_orders_order_ymd ON orders(order_ymd);

개선 쿼리:
SELECT *
FROM orders
WHERE order_ymd = ?

요구사항:
- 개선 전/후 EXPLAIN 결과를 비교한다.
- 개선 전/후 실행 시간을 비교한다.
- README에 MySQL 8에서 generated column이나 functional index로 함수 기반 조회를 개선할 수 있음을 설명한다.
- 단, 가장 기본적인 개선 방법은 컬럼을 가공하지 않고 원본 컬럼에 범위 조건을 거는 것임을 함께 설명한다.
- index-practice-schema.sql에는 generated column이 이미 존재하는 경우 에러가 나지 않도록 주의해서 작성한다.

13단계: 클러스터링 팩터 유사 실습

ClusteringFactorLikeExperimentRunner를 작성해줘.

목표:
MySQL에는 Oracle의 Clustering Factor를 동일하게 보여주는 지표가 없다는 점을 설명하고,
데이터 입력 순서와 인덱스 범위 조회의 테이블 접근 패턴 차이를 유사하게 관찰한다.

요구사항:
- 선택 실습으로 구성한다.
- orders_random 테이블과 orders_ordered 테이블을 비교할 수 있게 해줘.
- orders_random:
    - order_date가 랜덤하게 분포된 상태로 삽입된 테이블
- orders_ordered:
    - order_date 순서대로 삽입된 테이블
- 두 테이블 모두 order_date 인덱스를 생성한다.
- 같은 order_date 범위 조회를 실행한다.
- EXPLAIN ANALYZE와 수행 시간을 비교한다.
- README에 다음 내용을 설명한다.
    - Oracle의 Clustering Factor는 인덱스 키 순서와 테이블 데이터 저장 순서의 유사도를 나타낸다.
    - MySQL/InnoDB는 PK 기준 클러스터드 인덱스 구조이므로 Oracle Heap Table의 Clustering Factor와 동일하게 비교하면 안 된다.
    - 다만 보조 인덱스 탐색 후 PK를 통해 테이블 레코드를 찾는 과정에서 랜덤 액세스 성격이 발생할 수 있다.
    - 데이터 물리 배치, PK 설계, 조회 패턴이 성능에 영향을 줄 수 있다.

14단계: index-practice-schema.sql 작성

index-practice-schema.sql에는 다음 DDL을 포함해줘.

- 필요한 인덱스 생성 SQL
- generated column 추가 SQL
- generated column 인덱스 생성 SQL
- 실습용 보조 테이블 orders_random, orders_ordered 생성 SQL
- 이미 존재하는 인덱스나 컬럼 때문에 실패하지 않도록 가능한 범위에서 방어적으로 작성
- MySQL에서 CREATE INDEX IF NOT EXISTS가 버전에 따라 제한될 수 있으므로, README에 수동 실행 시 주의사항을 적어줘.

15단계: README 작성 요구사항

README.md에는 다음 섹션을 포함해줘.

1. 실습 목적
2. 사전 준비
3. application.yml 설정 설명
4. 실행계획 확인 방법
5. EXPLAIN 주요 컬럼 설명
    - type
    - key
    - rows
    - filtered
    - Extra
6. EXPLAIN FORMAT=JSON 보는 방법
7. EXPLAIN ANALYZE 보는 방법
8. Index Range Scan 실습
9. 인덱스 컬럼 가공 실습
10. DATE(order_date) 개선 실습
11. 결합 인덱스 선두 컬럼 실습
12. Index Full Scan 실습
13. Primary Key Lookup 실습
14. Covered Index / Covered Query 실습
15. 커버링 인덱스 추가 실습
16. 선행 컬럼 범위조건 실습
17. IN-List 실습
18. 함수기반 인덱스 대체 실습
19. 클러스터링 팩터 유사 실습
20. MySQL과 Oracle의 차이
21. 실습 결과 해석 시 주의사항

README에 반드시 포함할 핵심 문장:
- 인덱스를 사용한다는 사실만으로 쿼리가 빠르다고 판단하면 안 된다.
- B*Tree 인덱스는 수직적 탐색으로 시작점을 찾고, 수평적 탐색으로 범위를 읽는다.
- Index Range Scan은 인덱스 선두 컬럼과 조건 형태에 크게 영향을 받는다.
- 인덱스 컬럼을 함수로 가공하면 일반적인 인덱스 Range Scan이 어려워질 수 있다.
- 같은 의미의 조건이라도 컬럼을 가공하지 않는 범위 조건으로 바꾸면 인덱스를 더 잘 활용할 수 있다.
- 결합 인덱스에서는 선두 컬럼 조건이 중요하다.
- 커버링 인덱스는 테이블 랜덤 액세스를 줄이는 데 유리하다.
- 인덱스 컬럼을 많이 추가하면 조회는 빨라질 수 있지만 DML 비용과 저장공간 비용이 증가한다.
- MySQL의 실행계획 용어와 Oracle의 실행계획 용어는 다르다.
- Oracle의 Index Skip Scan과 Clustering Factor를 MySQL에서 동일하게 관찰할 수는 없다.