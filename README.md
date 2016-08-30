# AndroidWear_SensorRecording
Android sensor control & recording - Accelerometer, gyro, heart rate

## Enviroment
* Target Device : LG smart watch Urbane
* Target OS Version : Android 6.0.1
* Android studio SDK Version : API 23 : Android 6.0 (Marshmallow)

## DONE
* Send data read from various sensors to Android phone.
* Turn on the app continuously without touching.

sensor value is recorded at /sdcard/wearable/

## 사용법

1. fork합니다
2. 안드로이스 스튜디오 실행하시고 왼쪽 상단 File - New - import from version Control - Github 선택하시고 로그인합니다.
3. AndroidWear_SensorRecording 리파지토리를 선택하시고 clone합니다.
4. 빌드 및 포팅방법입니다.
  * Wear기기를 연결합니다.
  * ![alt text](https://github.com/Jungmo/project_report/blob/master/image/wearbutton.png?raw=true "wear button")
  * 그림과 같이 모바일을 선택하시고 실행버튼을 누릅니다.
  * Mobile 기기를 연결합니다.
  * ![alt text](https://github.com/Jungmo/project_report/blob/master/image/mobilebutton.png?raw=true "mobile button")
  * 그림과 같이 모바일을 선택하시고 실행버튼을 누릅니다.

**데이터베이스를 안 쓰실거면 보내드린 코드에서 mobile - src - main - java - 프로젝트이름 - MessageActivity 에 있는 코드 중 제일 밑 IF문을 주석처리 하면됩니다.**
