# [네트워크보안 연구실] IoT 기술 활용 스마트 제어 프로젝트
## 개요
1. 작업기간 : 2019.12 ~ 2020.03
2. 역할분배
- 기획 : 김소현, 이주완
- Front-end 개발 : 김소현, 이민규, 이주완
- Back-end 개발 : 김민규, 최한수 (코드 비공개)
- 보안 : 이주완, 최한수
3. 소개
- 네트워크보안 연구실 부원만이 사용할 수 있는 랩실 고유 어플리케이션
- 외부에서 랩실 내부 상태 조회가 가능한 기능 제공


## 기능 요구사항
### Application
- 회원가입
- 로그인(관리자모드/랩실부원모드)
- 자동로그인
- 비밀번호 재발급

- 관리자모드
	- 신규 회원 정보 등록
	- 일정 등록
	- 회의록 등록
	- 인원현황 등록
	- 조직도/구성도/IP,출입키 현황 등록
	- 랩실 규칙 등록

- 랩실부원모드
	- 일정 조회
	- 회의록 조회
	- 인원현황 조회
	- 조직도/구성도/IP,출입키 현황 조회
	- 랩실 규칙 조회
	- 재실/물/커피/A4 상태 조회
	- 전등 제어
	- 버티칼 제어

### IoT
- 재실여부 확인 - 초음파센서
- 전등제어(현재상태 확인 및 불켜고끄기) - 조도센서, 서보모터
- 물 잔여량 확인 - 무게센서
- 커피 잔여량 확인 - 무게센서
- A4용지 잔여량 확인 - 무게센서
- 버티칼 올리고내리기 - 서보모터

### 위젯
- 오늘 일정 조회(최대 2개)
- 재실여부/물잔여량/커피잔여량/A4잔여량 조회
- 크기 : 4X2


## 환경
### 구현환경
- Android Studio (Front-end 개발)
- Eclipse (Back-end 개발)

### 사용물품
| 품목 | 품명 | 규격 |
|:---:|:---:|:---:|
| 와이파이쉴드 | 우노 WIFI ESP8266 보드 | 5핀 |
| 초음파센서 | HC-SR04 | |
| 조도센서 | GL10537-1 | 10파이 |
| 서보모터 | SG90 | |
| 로드셀 무게센서 | DM865 | 3선식 |
| 로드셀 센서 HX711 모듈 | DM940 | |
