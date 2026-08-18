| 항목 | 결과 |
|---|---|
| 침해 시각 | 2026-06-28 08:58:19 UTC (KST 17:58) |
| 공격자 | `188.213.212.204`, Go 기반 자동 웜 |
| 침입 경로 | Next.js v15.1.6 RCE (`POST /` × 12) |
| 침입 소요 시간 | 약 1분 (08:58:17 ~ 08:59:14) |
| 심어진 것 | XMRig(미설정), `scanner_linux`(Elasticsearch 랜섬 웜) |
| 악성 활동 기간 | 6/29 06:36 ~ 7/8 02:06 (약 10일) |
| 정지 원인 | `data.log` 4.48GB → 디스크 100% → 재부팅 → 지속성 없어 종료 |
| 재접속 | 공격자 IP 요청 14건이 전부, 이후 재접속 없음 (7/4까지 확인 가능) |
| 실제 피해 | 채굴 미작동, 타 서버 공격 실패, 데이터 전송 요금 약 2만원 |
| 권한 상승 | 없음 (`ec2-user` 수준, 루트킷 없음) |
| 계정 침해 | 없음 |

## 1. 악성 파일 탐색
`sudo find / -xdev -iname '*xmrig*' 2>/dev/null`
→ /home/ec2-user/customer-web/customer-web-upload/ 에서 발견. /tmp가 아닌 앱 디렉터리라는 점이 첫 단서

## 2. 침해 시각(T0) 특정
`stat xmrig.tar.gz`

→ Birth: 2026-06-28 08:58:22 = T0

→ Modify: 2023-11-23은 배포판 원본 타임스탬프 (tar가 보존), 위조 아님

## 3. 침해 파일 목록화
`ls -la --time-style=full-iso /home/ec2-user/customer-web/customer-web-upload/`

→ scanner_linux(9MB, 실행권한), data.log(4.48GB), exploited.log/failed.log/scanner_deployed.log(0바이트) 발견

→ 디렉터리 권한 777 확인

## 4. 디스크 점유 원인
`df -h`

`sudo du -sh /home/ec2-user/* /var/log/* 2>/dev/null | sort -h | tail -15`

→ / 8GB 중 100% 사용, 여유 120K

→ customer-web 5.1GB (대부분 data.log)

## 5. 악성 바이너리 분석 ★
`strings scanner_linux | grep -iE 'CVE-|/api/|elastic|exploit|payload' | sort -u`

### 핵심 발췌
```
[Next.js Scanner + Exploitation Framework]
Z:/Ransom_Botnet/Elasticsearch/go/worm.go
Next.js scanner with RCE detection + unauthenticated Elasticsearch ransomer
var res = process.mainModule.require('child_process').execSync('%s', ...);
throw Object.assign(new Error('NEXT_REDIRECT'), {digest:`${res}`});
nohup ./scanner_linux -t 1000 >/dev/null 2>&1 &
```
→ 웜 정체와 RCE 페이로드 확보

## 6. 악성 활동 로그 확인
`head -c 2000 data.log`

`cat monitor.log | head -50`

→ 전역 포트 스캔 기록 (OPEN_80, OPEN_443, OPEN_9200_ES)

→ 6/29 06:36 시작, 7/8 02:06까지

## 7. 채굴기 설정 확인
`cat xmrig-6.21.0/config.json`
→ "user": "YOUR_WALLET_ADDRESS" — 기본값. 채굴 미작동 확정

## 8. 공격 표면 확인
`sudo ss -tulpn`

`ps aux | grep -iE 'node|next|java'`

`sudo nginx -T | grep -E 'access_log|proxy_pass'`

→ nginx(80/443) → Next.js v15.1.6(3000) + Spring Boot(8080) 프록시 구조

## 9. 침입 경로 확정 ★
`sudo zcat /var/log/nginx/access.log-20260629.gz | grep "28/Jun/2026:08:5"`
```
188.213.212.204 08:58:18 "GET / " 200      ← Next.js 탐지
188.213.212.204 08:58:19 "POST /" 500      ← RCE 주입 (12회)
188.213.212.204 08:59:14 "POST /" 499      ← 종료
User-Agent: Go-http-client/1.1
```
→ 500 응답 = 성공 신호 (페이로드가 의도적으로 예외를 던지는 구조)

→ 파일 Birth(08:58:22)와 시각 일치

## 10. 재접속 여부
`sudo zcat access.log-20260629.gz | grep "188.213.212.204" | wc -l` # 14

`sudo zcat access.log-2026070*.gz | grep "188.213.212.204"` # 없음

## 11. SSH 침입 여부
`last -20`

`cat ~/.ssh/authorized_keys`

→ 전부 본인 접속(13.209.1.x는 EC2 Instance Connect 대역), 등록 키 1개뿐

## 12. 지속성 조사
`sudo crontab -l; crontab -l`

`sudo systemctl list-timers --all --no-pager`

`ls -la /etc/systemd/system/`

`cat /etc/ld.so.preload`

→ cron 없음, 타이머 전부 OS 기본, 서비스 파일 2개 모두 T0 이전(6/27) 생성

→ 지속성 없음 = 재부팅으로 종료된 이유

## 13. T0 전후 변경 파일
`sudo find / -xdev -newermt "2026-06-28 07:00" ! -newermt "2026-06-28 12:00" -type f 2>/dev/null`

→ 시스템 영역 변조 없음






