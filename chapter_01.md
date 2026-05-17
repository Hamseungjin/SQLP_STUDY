# 백그라운드 프로세스
## System Monitor (SMON)
에러가 발생한 시스템 reboot 시 인스턴스 복구 수행, 임시 세그먼트와 익스텐트 모니터링
## Process Monitor (PMON)
이상이 생긴 프로세스가 사용하던 리소스 복구
## Database Writers (DBWn)
버퍼 캐시의 Dirty 버퍼를 데이터 파일에 기록
```
Data File의 블록
    ↓ 읽기(메모리 버퍼 캐시에 올림)
Clean Buffer
    ↓ 수정
Dirty Buffer: 메모리 상에서 수정된 블록
```
## Log Writer (LGWR)
로그 버퍼 엔트리를 Redo 로그 파일에 기록 (COMMIT)
*사용자 프로세스가 로그를 로그 버퍼에 기록 -> LGWR이 주기적으로 Redo 로그 파일에 기록*
## Archiver (ARCn)
오래된 Redo 로그를 Archive 로그 디렉토리로 백업
## Checkpoint (CKPT)
- 이전에 checkpoint가 발생한 마지막 시점 이후의 데이터베이스 변경 사항을 데이터 파일에 기록하도록 트리거
- 어디까지 기록했는지 컨트롤 파일과 데이터 파일 헤더에 저장
- 에러 발생 시 마지막 체크포인트 시점(버퍼 캐시와 데이터 파일이 동기화된 시점) 이후 로그 데이터만 디스크에 기록하여 복구
## Recoverer (RECO)
분산 트랜잭션 에러 복구
# 데이터 저장 구조
<img width="988" height="684" alt="Screenshot_20260516_182352_Obsidian" src="https://github.com/user-attachments/assets/3048b93f-4f8a-4ac9-9a7d-a86478e845d5" />

## 블록(페이지)
I/O 단위
## 익스텐트
공간 확장 단위
## 세그먼트
- 데이터 공간을 사용하는 Object(Table, Index, Cluster, Partition, Lob) 
	<-> 데이터 공간을 사용하지 않는 Object (View, Sequence, ...)
## 테이블 스페이스
세그먼트를 담는 컨테이너, 여러 데이터파일로 구성
# Undo / Redo
## Undo
변경 전 정보 저장
- Transaction Rollback
- Transaction Recovery (Instance Recovery의 Rollback 단계)
- Read Consistency
## Redo
변경된 정보 저장
- Database Recovery (=Media Recovery) - 디스크/파일 문제, Archived Redo 사용
- Cache Recovery (Instance Recovery의 Roll Forward 단계)
- Fast Commit
## Instance Recovery
### 1. Cache Recovery / Roll Forward
- Commit된 변경이 Data File에 없는 문제
```
T1: UPDATE sal 3000 → 5000
T1: COMMIT 완료
LGWR: Redo Log 기록 완료
DBWn: Data File에는 아직 안 씀
장애 발생
```
- Redo 사용, Data File에 저장
### 2. Transaction Recovery / Rollback
- Commit 안 된 변경이 Data File에 있는 문제
- Undo 사용, Data File Rollback
# SGA(System Global Area)
## DB 버퍼 캐시
테이블 블록, 인덱스 블록, Undo 블록
## 라이브러리 캐시
SQL, DB 저장형 함수/프로시저, 트리거
## 딕셔너리 캐시
테이블 정보, 인덱스 정보, 데이터파일 정보, 시퀀스
## Result 캐시
SQL 결과집합
# 메모리 버퍼캐시 경유 블록 I/O
병렬 처리 등 Direct Path I/O를 제외할 경우 모든 블록 I/O가 경유
# Multiblock I/O
- Table Full Scan, Index Fast Full Scan 시 사용
- 한 번의 I/O call에서 하나의 익스텐트만 읽음
- db_file_multiblock_read_count param
- db file scattered read 대기 이벤트를
# BCHR
(1 - disk / (consistent(=query) + current)) * 100
# LRU / MRU
버퍼 캐시에 적재되는 블록 관리
- LRU (Least Recently Used)
- MRU (Most Recently Used)
가장 최근에 사용된 데이터 블록을 저장하면서 LRU를 제거
