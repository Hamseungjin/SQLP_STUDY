인덱스 구조 및 사용법

다음 실습 쿼리와 실행 코드를 작성해줘.

실습 1: Index Range Scan이 가능한 쿼리
- order_date 인덱스를 사용
- 예:
  SELECT * FROM orders
  WHERE order_date >= ? AND order_date < ?
- EXPLAIN 결과에서 type이 range인지 확인

실습 2: 인덱스 컬럼을 가공해서 Range Scan이 어려워지는 쿼리
- 예:
  SELECT * FROM orders
  WHERE DATE(order_date) = ?
- 또는 YEAR(order_date) = ?
- EXPLAIN 결과에서 인덱스 사용 여부, rows 추정치 차이를 비교

실습 3: 같은 의미지만 인덱스를 사용할 수 있도록 개선한 쿼리
- DATE(order_date) = '2025-01-01' 대신
  order_date >= '2025-01-01 00:00:00'
  AND order_date < '2025-01-02 00:00:00'
- 개선 전/후 실행 계획과 수행 시간 비교

실습 4: 결합 인덱스(customer_id, status) 사용
- customer_id와 status를 모두 조건에 넣은 경우
- status만 조건에 넣은 경우
- customer_id만 조건에 넣은 경우
- 각 경우에 대해 EXPLAIN 결과를 출력하고, 결합 인덱스의 선두 컬럼 원리를 설명

주의:
- MySQL은 Oracle의 Index Skip Scan과 동작 방식이 다르다.
- MySQL 8 기준으로 Skip Scan이 항상 명확히 관찰되는 것은 아니므로, README에는 “Oracle 실습 시나리오와 MySQL 실습 시나리오의 차이”를 반드시 설명해줘.
- MySQL에서 가능한 범위 안에서는 EXPLAIN FORMAT=JSON 또는 EXPLAIN ANALYZE를 사용해 비교해줘.

4단계: 실행 계획과 I/O 통계 확인

ExplainPlanService를 만들어 다음 기능을 제공해줘:
- EXPLAIN 쿼리 실행
- EXPLAIN FORMAT=JSON 실행
- 가능하면 EXPLAIN ANALYZE 실행
- 결과를 콘솔 로그로 보기 좋게 출력

1. Index Full Scan 실습
- 결합 인덱스 idx_orders_customer_status(customer_id, status)가 있는 상태에서
  선두 컬럼 customer_id 없이 status만 조건으로 조회하는 쿼리를 실행해줘.
- 예:
  SELECT customer_id, status FROM orders WHERE status = ?
- EXPLAIN 결과에서 type, key, rows, Extra를 확인하고,
  Index Range Scan, Index Full Scan, Table Full Scan의 차이를 README에 설명해줘.

2. Index Unique Scan 또는 Primary Key Lookup 실습
- order_id PK로 단건 조회하는 실습을 추가해줘.
- 예:
  SELECT * FROM orders WHERE order_id = ?
- MySQL EXPLAIN에서 type=const 또는 eq_ref/ref로 보일 수 있음을 설명하고,
  Oracle의 Index Unique Scan과 개념적으로 대응된다고 설명해줘.

3. Covered Index / Covered Query 실습
- 다음 두 쿼리를 비교해줘.

A. 테이블 랜덤 액세스가 발생할 수 있는 쿼리:
SELECT * FROM orders
WHERE customer_id = ? AND status = ?

B. 인덱스만으로 처리 가능한 커버링 쿼리:
SELECT customer_id, status
FROM orders
WHERE customer_id = ? AND status = ?

- EXPLAIN의 Extra에서 Using index가 나타나는지 확인해줘.
- Covered Index가 테이블 랜덤 액세스를 줄이는 원리를 README에 설명해줘.

4. 커버링 인덱스 추가 실습
- 기존 idx_orders_customer_status(customer_id, status)와 별도로
  idx_orders_customer_status_date_amount(customer_id, status, order_date, amount)를 추가해줘.
- 다음 쿼리의 실행 계획을 비교해줘.
  SELECT order_date, amount
  FROM orders
  WHERE customer_id = ? AND status = ?
- 인덱스에 필요한 컬럼을 추가하면 테이블 랜덤 액세스를 줄일 수 있음을 설명해줘.
- 단, 인덱스가 많아지면 INSERT/UPDATE/DELETE 비용이 증가한다는 점도 설명해줘.

5. 선행 컬럼 범위조건 실습
- 다음 두 인덱스를 비교해줘.
  A. idx_orders_customer_status_date(customer_id, status, order_date)
  B. idx_orders_customer_date_status(customer_id, order_date, status)

- 다음 쿼리에서 어떤 인덱스가 더 효율적인지 EXPLAIN ANALYZE로 비교해줘.
  SELECT *
  FROM orders
  WHERE customer_id = ?
  AND status = ?
  AND order_date >= ?
  AND order_date < ?

- 선행 컬럼이 모두 = 조건이면 인덱스 스캔 효율이 좋고,
  중간에 범위조건이 오면 뒤쪽 컬럼을 인덱스 탐색 조건으로 충분히 활용하기 어려울 수 있음을 설명해줘.

6. IN-List 전환 실습
- status BETWEEN 또는 범위성 조건 대신 status IN (...) 조건을 사용하는 쿼리를 비교해줘.
- MySQL과 Oracle의 실행계획 표현 차이를 설명해줘.
- Oracle에서는 INLIST ITERATOR가 나타날 수 있고,
  MySQL에서는 range/ref 접근 또는 optimizer trace를 통해 다르게 표현될 수 있음을 설명해줘.

7. 함수기반 인덱스 대체 실습
- DATE(order_date) = ? 쿼리는 일반 order_date 인덱스를 제대로 활용하기 어렵다는 점을 보여줘.
- MySQL 8의 functional index 또는 generated column을 이용해 이를 개선하는 예시를 추가해줘.
- 예:
  ALTER TABLE orders ADD COLUMN order_ymd DATE GENERATED ALWAYS AS (DATE(order_date)) STORED;
  CREATE INDEX idx_orders_order_ymd ON orders(order_ymd);
- 개선 전/후 EXPLAIN을 비교해줘.

8. 클러스터링 팩터 유사 실습
- MySQL에는 Oracle의 클러스터링 팩터를 동일하게 보여주는 지표가 없다는 점을 설명해줘.
- 대신 order_date 순서대로 입력한 테이블과 랜덤 order_date로 입력한 테이블을 비교하는 선택 실습을 추가해줘.
- 같은 order_date 범위 조회 시 테이블 접근 패턴과 실행 시간이 달라질 수 있음을 설명해줘.